/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jface.text;


/**
 * <code>ChildDocumentManager</code> is one particular implementation of 
 * <code>ISlaveDocumentManager</code>. This manager creates so called child
 * documents as slave documents for given master documents.<p>
 * 
 * A child document represents a particular range of the parent 
 * document and is accordingly adapted to changes of the parent document. 
 * Vice versa, the parent document is accordingly adapted to changes of
 * its child documents. The manager does not maintain any particular management
 * structure but utilizes mechanisms given by <code>IDocument</code> such
 * as position categories and position updaters. <p>
 *
 * For internal use only.
 */
public final class ChildDocumentManager implements IDocumentListener, ISlaveDocumentManager {
	
	
	/** 
	 * Name of the position category used to keep track of the ranges of the parent documents
	 * that correspond to child documents.
	 */
	public final static String CHILDDOCUMENTS= "__childdocuments"; //$NON-NLS-1$
	
	
	/**
	 * Positions which are used to mark the child documents offset ranges into
	 * the parent documents. This position uses as bidirectional reference as
	 * it knows the child document as well as the parent document.
	 */
	static class ChildPosition extends Position {
		
		/** The parent document. */
		public IDocument fParentDocument;
		/* The child document */
		public ChildDocument fChildDocument;
		
		/**
		 * Creates a new child position for the given parent document.
		 * @param parentDocument the parent document
		 * @param offset the offset into the parent document
		 * @param length the length in the parent document
		 */
		public ChildPosition(IDocument parentDocument, int offset, int length) {
			super(offset, length);
			fParentDocument= parentDocument;
		}
		
		/**
		 * Changed to be compatible to the position updater behavior
		 * @see Position#overlapsWith(int, int)
		 */
		public boolean overlapsWith(int offset, int length) {
			boolean append= (offset == this.offset + this.length) && length == 0;
			return append || super.overlapsWith(offset, length);
		}
	}	
	
	
	/**
	 * The position updater used to adapt the positions representing
	 * the child document ranges to changes of the parent document.
	 */
	static class ChildPositionUpdater extends DefaultPositionUpdater {
		
		/** Cached document event */
		private DocumentEvent fDocumentEvent;

		/**
		 * Creates the position updated.
		 */
		protected ChildPositionUpdater() {
			super(CHILDDOCUMENTS);
		}
		
		/**
		 * Child document ranges cannot be deleted other then by calling
		 * freeChildDocument.
		 */
		protected boolean notDeleted() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jface.text.DefaultPositionUpdater#update(org.eclipse.jface.text.DocumentEvent)
		 */
		public void update(DocumentEvent event) {
			try {
				fDocumentEvent= event;
				super.update(event);
			} finally {
				fDocumentEvent= null;
			}
		}

		/**
		 * If an insertion happens at a child document's start offset, the
		 * position is extended rather than shifted. Also, if something is added 
		 * right behind the end of the position, the position is extended rather
		 * than kept stable.
		 * 
		 * In auto expand mode the position is always streched to contain the 
		 * whole area of the change.
		 */
		protected void adaptToInsert() {

			int myStart= fPosition.offset;
			int myEnd=   fPosition.offset + fPosition.length;
			boolean isAutoExpanding= isAutoExpanding();
	
			if (fLength != 0 && fOffset < myEnd && !isAutoExpanding) {
				super.adaptToInsert();
				return;
			}
			
			int yoursStart= fOffset;
			int yoursEnd=   fOffset + fReplaceLength -1;
			yoursEnd= Math.max(yoursStart, yoursEnd);
			
			if (myEnd < yoursStart) {
				if (isAutoExpanding)
					fPosition.length= yoursEnd - myStart + 1;
				return;
			}
			
			if (myStart <= yoursStart)
				fPosition.length += fReplaceLength;
			else { // yoursStart < myStart
				if (isAutoExpanding) {
					fPosition.offset= yoursStart;
					fPosition.length += (myStart - yoursStart + fReplaceLength);
				} else {
					fPosition.offset += fReplaceLength;
				}
			}
		}
		
		/**
		 * Returns whether the child documents should automatically expand to include
		 * any parent document change.
		 * 
		 * @return <code>true</code> if auto expanding, <code>false</code> otherwise
		 * @since 2.1
		 */
		private boolean isAutoExpanding() {
			if (fPosition instanceof ChildPosition) {
				ChildPosition position= (ChildPosition) fPosition;
				return position.fChildDocument.isAutoExpandEvent(fDocumentEvent);
			}
			return false;
		}
	}
	
	/**
	 * The child document partitioner uses the parent document to answer all questions.
	 */
	static class ChildPartitioner implements IDocumentPartitioner {
		
		/** The child document. */
		protected ChildDocument fChildDocument;
		/** The parent document */
		protected IDocument fParentDocument;
		/** 
		 * The parent document as <code>IDocumentExtensions3</code>.
		 * @since 3.0
		 */
		protected IDocumentExtension3 fParentDocument3;
		/** 
		 * The partitioning of this partitioner.
		 * @since 3.0
		 */
		protected String fPartitioning;
		
		/** 
		 * Creates a new child document partitioner for the given document
		 * partitioning.
		 * 
		 * @param partitioning the document partitioning
		 * @since 3.0
		 */
		protected ChildPartitioner(String partitioning) {
			fPartitioning= partitioning;
		}
		
		/** 
		 * Creates a new child document partitioner.
		 */
		protected ChildPartitioner() {
			fPartitioning= null;
		}
		
		/*
		 * @see IDocumentPartitioner#getPartition(int)
		 */
		public ITypedRegion getPartition(int offset) {
			try {
				offset += fChildDocument.getParentDocumentRange().getOffset();
				if (fParentDocument3 != null)
					return fParentDocument3.getPartition(fPartitioning, offset);
				return fParentDocument.getPartition(offset);
			} catch (BadLocationException x) {
			} catch (BadPartitioningException x) {
			}
			
			return null;
		}
		
		/*
		 * @see IDocumentPartitioner#computePartitioning(int, int)
		 */
		public ITypedRegion[] computePartitioning(int offset, int length) {
			try {
				offset += fChildDocument.getParentDocumentRange().getOffset();
				if (fParentDocument3 != null)
					return fParentDocument3.computePartitioning(fPartitioning, offset, length);
				return fParentDocument.computePartitioning(offset, length);
			} catch (BadLocationException x) {
			} catch (BadPartitioningException x) {
			}
			
			return null;
		}
		
		/*
		 * @see IDocumentPartitioner#getContentType(int)
		 */
		public String getContentType(int offset) {
			try {
				offset += fChildDocument.getParentDocumentRange().getOffset();
				if (fParentDocument3 != null)
					return fParentDocument3.getContentType(fPartitioning, offset);
				return fParentDocument.getContentType(offset);
			} catch (BadLocationException x) {
			} catch (BadPartitioningException x) {
			}
			
			return null;
		}
		
		/*
		 * @see IDocumentPartitioner#getLegalContentTypes()
		 */
		public String[] getLegalContentTypes() {
			if (fParentDocument3 != null)
				try {
					return fParentDocument3.getLegalContentTypes(fPartitioning);
				} catch (BadPartitioningException x) {
					return new String[0];
				}
			return fParentDocument.getLegalContentTypes();
		}
		
		/*
		 * @see IDocumentPartitioner#documentChanged(DocumentEvent)
		 */
		public boolean documentChanged(DocumentEvent event) {
			// ignore as the parent does this for us
			return false;
		}
		
		/*
		 * @see IDocumentPartitioner#documentAboutToBeChanged(DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			// ignore as the parent does this for us
		}
		
		/*
		 * @see IDocumentPartitioner#disconnect()
		 */
		public void disconnect() {
			fChildDocument= null;
			fParentDocument= null;
			fParentDocument3= null;
		}
		
		/*
		 * @see IDocumentPartitioner#connect(IDocument)
		 */
		public void connect(IDocument childDocument) {
			Assert.isTrue(childDocument instanceof ChildDocument);
			fChildDocument= (ChildDocument) childDocument;
			fParentDocument= fChildDocument.getParentDocument();
			if (fParentDocument instanceof IDocumentExtension3)
				fParentDocument3= (IDocumentExtension3) fParentDocument;
		}	
	}
	
	
	
	/** The position updater shared by all documents which have child documents */
	private IPositionUpdater fChildPositionUpdater;	
	
	
	/**
	 * Returns the child position updater. If necessary, it is dynamically created.
	 *
	 * @return the child position updater
	 */
	protected IPositionUpdater getChildPositionUpdater() {
		if (fChildPositionUpdater == null)
			fChildPositionUpdater= new ChildPositionUpdater();
		return fChildPositionUpdater;
	}
	
	 /*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#createSlaveDocument(org.eclipse.jface.text.IDocument)
	 */
	public IDocument createSlaveDocument(IDocument master)  {

		if (!master.containsPositionCategory(CHILDDOCUMENTS)) {
			master.addPositionCategory(CHILDDOCUMENTS);
			master.addPositionUpdater(getChildPositionUpdater());
			master.addDocumentListener(this);
		}

		ChildPosition pos= new ChildPosition(master, 0, 0);
		try {
			master.addPosition(CHILDDOCUMENTS, pos);
		} catch (BadPositionCategoryException x) {
			// cannot happen
		} catch (BadLocationException x) {
			// (0, 0) is OK
		}

		ChildDocument child= new ChildDocument(master, pos);
		if (master instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) master;
			String[] partitionings= extension3.getPartitionings();
			for (int i= 0; i < partitionings.length; i++) {
				IDocumentPartitioner partitioner= new ChildPartitioner(partitionings[i]);
				child.setDocumentPartitioner(partitionings[i], partitioner);
				partitioner.connect(child);
			}
		} else {
			IDocumentPartitioner partitioner= new ChildPartitioner();
			child.setDocumentPartitioner(partitioner);
			partitioner.connect(child);
		}
		
		pos.fChildDocument= child;
		
		return child;
	}
	
	/*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#freeSlaveDocument(org.eclipse.jface.text.IDocument)
	 */
	public void freeSlaveDocument(IDocument slave) {
		
		if (! (slave instanceof ChildDocument))
			return;
			
		ChildDocument childDocument= (ChildDocument) slave;
		
		String[] partitionings= childDocument.getPartitionings();
		for (int i= 0; i < partitionings.length; i ++) {
			IDocumentPartitioner partitioner= childDocument.getDocumentPartitioner(partitionings[i]);
			if (partitioner != null)
				partitioner.disconnect();
		}
		
		ChildPosition pos= (ChildPosition) childDocument.getParentDocumentRange();
		IDocument parent= pos.fParentDocument;
		
		try {
			parent.removePosition(CHILDDOCUMENTS, pos);
			Position[] category= parent.getPositions(CHILDDOCUMENTS);
			if (category.length == 0) {
				parent.removeDocumentListener(this);
				parent.removePositionUpdater(getChildPositionUpdater());
				parent.removePositionCategory(CHILDDOCUMENTS);
			}
		} catch (BadPositionCategoryException x) {
			// cannot happen
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#createMasterSlaveMapping(org.eclipse.jface.text.IDocument)
	 */
	public IDocumentInformationMapping createMasterSlaveMapping(IDocument slave) {
		if (slave instanceof ChildDocument)
			return new ParentChildMapping((ChildDocument) slave);
		return null;
	}
	
	/*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#getMasterDocument(org.eclipse.jface.text.IDocument)
	 */
	public IDocument getMasterDocument(IDocument slave) {
		if (slave instanceof ChildDocument)
			return ((ChildDocument) slave).getParentDocument();
		return null;
	}
	
	/*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#isSlaveDocument(org.eclipse.jface.text.IDocument)
	 */
	public boolean isSlaveDocument(IDocument document) {
		return (document instanceof ChildDocument);
	}
	
	/**
	 * Informs all child documents of the document which issued this document event.
	 *
	 * @param about indicates whether the change is about to happen or alread happend
	 * @param event the document event which will be processed to inform child documents
	 */
	protected void fireDocumentEvent(boolean about, DocumentEvent event) {
		try {
			
			IDocument parent= event.getDocument();
			Position[] children= parent.getPositions(CHILDDOCUMENTS);
			for (int i= 0; i < children.length; i++) {
				Object o= children[i];
				if (o instanceof ChildPosition) {
					ChildPosition pos= (ChildPosition) o;
					if (about)
						pos.fChildDocument.parentDocumentAboutToBeChanged(event);
					else
						pos.fChildDocument.parentDocumentChanged(event);
				}
			}
		} catch (BadPositionCategoryException x) {
			// cannot happen
		}
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		fireDocumentEvent(false, event);
	}

	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
		fireDocumentEvent(true, event);
	}
	
	/*
	 * @see org.eclipse.jface.text.ISlaveDocumentManager#setAutoExpandMode(org.eclipse.jface.text.IDocument, boolean)
	 */
	public void setAutoExpandMode(IDocument slaveDocument, boolean autoExpand) {
		if (slaveDocument instanceof ChildDocument)
			((ChildDocument) slaveDocument).setAutoExpandMode(autoExpand);
	}
}
