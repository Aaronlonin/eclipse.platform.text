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

package org.eclipse.ui.editors.text;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;



/**
 * Shareable document provider specialized for file resources (<code>IFile</code>).
 * <p>
 * This class may be instantiated or be subclassed.</p>
 */
public class FileDocumentProvider extends StorageDocumentProvider {

	/**
	 * Qualified name for the encoding key.
	 * 
	 * @since 2.1
	 */
	private static final QualifiedName ENCODING_KEY = new QualifiedName("org.eclipse.ui.editors", "encoding"); //$NON-NLS-1$ //$NON-NLS-2$
	/** 
	 * The runnable context for that provider.
	 * @since 3.0
	 */
	private WorkspaceOperationRunner fOperationRunner;
	
	/**
	 * Runnable encapsulating an element state change. This runnable ensures 
	 * that a element change failed message is sent out to the element state listeners
	 * in case an exception occurred.
	 * 
	 * @since 2.0
	 */
	protected class SafeChange implements Runnable {
		
		/** The input that changes. */
		private IFileEditorInput fInput;
		
		/**
		 * Creates a new safe runnable for the given input.
		 * 
		 * @param input the input
		 */
		public SafeChange(IFileEditorInput input) {
			fInput= input;
		}
		
		/** 
		 * Execute the change.
		 * Subclass responsibility.
		 * 
		 * @param input the input
		 * @exception an exception in case of error
		 */
		protected void execute(IFileEditorInput input) throws Exception {
		}
		
		/*
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			
			if (getElementInfo(fInput) == null) {
				fireElementStateChangeFailed(fInput);
				return;
			}
			
			try {
				execute(fInput);
			} catch (Exception e) {
				fireElementStateChangeFailed(fInput);
			}
		}
	}
	
	
	/**
	 * Synchronizes the document with external resource changes.
	 */
	protected class FileSynchronizer implements IResourceChangeListener, IResourceDeltaVisitor {
		
		/** The file editor input. */
		protected IFileEditorInput fFileEditorInput;
		/**
		 * A flag indicating whether this synchronizer is installed or not.
		 * 
		 * @since 2.1
		 */
		protected boolean fIsInstalled= false;
		
		/**
		 * Creates a new file synchronizer. Is not yet installed on a resource.
		 * 
		 * @param fileEditorInput the editor input to be synchronized
		 */
		public FileSynchronizer(IFileEditorInput fileEditorInput) {
			fFileEditorInput= fileEditorInput;
		}
		
		/**
		 * Creates a new file synchronizer which is not yet installed on a resource.
		 * 
		 * @param fileEditorInput the editor input to be synchronized
		 * @deprecated use FileSynchronizer(IFileEditorInput)
		 */
		public FileSynchronizer(FileEditorInput fileEditorInput) {
			fFileEditorInput= fileEditorInput;
		}
		
		/**
		 * Returns the file wrapped by the file editor input.
		 * 
		 * @return the file wrapped by the editor input associated with that synchronizer
		 */
		protected IFile getFile() {
			return fFileEditorInput.getFile();
		}
		
		/**
		 * Installs the synchronizer on the input's file.
		 */
		public void install() {
			getFile().getWorkspace().addResourceChangeListener(this);
			fIsInstalled= true;
		}
		
		/**
		 * Uninstalls the synchronizer from the input's file.
		 */
		public void uninstall() {
			getFile().getWorkspace().removeResourceChangeListener(this);
			fIsInstalled= false;
		}
		
		/*
		 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
		 */
		public void resourceChanged(IResourceChangeEvent e) {
			IResourceDelta delta= e.getDelta();
			try {
				if (delta != null && fIsInstalled)
					delta.accept(this);
			} catch (CoreException x) {
				handleCoreException(x, TextEditorMessages.getString("FileDocumentProvider.resourceChanged")); //$NON-NLS-1$
			}
		}
		
		/*
		 * @see IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
						
			if (delta != null && getFile().equals(delta.getResource())) {
				
				Runnable runnable= null;
				
				switch (delta.getKind()) {
					case IResourceDelta.CHANGED:
						if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
							FileInfo info= (FileInfo) getElementInfo(fFileEditorInput);
							if (info != null && !info.fCanBeSaved && computeModificationStamp(getFile()) != info.fModificationStamp) {
								runnable= new SafeChange(fFileEditorInput) {
									protected void execute(IFileEditorInput input) throws Exception {
										handleElementContentChanged(input);
									}
								};
							}
						}
						break;
					case IResourceDelta.REMOVED:
						if ((IResourceDelta.MOVED_TO & delta.getFlags()) != 0) {
							final IPath path= delta.getMovedToPath();
							runnable= new SafeChange(fFileEditorInput) {
								protected void execute(IFileEditorInput input) throws Exception {
									handleElementMoved(input, path);
								}
							};
						} else {
							FileInfo info= (FileInfo) getElementInfo(fFileEditorInput);
							if (info != null && !info.fCanBeSaved) {
								runnable= new SafeChange(fFileEditorInput) {
									protected void execute(IFileEditorInput input) throws Exception {
										handleElementDeleted(input);
									}
								};
							}
						}
						break;
				}
				
				if (runnable != null)
					update(runnable);
			}
			
			return true; // because we are sitting on files anyway
		}
		
		/**
		 * Posts the update code "behind" the running operation.
		 *
		 * @param runnable the update code
		 */
		protected void update(Runnable runnable) {
			
			if (runnable instanceof SafeChange)
				fireElementStateChanging(fFileEditorInput);
			
			IWorkbench workbench= PlatformUI.getWorkbench();
			IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
			if (windows != null && windows.length > 0) {
				Display display= windows[0].getShell().getDisplay();
				display.asyncExec(runnable);
			} else {
				runnable.run();
			}
		}
	}
	
	
	
	/**
	 * Bundle of all required information to allow files as underlying document resources. 
	 */
	protected class FileInfo extends StorageInfo {
		
		/** The file synchronizer. */
		public FileSynchronizer fFileSynchronizer;
		/** The time stamp at which this provider changed the file. */
		public long fModificationStamp= IResource.NULL_STAMP;
		
		/**
		 * Creates and returns a new file info.
		 * 
		 * @param document the document
		 * @param model the annotation model
		 * @param fileSynchronizer the file synchronizer
		 */
		public FileInfo(IDocument document, IAnnotationModel model, FileSynchronizer fileSynchronizer) {
			super(document, model);
			fFileSynchronizer= fileSynchronizer;
		}
	}
	
	
	/**
	 * Creates and returns a new document provider.
	 */
	public FileDocumentProvider() {
		super();
	}
	
	/**
	 * Overrides <code>StorageDocumentProvider#setDocumentContent(IDocument, IEditorInput)</code>.
	 * 
	 * @deprecated use file encoding based version
	 * @since 2.0
	 */
	protected boolean setDocumentContent(IDocument document, IEditorInput editorInput) throws CoreException {
		if (editorInput instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) editorInput).getFile();
			setDocumentContent(document, file.getContents(false));
			return true;
		}
		return super.setDocumentContent(document, editorInput);
	}
	
	/*
	 * @see StorageDocumentProvider#setDocumentContent(IDocument, IEditorInput, String)
	 * @since 2.0
	 */
	protected boolean setDocumentContent(IDocument document, IEditorInput editorInput, String encoding) throws CoreException {
		if (editorInput instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) editorInput).getFile();
			setDocumentContent(document, file.getContents(false), encoding);
			return true;
		}
		return super.setDocumentContent(document, editorInput, encoding);
	}
	
	/*
	 * @see AbstractDocumentProvider#createAnnotationModel(Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			return new ResourceMarkerAnnotationModel(input.getFile());
		}
		
		return super.createAnnotationModel(element);
	}
	
	/**
	 * Checks whether the given resource has been changed on the 
	 * local file system by comparing the actual time stamp with the 
	 * cached one. If the resource has been changed, a <code>CoreException</code>
	 * is thrown.
	 * 
	 * @param cachedModificationStamp the chached modification stamp
	 * @param resource the resource to check
	 * @exception CoreException if resource has been changed on the file system
	 */
	protected void checkSynchronizationState(long cachedModificationStamp, IResource resource) throws CoreException {
		if (cachedModificationStamp != computeModificationStamp(resource)) {
			Status status= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IResourceStatus.OUT_OF_SYNC_LOCAL, TextEditorMessages.getString("FileDocumentProvider.error.out_of_sync"), null); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}
	
	/**
	 * Computes the initial modification stamp for the given resource.
	 * 
	 * @param resource the resource
	 * @return the modification stamp
	 */
	protected long computeModificationStamp(IResource resource) {
		long modificationStamp= resource.getModificationStamp();
		
		IPath path= resource.getLocation();
		if (path == null)
			return modificationStamp;
			
		modificationStamp= path.toFile().lastModified();
		return modificationStamp;
	}
	
	/*
	 * @see IDocumentProvider#getModificationStamp(Object)
	 */
	public long getModificationStamp(Object element) {
		
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			return computeModificationStamp(input.getFile());
		}
		
		return super.getModificationStamp(element);
	}
	
	/*
	 * @see IDocumentProvider#getSynchronizationStamp(Object)
	 */
	public long getSynchronizationStamp(Object element) {
		
		if (element instanceof IFileEditorInput) {
			FileInfo info= (FileInfo) getElementInfo(element);
			if (info != null)
				return info.fModificationStamp;
		}
		
		return super.getSynchronizationStamp(element);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doSynchronize(java.lang.Object)
	 */
	protected void doSynchronize(Object element, IProgressMonitor monitor)  throws CoreException {
		if (element instanceof IFileEditorInput) {
			
			IFileEditorInput input= (IFileEditorInput) element;
			
			FileInfo info= (FileInfo) getElementInfo(element);
			if (info != null) {
				
				if (info.fFileSynchronizer != null) { 
					info.fFileSynchronizer.uninstall();
					refreshFile(input.getFile(), monitor);
					info.fFileSynchronizer.install();			
				} else {
					refreshFile(input.getFile(), monitor);
				}

				handleElementContentChanged((IFileEditorInput) element);
			}
			return;
			
		}
		super.doSynchronize(element, monitor);
	}
	
	/*
	 * @see IDocumentProvider#isDeleted(Object)
	 */
	public boolean isDeleted(Object element) {
		
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			
			IPath path= input.getFile().getLocation();
			if (path == null)
				return true;
				
			return !path.toFile().exists();
		}
		
		return super.isDeleted(element);
	}
	
	/*
	 * @see AbstractDocumentProvider#doSaveDocument(IProgressMonitor, Object, IDocument, boolean)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
		if (element instanceof IFileEditorInput) {
			
			IFileEditorInput input= (IFileEditorInput) element;
			
			try {
			
			String encoding= getEncoding(input);
			if (encoding == null)
				encoding= getDefaultEncoding();
			InputStream stream= new ByteArrayInputStream(document.get().getBytes(encoding));
			IFile file= input.getFile();
									
				if (file.exists()) {
					
					FileInfo info= (FileInfo) getElementInfo(element);
					
					if (info != null && !overwrite)
						checkSynchronizationState(info.fModificationStamp, file);
					
					// inform about the upcoming content change
					fireElementStateChanging(element);
					try {
						file.setContents(stream, overwrite, true, monitor);
					} catch (CoreException x) {
						// inform about failure
						fireElementStateChangeFailed(element);
						throw x;
					} catch (RuntimeException x) {
						// inform about failure
						fireElementStateChangeFailed(element);
						throw x;
					}
					
					// If here, the editor state will be flipped to "not dirty".
					// Thus, the state changing flag will be reset.
					
					if (info != null) {
											
						ResourceMarkerAnnotationModel model= (ResourceMarkerAnnotationModel) info.fModel;
						model.updateMarkers(info.fDocument);
						
						info.fModificationStamp= computeModificationStamp(file);
					}
					
				} else {
					try {
						monitor.beginTask(TextEditorMessages.getString("FileDocumentProvider.task.saving"), 2000); //$NON-NLS-1$
						ContainerGenerator generator = new ContainerGenerator(file.getParent().getFullPath());
						generator.generateContainer(new SubProgressMonitor(monitor, 1000));
						file.create(stream, false, new SubProgressMonitor(monitor, 1000));
					}
					finally {
						monitor.done();
					}
				}
				
			} catch (IOException x) {
				IStatus s= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, x.getMessage(), x);
				throw new CoreException(s);
			}
			
		} else {
			super.doSaveDocument(monitor, element, document, overwrite);
		}
	}
	
	/*
	 * @see AbstractDocumentProvider#createElementInfo(Object)
	 */
	protected ElementInfo createElementInfo(Object element) throws CoreException {
		if (element instanceof IFileEditorInput) {
			
			IFileEditorInput input= (IFileEditorInput) element;
			
			try {
				refreshFile(input.getFile());
			} catch (CoreException x) {
				handleCoreException(x,TextEditorMessages.getString("FileDocumentProvider.createElementInfo")); //$NON-NLS-1$
			}
			
			IDocument d= null;
			IStatus s= null;
			
			try {
				d= createDocument(element);
			} catch (CoreException x) {
				s= x.getStatus();
				d= createEmptyDocument();
			}
			
			IAnnotationModel m= createAnnotationModel(element);
			FileSynchronizer f= new FileSynchronizer(input);
			f.install();
			
			FileInfo info= new FileInfo(d, m, f);
			info.fModificationStamp= computeModificationStamp(input.getFile());
			info.fStatus= s;
			info.fEncoding= getPersistedEncoding(input);
			
			return info;
		}
		
		return super.createElementInfo(element);
	}
	
	/*
	 * @see AbstractDocumentProvider#disposeElementInfo(Object, ElementInfo)
	 */
	protected void disposeElementInfo(Object element, ElementInfo info) {
		if (info instanceof FileInfo) {
			FileInfo fileInfo= (FileInfo) info;
			if (fileInfo.fFileSynchronizer != null)
				fileInfo.fFileSynchronizer.uninstall();
		}
		
		super.disposeElementInfo(element, info);
	}	
	
	/**
	 * Updates the element info to a change of the file content and sends out
	 * appropriate notifications.
	 *
	 * @param fileEditorInput the input of an text editor
	 */
	protected void handleElementContentChanged(IFileEditorInput fileEditorInput) {
		FileInfo info= (FileInfo) getElementInfo(fileEditorInput);
		if (info == null)
			return;
		
		IDocument document= createEmptyDocument();
		IStatus status= null;
		
		try {
			
			try {
				refreshFile(fileEditorInput.getFile());
			} catch (CoreException x) {
				handleCoreException(x, "FileDocumentProvider.handleElementContentChanged"); //$NON-NLS-1$
			}
			
			setDocumentContent(document, fileEditorInput, info.fEncoding);
			
		} catch (CoreException x) {
			status= x.getStatus();
		}
		
		String newContent= document.get();
		
		if ( !newContent.equals(info.fDocument.get())) {
			
			// set the new content and fire content related events
			fireElementContentAboutToBeReplaced(fileEditorInput);
			
			removeUnchangedElementListeners(fileEditorInput, info);
			
			info.fDocument.removeDocumentListener(info);
			info.fDocument.set(newContent);
			info.fCanBeSaved= false;
			info.fModificationStamp= computeModificationStamp(fileEditorInput.getFile());
			info.fStatus= status;
			
			addUnchangedElementListeners(fileEditorInput, info);
			
			fireElementContentReplaced(fileEditorInput);
			
		} else {
			
			removeUnchangedElementListeners(fileEditorInput, info);
			
			// fires only the dirty state related event
			info.fCanBeSaved= false;
			info.fModificationStamp= computeModificationStamp(fileEditorInput.getFile());
			info.fStatus= status;
			
			addUnchangedElementListeners(fileEditorInput, info);
			
			fireElementDirtyStateChanged(fileEditorInput, false);
		}
	}
	
	/**
	 * Sends out the notification that the file serving as document input has been moved.
	 * 
	 * @param fileEditorInput the input of an text editor
	 * @param path the path of the new location of the file
	 */
	protected void handleElementMoved(IFileEditorInput fileEditorInput, IPath path) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IFile newFile= workspace.getRoot().getFile(path);
		fireElementMoved(fileEditorInput, newFile == null ? null : new FileEditorInput(newFile));
	}
	
	/**
	 * Sends out the notification that the file serving as document input has been deleted.
	 *
	 * @param fileEditorInput the input of an text editor
	 */
	protected void handleElementDeleted(IFileEditorInput fileEditorInput) {
		fireElementDeleted(fileEditorInput);
	}
	
	/*
	 * @see AbstractDocumentProvider#getElementInfo(Object)
	 * It's only here to circumvent visibility issues with certain compilers.
	 */
	protected ElementInfo getElementInfo(Object element) {
		return super.getElementInfo(element);
	}
	
	/*
	 * @see AbstractDocumentProvider#doValidateState(Object, Object)
	 * @since 2.0
	 */
	protected void doValidateState(Object element, Object computationContext) throws CoreException {
		
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			FileInfo info= (FileInfo) getElementInfo(input);
			if (info != null) {
				IFile file= input.getFile();
				if (file.isReadOnly()) { // do not use cached state here
					IWorkspace workspace= file.getWorkspace();
					workspace.validateEdit(new IFile[] { file }, computationContext);
				}
			}
		}
		
		super.doValidateState(element, computationContext);
	}
	
	/*
	 * @see IDocumentProviderExtension#isModifiable(Object)
	 * @since 2.0
	 */
	public boolean isModifiable(Object element) {
		if (!isStateValidated(element)) {
			if (element instanceof IFileEditorInput)
				return true;
		}
		return super.isModifiable(element);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doResetDocument(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doResetDocument(Object element, IProgressMonitor monitor) throws CoreException {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			try {
				refreshFile(input.getFile(), monitor);
			} catch (CoreException x) {
				handleCoreException(x,TextEditorMessages.getString("FileDocumentProvider.resetDocument")); //$NON-NLS-1$
			}
		}
		
		super.doResetDocument(element, monitor);
		
		IAnnotationModel model= getAnnotationModel(element);
		if (model instanceof AbstractMarkerAnnotationModel) {
			AbstractMarkerAnnotationModel markerModel= (AbstractMarkerAnnotationModel) model;
			markerModel.resetMarkers();
		}
	}
	
	/**
	 * Refreshes the given file resource.
	 * 
	 * @param file
	 * @throws  a CoreException if the refresh fails
	 * @since 2.1
	 */
	protected void refreshFile(IFile file) throws CoreException {
		refreshFile(file, getProgressMonitor());
	}
	
	/**
	 * Refreshes the given file resource.
	 * 
	 * @param file the file to be refreshed
	 * @param monitor the progress monitor
	 * @throws  a CoreException if the refresh fails
	 * @since 3.0
	 */
	protected void refreshFile(IFile file, IProgressMonitor monitor) throws CoreException {
		try {
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (OperationCanceledException x) {
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IDocumentProviderExtension3#isSynchronized(java.lang.Object)
	 * @since 3.0
	 */
	public boolean isSynchronized(Object element) {
		if (element instanceof IFileEditorInput) {
			if (getElementInfo(element) != null) {
				IFileEditorInput input= (IFileEditorInput) element;
				IResource resource= input.getFile();
				return resource.isSynchronized(IResource.DEPTH_ZERO);
			}
			return false;
		}
		return super.isSynchronized(element);
	}
	
	// --------------- Encoding support ---------------
	
	/**
	 * Returns the persited encoding for the given element.
	 * 
	 * @param element the element for which to get the persisted encoding
	 * @since 2.1
	 */
	protected String getPersistedEncoding(Object element) {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput editorInput= (IFileEditorInput)element;
			IFile file= editorInput.getFile();
			if (file != null)
				try {
					return file.getPersistentProperty(ENCODING_KEY);
				} catch (CoreException ex) {
					return null;
				}
		}
		return null;
	}

	/**
	 * Persists the given encoding for the given element.
	 * 
	 * @param element the element for which to store the persisted encoding
	 * @param encoding the encoding
	 * @since 2.1
	 */
	protected void persistEncoding(Object element, String encoding) throws CoreException {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput editorInput= (IFileEditorInput)element;
			IFile file= editorInput.getFile();
			if (file != null)
				file.setPersistentProperty(ENCODING_KEY, encoding);
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		if (fOperationRunner == null)
			fOperationRunner = new WorkspaceOperationRunner();
		fOperationRunner.setProgressMonitor(monitor);
		return fOperationRunner;
	}
}
