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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.filebuffers.FileBuffers;

/**
 * @since 3.0
 */
public abstract class JavaFileBuffer extends AbstractFileBuffer  {
	
	/** The location */
	protected IPath fLocation;
	/** The element for which the info is stored */
	protected File fFile;
	/** How often the element has been connected */
	protected int fReferenceCount;
	/** Can the element be saved */
	protected boolean fCanBeSaved= false;
	/** The status of this element */
	protected IStatus fStatus;
	/** The time stamp at which this buffer synchronized with the underlying file. */
	protected long fSynchronizationStamp= IFile.NULL_STAMP;
	/** How often the synchronization context has been requested */
	protected int fSynchronizationContextCount;
	/** The text file buffer manager */
	protected TextFileBufferManager fManager;
	
	
	public JavaFileBuffer(TextFileBufferManager manager) {
		super();
		fManager= manager;
	}
	
	abstract protected void addFileBufferContentListeners();
	
	abstract protected void removeFileBufferContentListeners();
	
	abstract protected void initializeFileBufferContent(IProgressMonitor monitor) throws CoreException;
	
	abstract protected void commitFileBufferContent(IProgressMonitor monitor, boolean overwrite) throws CoreException;
	
	/**
	 * Returns the file at the given location or <code>null</code> if
	 * there is no such file.
	 * 
	 * @param location the location
	 * @return the file at the given location
	 */
	private File getFileAtLocation(IPath location) {
		File file=  FileBuffers.getSystemFileAtLocation(location);
		return file.exists() ? file : null;
	}
	
	public void create(IPath location, IProgressMonitor monitor) throws CoreException {
		File file= getFileAtLocation(location);
		if (file == null)
			throw new CoreException(new Status(IStatus.ERROR, FileBuffersPlugin.PLUGIN_ID, IStatus.OK, "File does not exist", null));
		
		fLocation= location;
		fFile= file;
		initializeFileBufferContent(monitor);
		fSynchronizationStamp= fFile.lastModified();
		
		addFileBufferContentListeners();
	}
	
	public void connect() {
		++ fReferenceCount;
	}
	
	public void disconnect() throws CoreException {
		-- fReferenceCount;
	}
	
	/**
	 * Returns whether this file buffer has already been disposed.
	 * 
	 * @return <code>true</code> if already disposed, <code>false</code> otherwise
	 */
	public boolean isDisposed() {
		return fReferenceCount <= 0;
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#getLocation()
	 */
	public IPath getLocation() {
		return fLocation;
	}

	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#commit(org.eclipse.core.runtime.IProgressMonitor, boolean)
	 */
	public void commit(IProgressMonitor monitor, boolean overwrite) throws CoreException {
		if (!isDisposed() && fCanBeSaved) {
			
			fManager.fireStateChanging(this);
			
			try {
				commitFileBufferContent(monitor, overwrite);
			} catch (CoreException x) {
				fManager.fireStateChangeFailed(this);
				throw x;
			} catch (RuntimeException x) {
				fManager.fireStateChangeFailed(this);
				throw x;				
			}
			
			fCanBeSaved= false;
			addFileBufferContentListeners();
			fManager.fireDirtyStateChanged(this, fCanBeSaved);
		}
	}

	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#isDirty()
	 */
	public boolean isDirty() {
		return fCanBeSaved;
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#isShared()
	 */
	public boolean isShared() {
		return fReferenceCount > 1;
	}

	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#validateState(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object)
	 */
	public void validateState(IProgressMonitor monitor, Object computationContext) throws CoreException {
		// nop
	}

	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#isStateValidated()
	 */
	public boolean isStateValidated() {
		return true;
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#resetStateValidation()
	 */
	public void resetStateValidation() {
		// nop
	}

	/**
	 * Sends out the notification that the file serving as document input has been moved.
	 * 
	 * @param newLocation the path of the new location of the file
	 */
	protected void handleFileMoved(IPath newLocation) {
		fManager.fireUnderlyingFileMoved(this, newLocation);
	}

	/**
	 * Defines the standard procedure to handle <code>CoreExceptions</code>. Exceptions
	 * are written to the plug-in log.
	 *
	 * @param exception the exception to be logged
	 * @param message the message to be logged
	 */
	protected void handleCoreException(CoreException exception) {
		ILog log= Platform.getPlugin(FileBuffersPlugin.PLUGIN_ID).getLog();
		log.log(exception.getStatus());
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#isSynchronized()
	 */
	public boolean isSynchronized() {
		return fSynchronizationStamp == fFile.lastModified();
	}
	
	/*
	 * @see org.eclipse.core.filebuffers.IFileBuffer#getModifcationStamp()
	 */
	public long getModifcationStamp() {
		return fFile.lastModified();
	}
	
	/**
	 * Requests the file buffer manager's synchronization context for this file buffer.
	 */
	public void requestSynchronizationContext() {
		++ fSynchronizationContextCount;
	}
	
	/**
	 * Releases the file buffer manager's synchronization context for this file buffer.
	 */
	public void releaseSynchronizationContext() {
		-- fSynchronizationContextCount;
	}
}
