/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.core.internal.filebuffers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.filebuffers.IAnnotationModelManager;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

/**
 * @since 3.0
 */
public class JavaTextFileBuffer extends JavaFileBuffer implements ITextFileBuffer {
	
	
	private class DocumentListener implements IDocumentListener {

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
			fCanBeSaved= true;
			removeFileBufferContentListeners();
			fManager.fireDirtyStateChanged(JavaTextFileBuffer.this, fCanBeSaved);
		}
	}
	
	/**
	 * Reader chunk size.
	 */
	static final private int READER_CHUNK_SIZE= 2048;
	/**
	 * Buffer size.
	 */
	static final private int BUFFER_SIZE= 8 * READER_CHUNK_SIZE;
	/**
	 * Constant for representing the ok status. This is considered a value object.
	 */
	static final private IStatus STATUS_OK= new Status(IStatus.OK, FileBuffersPlugin.PLUGIN_ID, IStatus.OK, "OK", null);
	/**
	 * Constant for representing the error status. This is considered a value object.
	 */
	static final private IStatus STATUS_ERROR= new Status(IStatus.ERROR, FileBuffersPlugin.PLUGIN_ID, IStatus.INFO, "Error", null);

	
	
	/** The element's document */
	protected IDocument fDocument;
	/** The encoding used to create the document from the storage or <code>null</code> for workbench encoding. */
	protected String fEncoding;
	/** Internal document listener */
	protected IDocumentListener fDocumentListener= new DocumentListener();
	/** The annotation model manager */
	protected IAnnotationModelManager fAnnotationModelManager;



	public JavaTextFileBuffer(TextFileBufferManager manager) {
		super(manager);
	}

	/*
	 * @see org.eclipse.core.buffer.text.IBufferedTextFile#getDocument()
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/*
	 * @see org.eclipse.core.buffer.text.IBufferedTextFile#getEncoding()
	 */
	public String getEncoding() {
		return fEncoding;
	}

	/*
	 * @see org.eclipse.core.buffer.text.IBufferedTextFile#setEncoding(java.lang.String)
	 */
	public void setEncoding(String encoding) {
		fEncoding= encoding;
	}
	
	/*
	 * @see org.eclipse.core.buffer.text.IBufferedFile#getStatus()
	 */
	public IStatus getStatus() {
		if (!isDisposed()) {
			if (fStatus != null)
				return fStatus;
			return (fDocument == null ? STATUS_ERROR : STATUS_OK);
		}
		return STATUS_ERROR;	
	}
	
	private InputStream getFileContents(IProgressMonitor monitor) {
		try {
			return new FileInputStream(fFile);
		} catch (FileNotFoundException e) {
		}
		return null;
	}
	
	private void setFileContents(InputStream stream, boolean overwrite, IProgressMonitor monitor) {
		try {
			OutputStream out= new FileOutputStream(fFile, false);
			
			try {
				byte[] buffer = new byte[8192];
				while (true) {
					int bytesRead = -1;
					try {
						bytesRead = stream.read(buffer);
					} catch (IOException e) {
					}
					if (bytesRead == -1)
						break;
					try {
						out.write(buffer, 0, bytesRead);
					} catch (IOException e) {
					}
					monitor.worked(1);
				}
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
				} finally {
					try {
						out.close();
					} catch (IOException e) {
					}
				}
			}		
		} catch (FileNotFoundException e) {
		}
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#revert(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void revert(IProgressMonitor monitor) throws CoreException {
		if (isDisposed())
			return;
		
		IDocument original= null;
		IStatus status= null;
		
		try {
			original= fManager.createEmptyDocument(getLocation());
			setDocumentContent(original, getFileContents(monitor), fEncoding);
		} catch (CoreException x) {
			status= x.getStatus();
		}
			
		fStatus= status;			
			
		if (original != null) {
			
			String originalContents= original.get();
			boolean replaceContents= !originalContents.equals(fDocument.get());
			
			if (replaceContents)  {
				fManager.fireBufferContentAboutToBeReplaced(this);
				fDocument.set(original.get());
			}
			
			if (fCanBeSaved) {
				fCanBeSaved= false;
				addFileBufferContentListeners();
			}
			
			if (replaceContents)
				fManager.fireBufferContentReplaced(this);
				
			fManager.fireDirtyStateChanged(this, fCanBeSaved);
		}
	}
	
	/*
	 * @see org.eclipse.core.internal.filebuffers.FileBuffer#addFileBufferContentListeners()
	 */
	protected void addFileBufferContentListeners() {
		if (fDocument != null)
			fDocument.addDocumentListener(fDocumentListener);
	}
	
	/*
	 * @see org.eclipse.core.internal.filebuffers.FileBuffer#removeFileBufferContentListeners()
	 */
	protected void removeFileBufferContentListeners() {
		if (fDocument != null)
			fDocument.removeDocumentListener(fDocumentListener);
	}
	
	/*
	 * @see org.eclipse.core.internal.filebuffers.FileBuffer#initializeFileBufferContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void initializeFileBufferContent(IProgressMonitor monitor) throws CoreException {		
		try {
			fDocument= fManager.createEmptyDocument(getLocation());
			setDocumentContent(fDocument, getFileContents(monitor), fEncoding);
		} catch (CoreException x) {
			fDocument= fManager.createEmptyDocument(getLocation());
			fStatus= x.getStatus();
		}
	}
	
	/*
	 * @see org.eclipse.core.internal.filebuffers.FileBuffer#commitFileBufferContent(org.eclipse.core.runtime.IProgressMonitor, boolean)
	 */
	protected void commitFileBufferContent(IProgressMonitor monitor, boolean overwrite) throws CoreException {
		try {
			
			String encoding= getEncoding();
			if (encoding == null)
				encoding= fManager.getDefaultEncoding();
				
			InputStream stream= new ByteArrayInputStream(fDocument.get().getBytes(encoding));
									
			if (fFile.exists()) {
								
				if (!overwrite)
					checkSynchronizationState();
							
					
				// here the file synchronizer should actually be removed and afterwards added again. However,
				// we are already inside an operation, so the delta is sent AFTER we have added the listener
				setFileContents(stream, overwrite, monitor);
				// set synchronization stamp to know whether the file synchronizer must become active
				fSynchronizationStamp= fFile.lastModified();
				
				// TODO if there is an annotation model update it here
				
			} else {

//				try {
//					monitor.beginTask("Saving", 2000); //$NON-NLS-1$
//					ContainerGenerator generator = new ContainerGenerator(fFile.getWorkspace(), fFile.getParent().getFullPath());
//					generator.generateContainer(new SubProgressMonitor(monitor, 1000));
//					fFile.create(stream, false, new SubProgressMonitor(monitor, 1000));
//				}
//				finally {
//					monitor.done();
//				}

			}
			
		} catch (IOException x) {
			IStatus s= new Status(IStatus.ERROR, FileBuffersPlugin.PLUGIN_ID, IStatus.OK, x.getMessage(), x);
			throw new CoreException(s);
		}	
	}
	
	/**
	 * Intitializes the given document with the given stream using the given encoding.
	 *
	 * @param document the document to be initialized
	 * @param contentStream the stream which delivers the document content
	 * @param encoding the character encoding for reading the given stream
	 * @exception CoreException if the given stream can not be read
	 */
	private void setDocumentContent(IDocument document, InputStream contentStream, String encoding) throws CoreException {
		Reader in= null;
		try {
			
			if (encoding == null)
				encoding= fManager.getDefaultEncoding();
				
			in= new BufferedReader(new InputStreamReader(contentStream, encoding), BUFFER_SIZE);
			StringBuffer buffer= new StringBuffer(BUFFER_SIZE);
			char[] readBuffer= new char[READER_CHUNK_SIZE];
			int n= in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n= in.read(readBuffer);
			}
			
			document.set(buffer.toString());
		
		} catch (IOException x) {
			String msg= x.getMessage() == null ? "" : x.getMessage(); //$NON-NLS-1$
			IStatus s= new Status(IStatus.ERROR, FileBuffersPlugin.PLUGIN_ID, IStatus.OK, msg, x);
			throw new CoreException(s);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
				}
			}
		}	
	}
	
	/**
	 * Checks whether the given file is synchronized with the the local file system. 
	 * If the file has been changed, a <code>CoreException</code> is thrown.
	 * 
	 * @param file the file to check
	 * @exception CoreException if file has been changed on the file system
	 */
	private void checkSynchronizationState() throws CoreException {
		if (!isSynchronized()) {
			Status status= new Status(IStatus.ERROR, FileBuffersPlugin.PLUGIN_ID, IResourceStatus.OUT_OF_SYNC_LOCAL, "out of sync", null); 
			throw new CoreException(status);
		}
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.ITextFileBuffer#getAnnotationModelManager()
	 */
	public IAnnotationModelManager getAnnotationModelManager() {
		// TODO
		if (fAnnotationModelManager == null)
			fAnnotationModelManager= new AnnotationModelManager();
		return fAnnotationModelManager;
	}
}
