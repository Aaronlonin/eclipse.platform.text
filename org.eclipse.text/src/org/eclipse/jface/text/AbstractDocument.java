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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;


/**
 * Abstract implementation of <code>IDocument</code>. 
 * Implements the complete contract of <code>IDocument</code>, <code>IDocumentExtension</code>,
 * and <code>IDocumentExtension2</code>.<p>
 * 
 * An <code>AbstractDocument</code> supports the following implementation plug-ins:
 * <ul>
 * <li> a text store for storing and managing the document's content,
 * <li> a line tracker to map character positions to line numbers and vice versa
 * </ul>
 * The document can dynamically change the text store when switching between 
 * sequential rewrite mode and normal mode.<p>
 * 
 * This class must be subclassed. Subclasses must configure which implementation 
 * plug-ins the document should use. Subclasses are not intended to overwrite existing methods.
 *
 * @see IDocument
 * @see ITextStore
 * @see ILineTracker
 */
public abstract class AbstractDocument implements IDocument, IDocumentExtension, IDocumentExtension2, IDocumentExtension3, IRepairableDocument {
	
	/**
	 * Inner class to bundle a registered post notification replace operation together with its
	 * owner.
	 * 
	 * @since 2.0
	 */
	static private class RegisteredReplace {
		/** The owner of this replace operation. */
		IDocumentListener fOwner;
		/** The replace operation */
		IDocumentExtension.IReplace fReplace;
		
		/**
		 * Creates a new bundle object.
		 * @param owner the document listener owning the replace operation
		 * @param replace the replace operation
		 */
		RegisteredReplace(IDocumentListener owner, IDocumentExtension.IReplace replace) {
			fOwner= owner;
			fReplace= replace;
		}
	}
	
	
	/** The document's text store */
	private ITextStore   fStore;
	/** The document's line tracker */
	private ILineTracker fTracker;
	/** The registered document listeners */
	private List fDocumentListeners;
	/** The registered prenotified document listeners */
	private List fPrenotifiedDocumentListeners;
	/** The registered document partitioning listeners */
	private List fDocumentPartitioningListeners;
	/** All positions managed by the document */
	private Map fPositions;
	/** All registered document position updaters */
	private List fPositionUpdaters;
	/** 
	 * The list of post notification changes
	 * @since 2.0
	 */
	private List fPostNotificationChanges;
	/** 
	 * The reentrance count for post notification changes.
	 * @since 2.0
	 */
	private int fReentranceCount= 0;
	/** 
	 * Indicates whether post notification change processing has been stopped.
	 * @since 2.0
	 */
	private int fStoppedCount= 0;
	/**
	 * Indicates whether the registration of post notification changes should be
	 * ignored.
	 * @since 2.1
	 */
	private boolean fAcceptPostNotificationReplaces= true;
	/**
	 * Indicates whether the notification of listeners has been stopped.
	 * @since 2.1
	 */
	private int fStoppedListenerNotification= 0;
	/**
	 * The document event to be sent after listener notification has been resumed.
	 * @since 2.1
	 */
	private DocumentEvent fDeferredDocumentEvent;
	/**
	 * The registered document partitioners.
	 * @since 3.0
	 */
	private Map fDocumentPartitioners;
	/**
	 * The partitioning changed event.
	 * @since 3.0
	 */
	private DocumentPartitioningChangedEvent fDocumentPartitioningChangedEvent;
	/**
	 * The find/replace document adapter.
	 * @since 3.0
	 */
	private FindReplaceDocumentAdapter fFindReplaceDocumentAdapter;
	
	
	/**
	 * The default constructor does not perform any configuration
	 * but leaves it to the clients who must first initialize the
	 * implementation plug-ins and then call <code>completeInitialization</code>.
	 * Results in the construction of an empty document.
	 */
	protected AbstractDocument() {
	}
	
	
	//--- accessor to fields -------------------------------
	
	/**
	 * Returns the document's text store. Assumes that the
	 * document has been initialized with a text store.
	 *
	 * @return the document's text store
	 */
	protected ITextStore getStore() {
		Assert.isNotNull(fStore);
		return fStore;
	}
	
	/**
	 * Returns the document's line tracker. Assumes that the
	 * document has been initialized with a line tracker.
	 *
	 * @return the document's line tracker
	 */
	protected ILineTracker getTracker() {
		Assert.isNotNull(fTracker);
		return fTracker;
	}	
	
	/**
	 * Returns the document's document listeners.
	 *
	 * @return the document's document listeners
	 */
	protected List getDocumentListeners() {
		return fDocumentListeners;
	}
	
	/** 
	 * Returns the document's partitioning listeners .
	 *
	 * @return the document's partitioning listeners
	 */
	protected List getDocumentPartitioningListeners() {
		return fDocumentPartitioningListeners;
	}
	
	/**
	 * Returns all positions managed by the document grouped by category.
	 *
	 * @return the document's positions
     */
	protected Map getDocumentManagedPositions() {
		return fPositions;
	}
	
	/*
	 * @see IDocument#getDocumentPartitioner
	 */
	public IDocumentPartitioner getDocumentPartitioner() {
		return getDocumentPartitioner(DEFAULT_PARTITIONING);
	}
	
	
	
	//--- implementation configuration interface ------------
		
	/**
	 * Sets the document's text store.
	 * Must be called at the beginning of the constructor.
	 *
	 * @param store the document's text store
	 */
	protected void setTextStore(ITextStore store) {
		fStore= store;
	}
	
	/**
	 * Sets the document's line tracker. 
	 * Must be called at the beginnng of the constructor.
	 *
	 * @param tracker the document's line tracker
	 */
	protected void setLineTracker(ILineTracker tracker) {
		fTracker= tracker;
	}
		
	/*
	 * @see org.eclipse.jface.text.IDocument#setDocumentPartitioner(org.eclipse.jface.text.IDocumentPartitioner)
	 */
	public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
		setDocumentPartitioner(DEFAULT_PARTITIONING, partitioner);
	}
			
	/**
	 * Initializes document listeners, positions, and position updaters.
	 * Must be called inside the constructor after the implementation plug-ins
	 * have been set.
	 */
	protected void completeInitialization() {
		
		fPositions= new HashMap();
		fPositionUpdaters= new ArrayList();
		fDocumentListeners= new ArrayList();
		fPrenotifiedDocumentListeners= new ArrayList();
		fDocumentPartitioningListeners= new ArrayList();
		
		addPositionCategory(DEFAULT_CATEGORY);
		addPositionUpdater(new DefaultPositionUpdater(DEFAULT_CATEGORY));		
	}
	
		
	//-------------------------------------------------------
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addDocumentListener(org.eclipse.jface.text.IDocumentListener)
	 */
	public void addDocumentListener(IDocumentListener listener) {
		Assert.isNotNull(listener);
		if (! fDocumentListeners.contains(listener))
			fDocumentListeners.add(listener);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#removeDocumentListener(org.eclipse.jface.text.IDocumentListener)
	 */
	public void removeDocumentListener(IDocumentListener listener) {
		Assert.isNotNull(listener);
		fDocumentListeners.remove(listener);
	}
	
	/*
	 * @see IDocument#addPrenotifiedDocumentListener(IDocumentListener) 
	 */
	public void addPrenotifiedDocumentListener(IDocumentListener listener) {
		Assert.isNotNull(listener);
		if (! fPrenotifiedDocumentListeners.contains(listener))
			fPrenotifiedDocumentListeners.add(listener);
	}
	
	/*
	 * @see IDocument#removePrenotifiedDocumentListener(IDocumentListener)
	 */
	public void removePrenotifiedDocumentListener(IDocumentListener listener) {
		Assert.isNotNull(listener);
		fPrenotifiedDocumentListeners.remove(listener);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addDocumentPartitioningListener(org.eclipse.jface.text.IDocumentPartitioningListener)
	 */
	public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
		Assert.isNotNull(listener);
		if (! fDocumentPartitioningListeners.contains(listener))
			fDocumentPartitioningListeners.add(listener);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#removeDocumentPartitioningListener(org.eclipse.jface.text.IDocumentPartitioningListener)
	 */
	public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
		Assert.isNotNull(listener);
		fDocumentPartitioningListeners.remove(listener);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addPosition(java.lang.String, org.eclipse.jface.text.Position)
	 */
	public void addPosition(String category, Position position) throws BadLocationException, BadPositionCategoryException  {
		
		if ((0 > position.offset) || (0 > position.length) || (position.offset + position.length > getLength()))
			throw new BadLocationException();
			
		if (category == null)
			throw new BadPositionCategoryException();
			
		List list= (List) fPositions.get(category);
		if (list == null)
			throw new BadPositionCategoryException();
		
		list.add(computeIndexInPositionList(list, position.offset), position);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addPosition(org.eclipse.jface.text.Position)
	 */
	public void addPosition(Position position) throws BadLocationException {
		try {
			addPosition(DEFAULT_CATEGORY, position);
		} catch (BadPositionCategoryException e) {
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addPositionCategory(java.lang.String)
	 */
	public void addPositionCategory(String category) {
		
		if (category == null)
			return;
			
		if (!containsPositionCategory(category))
			fPositions.put(category, new ArrayList());
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#addPositionUpdater(org.eclipse.jface.text.IPositionUpdater)
	 */
	public void addPositionUpdater(IPositionUpdater updater) {
		insertPositionUpdater(updater, fPositionUpdaters.size());
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#containsPosition(java.lang.String, int, int)
	 */
	public boolean containsPosition(String category, int offset, int length) {
		
		if (category == null)
			return false;
			
		List list= (List) fPositions.get(category);
		if (list == null)
			return false;
		
		int size= list.size();
		if (size == 0)
			return false;
		
		int index= computeIndexInPositionList(list, offset);
		if (index < size) {
			Position p= (Position) list.get(index);
			while (p != null && p.offset == offset) {
				if (p.length == length)
					return true;
				++ index;
				p= (index < size) ? (Position) list.get(index) : null;
			}
		}
		
		return false;
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#containsPositionCategory(java.lang.String)
	 */
	public boolean containsPositionCategory(String category) {
		if (category != null)
			return fPositions.containsKey(category);
		return false;
	}
	
	
	/**
	 * Computes the index in the list of positions at which a position with the given
	 * offset would be inserted. The position is supposed to become the first in this list
	 * of all positions with the same offset.
	 *
	 * @param positions the list in which the index is computed
	 * @param offset the offset for which the index is computed
	 * @return the computed index
	 *
	 * @see IDocument#computeIndexInCategory(String, int)
	 */
	protected int computeIndexInPositionList(List positions, int offset) {
		
		if (positions.size() == 0)
			return 0;

		int left= 0;
		int right= positions.size() -1;
		int mid= 0;
		Position p= null;

		while (left < right) {
			
			mid= (left + right) / 2;
						
			p= (Position) positions.get(mid);
			if (offset < p.getOffset()) {
				if (left == mid)
					right= left;
				else
					right= mid -1;
			} else if (offset > p.getOffset()) {
				if (right == mid)
					left= right;
				else
					left= mid  +1;
			} else if (offset == p.getOffset()) {
				left= right= mid;
			}

		}

		int pos= left;
		p= (Position) positions.get(pos);
		if (offset > p.getOffset()) {
			// append to the end
			pos++;
		} else {
			// entry will became the first of all entries with the same offset
			do {
				--pos;
				if (pos < 0)
					break;
				p= (Position) positions.get(pos);
			} while (offset == p.getOffset());
			++pos;
		}
			
		Assert.isTrue(0 <= pos && pos <= positions.size());

		return pos;
	}

	/*
	 * @see org.eclipse.jface.text.IDocument#computeIndexInCategory(java.lang.String, int)
	 */
	public int computeIndexInCategory(String category, int offset) throws BadLocationException, BadPositionCategoryException {
		
		if (0 > offset || offset > getLength())
			throw new BadLocationException();
			
		List c= (List) fPositions.get(category);
		if (c == null)
			throw new BadPositionCategoryException();
		
		return computeIndexInPositionList(c, offset);
	}
		
	/**
	 * Fires the document partitioning changed notification to all registered 
	 * document partitioning listeners. Uses a robust iterator.
	 * 
	 * @deprecated use <code>fireDocumentPartitioningChanged(IRegion)</code> instead
	 */
	protected void fireDocumentPartitioningChanged() {
		
		if (fDocumentPartitioningListeners != null && fDocumentPartitioningListeners.size() > 0) {
			
			List list= new ArrayList(fDocumentPartitioningListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentPartitioningListener l= (IDocumentPartitioningListener) e.next();
				l.documentPartitioningChanged(this);
			}
		}
	}
	
	/**
	 * Fires the document partitioning changed notification to all registered 
	 * document partitioning listeners. Uses a robust iterator.
	 * 
	 * @param region the region in which partitioning has changed
	 * 
	 * @see IDocumentPartitioningListenerExtension
	 * @since 2.0
	 * @deprecated use <code>fireDocumentPartitioningChanged(DocumentPartitioningChangedEvent)</code> instead
	 */
	protected void fireDocumentPartitioningChanged(IRegion region) {
		
		if (fDocumentPartitioningListeners != null && fDocumentPartitioningListeners.size() > 0) {
			
			List list= new ArrayList(fDocumentPartitioningListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentPartitioningListener l= (IDocumentPartitioningListener) e.next();
				if (l instanceof IDocumentPartitioningListenerExtension)
					((IDocumentPartitioningListenerExtension) l).documentPartitioningChanged(this, region);
				else
					l.documentPartitioningChanged(this);
			}
		}
	}
	
	/**
	 * Fires the document partitioning changed notification to all registered 
	 * document partitioning listeners. Uses a robust iterator.
	 * 
	 * @param event the document partitioning changed event
	 * 
	 * @see IDocumentPartitioningListenerExtension2
	 * @since 3.0
	 */
	protected void fireDocumentPartitioningChanged(DocumentPartitioningChangedEvent event) {
		if (fDocumentPartitioningListeners == null || fDocumentPartitioningListeners.size() == 0)
			return;
			
		List list= new ArrayList(fDocumentPartitioningListeners);
		Iterator e= list.iterator();
		while (e.hasNext()) {
			IDocumentPartitioningListener l= (IDocumentPartitioningListener) e.next();
			if (l instanceof IDocumentPartitioningListenerExtension2) {
				IDocumentPartitioningListenerExtension2 extension2= (IDocumentPartitioningListenerExtension2) l;
				extension2.documentPartitioningChanged(event);
			} else if (l instanceof IDocumentPartitioningListenerExtension) {
				IDocumentPartitioningListenerExtension extension= (IDocumentPartitioningListenerExtension) l;
				extension.documentPartitioningChanged(this, event.getCoverage());
			} else {
				l.documentPartitioningChanged(this);
			}
		}
		
	}

	/**
	 * Fires the given document event to all registers document listeners informing them
	 * about the forthcoming document manipulation. Uses a robust iterator.
	 *
	 * @param event the event to be sent out
	 */
	protected void fireDocumentAboutToBeChanged(DocumentEvent event) {
		
		// IDocumentExtension
		if (fReentranceCount == 0)
			flushPostNotificationChanges();
					
		if (fDocumentPartitioners != null) {
			Iterator e= fDocumentPartitioners.values().iterator();
			while (e.hasNext()) {
				IDocumentPartitioner p= (IDocumentPartitioner) e.next();
				p.documentAboutToBeChanged(event);
			}
		}
			
		if (fPrenotifiedDocumentListeners.size() > 0) {
			
			List list= new ArrayList(fPrenotifiedDocumentListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentListener l= (IDocumentListener) e.next();
				l.documentAboutToBeChanged(event);
			}
		}
				
		if (fDocumentListeners.size() > 0) {
			
			List list= new ArrayList(fDocumentListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentListener l= (IDocumentListener) e.next();
				l.documentAboutToBeChanged(event);
			}
		}
	}
	
	
	/**
	 * Updates document partitioning and document positions according to the 
	 * specification given by the document event.
	 *
	 * @param event the document event describing the change to which structures must be adapted
	 */
	protected void updateDocumentStructures(DocumentEvent event) {
		
		if (fDocumentPartitioners != null) {
			fDocumentPartitioningChangedEvent= new DocumentPartitioningChangedEvent(this);
			Iterator e= fDocumentPartitioners.keySet().iterator();
			while (e.hasNext()) {
				String partitioning= (String) e.next();
				IDocumentPartitioner partitioner= (IDocumentPartitioner) fDocumentPartitioners.get(partitioning);
				if (partitioner instanceof IDocumentPartitionerExtension) {
					IDocumentPartitionerExtension extension= (IDocumentPartitionerExtension) partitioner;
					IRegion r= extension.documentChanged2(event);
					if (r != null)
						fDocumentPartitioningChangedEvent.setPartitionChange(partitioning, r.getOffset(), r.getLength());
				} else {
					if (partitioner.documentChanged(event))
						fDocumentPartitioningChangedEvent.setPartitionChange(partitioning, 0, event.getDocument().getLength());
				}
			}
		}
		
		if (fPositions.size() > 0)
			updatePositions(event);
	}
	
	/**
	 * Notifies all listeners about the given document change.
	 * Uses a robust iterator. <p>
	 * Executes all registered post notification replace operation.
	 * 
	 * @param event the event to be sent out.
	 */
	protected void doFireDocumentChanged(DocumentEvent event) {
		boolean changed= fDocumentPartitioningChangedEvent != null && !fDocumentPartitioningChangedEvent.isEmpty();
		IRegion change= changed ? fDocumentPartitioningChangedEvent.getCoverage() : null;
		doFireDocumentChanged(event, changed, change);
	}
	
	/**
	 * Notifies all listeners about the given document change.
	 * Uses a robust iterator. <p>
	 * Executes all registered post notification replace operation.
	 * 
	 * @param event the event to be sent out
	 * @param firePartitionChange <code>true</code> if a partition change notification should be sent
	 * @param partitionChange the region whose partitioning changed
	 * @since 2.0
	 * @deprecated use doFireDocumentChanged2(DocumentEvent) instead; this method will be removed
	 */
	protected void doFireDocumentChanged(DocumentEvent event, boolean firePartitionChange, IRegion partitionChange) {
		doFireDocumentChanged2(event);
	}
	
	/**
	 * Notifies all listeners about the given document change.
	 * Uses a robust iterator. <p>
	 * Executes all registered post notification replace operation.
	 * 
	 * @param event the event to be sent out
	 * @since 3.0
	 * @deprecated this method will be renamed to <code>doFireDocumentChanged</code>
	 */
	protected void doFireDocumentChanged2(DocumentEvent event) {
		
		DocumentPartitioningChangedEvent p= fDocumentPartitioningChangedEvent;
		fDocumentPartitioningChangedEvent= null;
		if (p != null && !p.isEmpty())
			fireDocumentPartitioningChanged(p);
		
		if (fPrenotifiedDocumentListeners.size() > 0) {
			
			List list= new ArrayList(fPrenotifiedDocumentListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentListener l= (IDocumentListener) e.next();
				l.documentChanged(event);
			}
		}
		
		if (fDocumentListeners.size() > 0) {
			
			List list= new ArrayList(fDocumentListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IDocumentListener l= (IDocumentListener) e.next();
				l.documentChanged(event);
			}
		}
		
		// IDocumentExtension
		++ fReentranceCount;
		try {
			if (fReentranceCount == 1)
				executePostNotificationChanges();
		} finally {
			-- fReentranceCount;
		}
	}
		
	/**
	 * Updates the internal document structures and informs all document listeners
	 * if listener notification has been enabled. Otherwise it remembers the event
	 * to be sent to the listeners on resume.
	 * 
	 * @param event the document event to be sent out
	 */
	protected void fireDocumentChanged(DocumentEvent event) {
		updateDocumentStructures(event);
		
		if (fStoppedListenerNotification == 0)
			doFireDocumentChanged(event);
		else
			fDeferredDocumentEvent= event;
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#getChar(int)
	 */
	public char getChar(int pos) throws BadLocationException {
		if ((0 > pos) || (pos >= getLength()))
			throw new BadLocationException();
		return getStore().get(pos);
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#getContentType(int)
	 */
	public String getContentType(int offset) throws BadLocationException {
		String contentType= null;
		try {
			contentType= getContentType(DEFAULT_PARTITIONING, offset);
			Assert.isNotNull(contentType);
		} catch (BadPartitioningException e) {
			Assert.isTrue(false);
		}
		return contentType;
	}
	
	/*
	 * @see IDocument#getLegalContentTypes()
	 */
	public String[] getLegalContentTypes() {
		String[] contentTypes= null;
		try {
			contentTypes= getLegalContentTypes(DEFAULT_PARTITIONING);
			Assert.isNotNull(contentTypes);
		} catch (BadPartitioningException e) {
			Assert.isTrue(false);
		}
		return contentTypes;
	}
		
	/*
	 * @see IDocument#getLength()
	 */
	public int getLength() {
		return getStore().getLength();
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#getLineDelimiter(int)
	 */
	public String getLineDelimiter(int line) throws BadLocationException {
		return getTracker().getLineDelimiter(line);
	}
	
	/*
	 * @see IDocument#getLegalLineDelimiters()
	 */
	public String[] getLegalLineDelimiters() {
		return getTracker().getLegalLineDelimiters();
	}
	
	/*
	 * @see IDocument#getLineLength(int)
	 */
	public int getLineLength(int line) throws BadLocationException {
		return getTracker().getLineLength(line);
	}
	
	/*
	 * @see IDocument#getLineOfOffset(int)
	 */
	public int getLineOfOffset(int pos) throws BadLocationException {
		return getTracker().getLineNumberOfOffset(pos);
	}
	
	/*
	 * @see IDocument#getLineOffset(int)
	 */
	public int getLineOffset(int line) throws BadLocationException {
		return getTracker().getLineOffset(line);
	}
	
	/*
	 * @see IDocument#getLineInformation(int)
	 */
	public IRegion getLineInformation(int line) throws BadLocationException {
		return getTracker().getLineInformation(line);
	}
	
	/*
	 * @see IDocument#getLineInformationOfOffset(int)
	 */
	public IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
		return getTracker().getLineInformationOfOffset(offset);
	}
	
	/*
	 * @see IDocument#getNumberOfLines()
	 */
	public int getNumberOfLines() {
		return getTracker().getNumberOfLines();
	}
	
	/*
	 * @see IDocument#getNumberOfLines(int, int)
	 */
	public int getNumberOfLines(int offset, int length) throws BadLocationException {
		return getTracker().getNumberOfLines(offset, length);
	}
	
	/*
	 * @see IDocument#computeNumberOfLines(String)
	 */
	public int computeNumberOfLines(String text) {
		return getTracker().computeNumberOfLines(text);
	}
	
	/*
	 * @see IDocument#getPartition(int)
	 */
	public ITypedRegion getPartition(int offset) throws BadLocationException {
		ITypedRegion partition= null;
		try {
			partition= getPartition(DEFAULT_PARTITIONING, offset);
			Assert.isNotNull(partition);
		} catch (BadPartitioningException e) {
			Assert.isTrue(false);
		}
		return  partition;
	}
	
	/*
	 * @see IDocument#computePartitioning(int, int)
	 */
	public ITypedRegion[] computePartitioning(int offset, int length) throws BadLocationException {
		ITypedRegion[] partitioning= null;
		try {
			partitioning= computePartitioning(DEFAULT_PARTITIONING, offset, length);
			Assert.isNotNull(partitioning);
		} catch (BadPartitioningException e) {
			Assert.isTrue(false);
		}
		return partitioning;
	}
	
	/*
	 * @see IDocument#getPositions(String)
	 */
	public Position[] getPositions(String category) throws BadPositionCategoryException {
		
		if (category == null)
			throw new BadPositionCategoryException();
			
		List c= (List) fPositions.get(category);
		if (c == null)
			throw new BadPositionCategoryException();
		
		Position[] positions= new Position[c.size()];
		c.toArray(positions);
		return positions;
	}
	
	/*
	 * @see IDocument#getPositionCategories()
	 */
	public String[] getPositionCategories() {
		String[] categories= new String[fPositions.size()];
		Iterator keys= fPositions.keySet().iterator();
		for (int i= 0; i < categories.length; i++)
			categories[i]= (String) keys.next();
		return categories;
	}
	
	/*
	 * @see IDocument#getPositionUpdaters()
	 */
	public IPositionUpdater[] getPositionUpdaters() {
		IPositionUpdater[] updaters= new IPositionUpdater[fPositionUpdaters.size()];
		fPositionUpdaters.toArray(updaters);
		return updaters;
	}
		
	/*
	 * @see IDocument#get()
	 */
	public String get() {
		return getStore().get(0, getLength());
	}
	
	/*
	 * @see IDocument#get(int, int)
	 */
	public String get(int pos, int length) throws BadLocationException {
		int myLength= getLength();
		if ((0 > pos) || (0 > length) || (pos + length > myLength))
			throw new BadLocationException();
		return getStore().get(pos, length);
	}
		
	/*
	 * @see IDocument#insertPositionUpdater(IPositionUpdater, int)
	 */
	public void insertPositionUpdater(IPositionUpdater updater, int index) {

		for (int i= fPositionUpdaters.size() - 1; i >= 0; i--) {
			if (fPositionUpdaters.get(i) == updater)
				return;
		} 
		
		if (index == fPositionUpdaters.size())
			fPositionUpdaters.add(updater);
		else
			fPositionUpdaters.add(index, updater);
	}
 		
	/*
	 * @see org.eclipse.jface.text.IDocument#removePosition(java.lang.String, org.eclipse.jface.text.Position)
	 */
	public void removePosition(String category, Position position) throws BadPositionCategoryException {
		
		if (position == null)
			return;

		if (category == null)
			throw new BadPositionCategoryException();
			
		List c= (List) fPositions.get(category);
		if (c == null)
			throw new BadPositionCategoryException();
			
		// remove based on identity not equality
		int size= c.size();
		for (int i= 0; i < size; i++) {
			if (position == c.get(i)) {
				c.remove(i);
				return;
			}
		}
	}
	
	/*
	 * @see IDocument#removePosition(Position)
	 */
	public void removePosition(Position position) {
		try {
			removePosition(DEFAULT_CATEGORY, position);
		} catch (BadPositionCategoryException e) {
		}
	}
		
	/*
	 * @see IDocument#removePositionCategory(String)
	 */
	public void removePositionCategory(String category) throws BadPositionCategoryException {

		if (category == null)
			return;
		
		if ( !containsPositionCategory(category))
			throw new BadPositionCategoryException();

		fPositions.remove(category);
	}
		
	/*
	 * @see IDocument#removePositionUpdater(IPositionUpdater)
	 */
	public void removePositionUpdater(IPositionUpdater updater) {
		for (int i= fPositionUpdaters.size() - 1; i >= 0; i--) {
			if (fPositionUpdaters.get(i) == updater) {
				fPositionUpdaters.remove(i);
				return;
			}
		} 
	}
	
	/*
	 * @see IDocument#replace(int, int, String)
	 */
	public void replace(int pos, int length, String text) throws BadLocationException {
		if ((0 > pos) || (0 > length) || (pos + length > getLength()))
			throw new BadLocationException();
			
		DocumentEvent e= new DocumentEvent(this, pos, length, text);
		fireDocumentAboutToBeChanged(e);
				
		getStore().replace(pos, length, text);
		getTracker().replace(pos, length, text);
		 			
		fireDocumentChanged(e);
	}
		
	/*
	 * @see IDocument#set(String)
	 */
	public void set(String text) {
		int length= getStore().getLength();
		DocumentEvent e= new DocumentEvent(this, 0, length, text);
		fireDocumentAboutToBeChanged(e);
		
		getStore().set(text);
		getTracker().set(text);
		
		fireDocumentChanged(e);
	}
		
	/**
	 * Updates all positions of all categories to the change
	 * described by the document event. All registered document
	 * updaters are called in the sequence they have been arranged.
	 * Uses a robust iterator.
	 *
	 * @param event the document event describing the change to which to adapt the positions
	 */
	protected void updatePositions(DocumentEvent event) {
		List list= new ArrayList(fPositionUpdaters);
		Iterator e= list.iterator();
		while (e.hasNext()) {
			IPositionUpdater u= (IPositionUpdater) e.next();
			u.update(event);
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocument#search(int, java.lang.String, boolean, boolean, boolean)
	 */
	public int search(int startPosition, String findString, boolean forwardSearch, boolean caseSensitive, boolean wholeWord) throws BadLocationException {
		try {
			IRegion region= getFindReplaceDocumentAdapter().search(startPosition, findString, forwardSearch, caseSensitive, wholeWord, false);
			if (region == null)
				return -1;
			else
				return region.getOffset();	
		} catch (IllegalStateException ex) {
			return -1;
		} catch (PatternSyntaxException ex) {
			return -1;
		}
	}
	
	/**
	 * Returns the find/replace adapter for this document.
	 * 
	 * @return this document's find/replace document adapter
	 * @since 3.0
	 */
	private FindReplaceDocumentAdapter getFindReplaceDocumentAdapter() {
		if (fFindReplaceDocumentAdapter == null)
			fFindReplaceDocumentAdapter=  new FindReplaceDocumentAdapter(this);
		
		return fFindReplaceDocumentAdapter;
	}
	
	/**
	 * Flushes all registered post notification changes.
	 * 
	 * @since 2.0
	 */
	private void flushPostNotificationChanges() {
		if (fPostNotificationChanges != null)
			fPostNotificationChanges.clear();
	}
	
	/**
	 * Executes all registered post notification changes. The process is
	 * repeated until no new post notification changes are added.
	 * 
	 * @since 2.0
	 */
	private void executePostNotificationChanges() {
		
		if (fStoppedCount > 0)
			return;
			
		while (fPostNotificationChanges != null) {
			List changes= fPostNotificationChanges;
			fPostNotificationChanges= null;
			
			Iterator e= changes.iterator();
			while (e.hasNext()) {
				RegisteredReplace replace = (RegisteredReplace) e.next();
				replace.fReplace.perform(this, replace.fOwner);
			}
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocumentExtension2#acceptPostNotificationReplaces()
	 * @since 2.1
	 */
	public void acceptPostNotificationReplaces() {
		fAcceptPostNotificationReplaces= true;
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocumentExtension2#ignorePostNotificationReplaces()
	 * @since 2.1
	 */
	public void ignorePostNotificationReplaces() {
		fAcceptPostNotificationReplaces= false;
	}
	
	/*
	 * @see IDocumentExtension#registerPostNotificationReplace(IDocumentListener, IDocumentExtension.IReplace)
	 * @since 2.0
	 */
	public void registerPostNotificationReplace(IDocumentListener owner, IDocumentExtension.IReplace replace) {
		if (fAcceptPostNotificationReplaces) {
			if (fPostNotificationChanges == null)
				fPostNotificationChanges= new ArrayList(1);
			fPostNotificationChanges.add(new RegisteredReplace(owner, replace));
		}
	}
	
	/*
	 * @see IDocumentExtension#stopPostNotificationProcessing()
	 * @since 2.0
	 */
	public void stopPostNotificationProcessing() {
		++ fStoppedCount;
	}
	
	/*
	 * @see IDocumentExtension#resumePostNotificationProcessing()
	 * @since 2.0
	 */
	public void resumePostNotificationProcessing() {
		-- fStoppedCount;
		if (fStoppedCount == 0 && fReentranceCount == 0)
			executePostNotificationChanges();
	}
	
	/*
	 * @see IDocumentExtension#startSequentialRewrite(boolean)
	 * @since 2.0
	 */
	public void startSequentialRewrite(boolean normalized) {
	}

	/*
	 * @see IDocumentExtension#stopSequentialRewrite()
	 * @since 2.0
	 */
	public void stopSequentialRewrite() {
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocumentExtension2#resumeListenerNotification()
	 * @since 2.1
	 */
	public void resumeListenerNotification() {
		-- fStoppedListenerNotification;
		if (fStoppedListenerNotification == 0) {
			resumeDocumentListenerNotification();
		}
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension2#stopListenerNotification()
	 * @since 2.1
	 */
	public void stopListenerNotification() {
		++ fStoppedListenerNotification;
	}
	
	/**
	 * Resumes the document listener notification by sending out the remembered
	 * partition changed and document event.
	 * 
	 * @since 2.1
	 */
	private void resumeDocumentListenerNotification() {
		if (fDeferredDocumentEvent != null) {
			DocumentEvent event= fDeferredDocumentEvent;
			fDeferredDocumentEvent= null;
			doFireDocumentChanged(event);
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#computePartitioning(java.lang.String, int, int)
	 * @since 3.0
	 */
	public ITypedRegion[] computePartitioning(String partitioning, int offset, int length) throws BadLocationException, BadPartitioningException {
		if ((0 > offset) || (0 > length) || (offset + length > getLength()))
			throw new BadLocationException();
		IDocumentPartitioner partitioner= getDocumentPartitioner(partitioning);
		if (partitioner != null)
			return partitioner.computePartitioning(offset, length);
		if (DEFAULT_PARTITIONING.equals(partitioning))
			return new TypedRegion[] { new TypedRegion(offset, length, DEFAULT_CONTENT_TYPE) };
		throw new BadPartitioningException();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#getContentType(java.lang.String, int)
	 * @since 3.0
	 */
	public String getContentType(String partitioning, int offset) throws BadLocationException, BadPartitioningException {
		if ((0 > offset) || (offset > getLength()))
			throw new BadLocationException();
		IDocumentPartitioner partitioner= getDocumentPartitioner(partitioning);
		if (partitioner != null)
			return partitioner.getContentType(offset);
		if (DEFAULT_PARTITIONING.equals(partitioning))
			return DEFAULT_CONTENT_TYPE;
		throw new BadPartitioningException();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#getDocumentPartitioner(java.lang.String)
	 * @since 3.0
	 */
	public IDocumentPartitioner getDocumentPartitioner(String partitioning)  {
		return fDocumentPartitioners != null ? (IDocumentPartitioner) fDocumentPartitioners.get(partitioning) : null;
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#getLegalContentTypes(java.lang.String)
	 * @since 3.0
	 */
	public String[] getLegalContentTypes(String partitioning) throws BadPartitioningException {
		IDocumentPartitioner partitioner= getDocumentPartitioner(partitioning);
		if (partitioner != null)
			return partitioner.getLegalContentTypes();
		if (DEFAULT_PARTITIONING.equals(partitioning))
			return new String[] { DEFAULT_CONTENT_TYPE };
		throw new BadPartitioningException();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#getPartition(java.lang.String, int)
	 * @since 3.0
	 */
	public ITypedRegion getPartition(String partitioning, int offset) throws BadLocationException, BadPartitioningException {
		if ((0 > offset) || (offset > getLength()))
			throw new BadLocationException();
		IDocumentPartitioner partitioner= getDocumentPartitioner(partitioning);
		if (partitioner != null)
			return partitioner.getPartition(offset);
		if (DEFAULT_PARTITIONING.equals(partitioning))
			return new TypedRegion(0, getLength(), DEFAULT_CONTENT_TYPE);
		throw new BadPartitioningException();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#getPartitionings()
	 * @since 3.0
	 */
	public String[] getPartitionings() {
		if (fDocumentPartitioners == null)
			return new String[0];
		String[] partitionings= new String[fDocumentPartitioners.size()];
		fDocumentPartitioners.keySet().toArray(partitionings);
		return partitionings;
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentExtension3#setDocumentPartitioner(java.lang.String, org.eclipse.jface.text.IDocumentPartitioner)
	 * @since 3.0
	 */
	public void setDocumentPartitioner(String partitioning, IDocumentPartitioner partitioner) {
		if (partitioner == null) {
			if (fDocumentPartitioners != null) {
				fDocumentPartitioners.remove(partitioning);
				if (fDocumentPartitioners.size() == 0)
					fDocumentPartitioners= null;
			}
		} else {
			if (fDocumentPartitioners == null)
				fDocumentPartitioners= new HashMap();
			fDocumentPartitioners.put(partitioning, partitioner);
		}
		DocumentPartitioningChangedEvent event= new DocumentPartitioningChangedEvent(this);
		event.setPartitionChange(partitioning, 0, getLength());
		fireDocumentPartitioningChanged(event);
	}
	
	/*
	 * @see org.eclipse.jface.text.IRepairableDocument#repairLineInformation()
	 * @since 3.0
	 */
	public void repairLineInformation() {
		getTracker().set(get());
	}
}
