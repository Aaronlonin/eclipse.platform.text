/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris.Dennis@invidi.com - http://bugs.eclipse.org/bugs/show_bug.cgi?id=29027
 *******************************************************************************/
package org.eclipse.ui.texteditor;


import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IMarkRegionTarget;
import org.eclipse.jface.text.IPostSelectionProvider;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerExtension;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.VerticalRuler;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.EditorPart;

import org.eclipse.ui.internal.ActionDescriptor;
import org.eclipse.ui.internal.EditorPluginAction;
import org.eclipse.ui.internal.texteditor.EditPosition;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;




/**
 * Abstract base implementation of a text editor.
 * <p>
 * Subclasses are responsible for configuring the editor appropriately.
 * The standard text editor, <code>TextEditor</code>, is one such example.</p>
 * <p>
 * If a subclass calls <code>setEditorContextMenuId</code> the arguments is
 * used as the id under which the editor's context menu is registered for extensions.
 * If no id is set, the context menu is registered under <b>[editor_id].EditorContext</b>
 * whereby [editor_id] is replaced with the editor's part id.  If the editor is instructed to
 * run in version 1.0 context menu registration compatibility mode, the latter form of the
 * registration even happens if a context menu id has been set via <code>setEditorContextMenuId</code>.
 * If no id is set while in compatibility mode, the menu is registered under 
 * <code>DEFAULT_EDITOR_CONTEXT_MENU_ID</code>.</p>
 * <p>
 * If a subclass calls <code>setRulerContextMenuId</code> the argument is
 * used as the id under which the ruler's context menu is registered for extensions.
 * If no id is set, the context menu is registered under <b>[editor_id].RulerContext</b>
 * whereby [editor_id] is replaced with the editor's part id.  If the editor is instructed to
 * run in version 1.0 context menu registration compatibility mode, the latter form of the
 * registration even happens if a context menu id has been set via <code>setRulerContextMenuId</code>.
 * If no id is set while in compatibility mode, the menu is registered under
 * <code>DEFAULT_RULER_CONTEXT_MENU_ID</code>.</p>
 *
 * @see org.eclipse.ui.editors.text.TextEditor
 */
public abstract class AbstractTextEditor extends EditorPart implements ITextEditor, IReusableEditor, ITextEditorExtension, ITextEditorExtension2, ITextEditorExtension3, INavigationLocationProvider {

	/**
	 * Tag used in xml configuration files to specify editor action contributions.
	 * Current value: <code>editorContribution</code>
	 * @since 2.0
	 */
	private static final String TAG_CONTRIBUTION_TYPE= "editorContribution"; //$NON-NLS-1$

	/**
	 * The text input listener.
	 * 
	 * @see ITextInputListener
	 * @since 2.1
	 */
	private static class TextInputListener implements ITextInputListener {
		/** Indicates whether the editor input changed during the process of state validation. */
		public boolean inputChanged;

		/** Detectors for editor input changes during the process of state validation. */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {}
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) { inputChanged= true; }
	}

	/**
	 * Internal element state listener.
	 */
	class ElementStateListener implements IElementStateListener, IElementStateListenerExtension {
		
			/**
			 * Internal <code>VerifyListener</code> for performing the state validation of the
			 * editor input in case of the first attempted manipulation via typing on the keyboard.
			 * @since 2.0
			 */
			class Validator implements VerifyListener {
				/*
				 * @see VerifyListener#verifyText(org.eclipse.swt.events.VerifyEvent)
				 */
				public void verifyText(VerifyEvent e) {
					if (! validateEditorInputState())
							e.doit= false;
					}
			};
		
		/**
		 * The listener's validator.
		 * @since 2.0
		 */
		private Validator fValidator;
		
		/*
		 * @see IElementStateListenerExtension#elementStateValidationChanged(Object, boolean)
		 * @since 2.0
		 */
		public void elementStateValidationChanged(Object element, boolean isStateValidated) {

			if (element != null && element.equals(getEditorInput())) {
				
				enableSanityChecking(true);

				if (isStateValidated && fValidator != null) {
					ISourceViewer viewer= getSourceViewer();
					if (viewer != null) {
						StyledText textWidget= viewer.getTextWidget();
						if (textWidget != null && !textWidget.isDisposed())
							textWidget.removeVerifyListener(fValidator);
						fValidator= null;
						enableStateValidation(false);
					}
				} else if (!isStateValidated && fValidator == null) {
					ISourceViewer viewer= getSourceViewer();
					if (viewer != null) {
						StyledText textWidget= viewer.getTextWidget();
						if (textWidget != null && !textWidget.isDisposed()) {
							fValidator= new Validator();
							enableStateValidation(true);
							textWidget.addVerifyListener(fValidator);
						}
					}
				}
				
			}
		}
		
		/*
		 * @see IElementStateListener#elementDirtyStateChanged(Object, boolean)
		 */
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			if (element != null && element.equals(getEditorInput())) {
				enableSanityChecking(true);
				firePropertyChange(PROP_DIRTY);
			}
		}
		
		/*
		 * @see IElementStateListener#elementContentAboutToBeReplaced(Object)
		 */
		public void elementContentAboutToBeReplaced(Object element) {
			if (element != null && element.equals(getEditorInput())) {
				enableSanityChecking(true);
				rememberSelection();
				resetHighlightRange();
			}
		}
		
		/*
		 * @see IElementStateListener#elementContentReplaced(Object)
		 */
		public void elementContentReplaced(Object element) {
			if (element != null && element.equals(getEditorInput())) {
				enableSanityChecking(true);
				firePropertyChange(PROP_DIRTY);
				restoreSelection();
			}
		}
		
		/*
		 * @see IElementStateListener#elementDeleted(Object)
		 */
		public void elementDeleted(Object deletedElement) {
			if (deletedElement != null && deletedElement.equals(getEditorInput())) {
				enableSanityChecking(true);
				close(false);
			}
		}
		
		/*
		 * @see IElementStateListener#elementMoved(Object, Object)
		 */
		public void elementMoved(Object originalElement, Object movedElement) {
						
			if (originalElement != null && originalElement.equals(getEditorInput())) {
				
				enableSanityChecking(true);
				
				if (!canHandleMove((IEditorInput) originalElement, (IEditorInput) movedElement)) {
					close(true);
					return;
				}
			
				if (movedElement == null || movedElement instanceof IEditorInput) {	
					rememberSelection();
										
					IDocumentProvider d= getDocumentProvider();
					IDocument changed= null;
					if (isDirty())
						changed= d.getDocument(getEditorInput());
						
					setInput((IEditorInput) movedElement);
					
					if (changed != null) {
						d.getDocument(getEditorInput()).set(changed.get());
						validateState(getEditorInput());
						updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE);
					}					
						
					restoreSelection();
				}
			}
		}
		
		/*
		 * @see IElementStateListenerExtension#elementStateChanging(Object)
		 * @since 2.0
		 */
		public void elementStateChanging(Object element) {
			if (element != null && element.equals(getEditorInput()))
				enableSanityChecking(false);
		}
		
		/*
		 * @see IElementStateListenerExtension#elementStateChangeFailed(Object)
		 * @since 2.0
		 */
		public void elementStateChangeFailed(Object element) {
			if (element != null && element.equals(getEditorInput()))
				enableSanityChecking(true);
		}
	};
	
	/**
	 * Internal text listener for updating all content dependent
	 * actions. The updating is done asynchronously.
	 */
	class TextListener implements ITextListener {
		
		/** The posted updater code. */
		private Runnable fRunnable= new Runnable() {
			public void run() {
				
				TextEvent textEvent= (TextEvent) fTextEventQueue.remove(0);
				
				if (fSourceViewer != null) {
					// check whether editor has not been disposed yet
					if (fTextEventQueue.isEmpty())
						updateContentDependentActions();
						
					// remember the last edit position
					if (isDirty() && (textEvent.getDocumentEvent() != null)) {
						ISelection sel= getSelectionProvider().getSelection();
						IEditorInput input= getEditorInput();
						Position pos= null;
						if (sel instanceof ITextSelection) {
							int offset= ((ITextSelection)sel).getOffset();
							int length= ((ITextSelection)sel).getLength();
							pos= new Position(offset, length);
							try {
								getDocumentProvider().getDocument(input).addPosition(pos);
							} catch (BadLocationException ex) {
								// pos is null
							}
						}
						TextEditorPlugin.getDefault().setLastEditPosition(new EditPosition(input, getEditorSite().getId(), getSelectionProvider().getSelection(), pos));
					}
				}
			}
		};
		
		/** Display used for posting the updater code. */
		private Display fDisplay;
		/**
		 * Display used for posting the updater code.
		 * @since 2.1
		 */
		private ArrayList fTextEventQueue= new ArrayList(5);
		
		/*
		 * @see ITextListener#textChanged(TextEvent)
		 */
		public void textChanged(TextEvent event) {
			
			/*
			 * Also works for text events which do not base on a DocumentEvent.
			 * This way, if the visible document of the viewer changes, all content
			 * dependent actions are updated as well.
			 */
							
			if (fDisplay == null)
				fDisplay= getSite().getShell().getDisplay();

			fTextEventQueue.add(event);
			fDisplay.asyncExec(fRunnable);
		}
	};
	
	/**
	 * Compare configuration elements according to the prerequisite relation
	 * of their defining plug-ins.
	 * 
	 * @since 2.0
	 */
	static class ConfigurationElementComparator implements Comparator {
		
		/*
		 * @see Comparator#compare(java.lang.Object, java.lang.Object)
		 * @since 2.0
		 */
		public int compare(Object object0, Object object1) {

			IConfigurationElement element0= (IConfigurationElement)object0;
			IConfigurationElement element1= (IConfigurationElement)object1;	
			
			if (dependsOn(element0, element1))
				return -1;
				
			if (dependsOn(element1, element0))
				return +1;
			
			return 0;
		}

		/**
		 * Returns whether one configuration element depends on the other element.
		 * This is done by checking the dependency chain of the defining plug-ins.
		 * 
		 * @param element0 the first element
		 * @param element1 the second element
		 * @return <code>true</code> if <code>element0</code> depends on <code>element1</code>.
		 * @since 2.0
		 */
		private static boolean dependsOn(IConfigurationElement element0, IConfigurationElement element1) {
			IPluginDescriptor descriptor0= element0.getDeclaringExtension().getDeclaringPluginDescriptor();
			IPluginDescriptor descriptor1= element1.getDeclaringExtension().getDeclaringPluginDescriptor();
			
			return dependsOn(descriptor0, descriptor1);
		}
		
		/**
		 * Returns whether one plug-in depends on the other plugin. 
		 * 
		 * @param descriptor0 descriptor of the first plug-in
		 * @param descriptor1 descriptor of the second plug-in
		 * @return <code>true</code> if <code>descriptor0</code> depends on <code>descriptor1</code>.
		 * @since 2.0
		 */
		private static boolean dependsOn(IPluginDescriptor descriptor0, IPluginDescriptor descriptor1) {

			IPluginRegistry registry= Platform.getPluginRegistry();
			IPluginPrerequisite[] prerequisites= descriptor0.getPluginPrerequisites();

			for (int i= 0; i < prerequisites.length; i++) {
				IPluginPrerequisite prerequisite= prerequisites[i];
				String id= prerequisite.getUniqueIdentifier();			
				IPluginDescriptor descriptor= registry.getPluginDescriptor(id);
				
				if (descriptor != null && (descriptor.equals(descriptor1) || dependsOn(descriptor, descriptor1)))
					return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Internal property change listener for handling changes in the editor's preferences.
	 */
	class PropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			handlePreferenceStoreChanged(event);
		}
	};

	/**
	 * Internal property change listener for handling workbench font changes.
	 * @since 2.1
	 */
	class FontPropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (fSourceViewer == null)
				return;
				
			String property= event.getProperty();
			
			if (getFontPropertyPreferenceKey().equals(property))
				initializeViewerFont(fSourceViewer);
		}
	};

	/**
	 * Internal key verify listener for triggering action activation codes.
	 */
	class ActivationCodeTrigger implements VerifyKeyListener {
		
		/** Indicates whether this trigger has been installed. */
		private boolean fIsInstalled= false;
		/**
		 * The key binding service to use.
		 * @since 2.0
		 */
		private IKeyBindingService fKeyBindingService;
		
		/*
		 * @see VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {
							
			ActionActivationCode code= null;
			int size= fActivationCodes.size();
			for (int i= 0; i < size; i++) {
				code= (ActionActivationCode) fActivationCodes.get(i);
				if (code.matches(event)) {
					IAction action= getAction(code.fActionId);
					if (action != null) {
						
						if (action instanceof IUpdate)
							((IUpdate) action).update();
						
						if (!action.isEnabled() && action instanceof IReadOnlyDependent) {
							IReadOnlyDependent dependent= (IReadOnlyDependent) action;
							boolean writable= dependent.isEnabled(true);
							if (writable) {
								event.doit= false;
								return;
							}
						} else if (action.isEnabled()) {
							event.doit= false;
							action.run();
							return;
						}
					}
				}
			}
		}
		
		/**
		 * Installs this trigger on the editor's text widget.
		 * @since 2.0
		 */
		public void install() {
			if (!fIsInstalled) {
				
				if (fSourceViewer instanceof ITextViewerExtension) {
					ITextViewerExtension e= (ITextViewerExtension) fSourceViewer;
					e.prependVerifyKeyListener(this);
				} else {
					StyledText text= fSourceViewer.getTextWidget();
					text.addVerifyKeyListener(this);
				}
				
				fKeyBindingService= getEditorSite().getKeyBindingService(); 
				fIsInstalled= true;
			}
		}
		
		/**
		 * Uninstalls this trigger from the editor's text widget.
		 * @since 2.0
		 */
		public void uninstall() {
			if (fIsInstalled) {
				
				if (fSourceViewer instanceof ITextViewerExtension) {
					ITextViewerExtension e= (ITextViewerExtension) fSourceViewer;
					e.removeVerifyKeyListener(this);
				} else if (fSourceViewer != null) {
					StyledText text= fSourceViewer.getTextWidget();
					if (text != null && !text.isDisposed())
						text.removeVerifyKeyListener(fActivationCodeTrigger);
				}
				
				fIsInstalled= false;
				fKeyBindingService= null;
			}
		}
		
		/**
		 * Registers the given action for key activation.
		 * @param action the action to be registered
		 * @since 2.0
		 */
		public void registerActionForKeyActivation(IAction action) {
			if (action.getActionDefinitionId() != null)
				fKeyBindingService.registerAction(action);
		}
		
		/**
		 * The given action is no longer available for key activation
		 * @param action the action to be unregistered
		 * @since 2.0
		 */
		public void unregisterActionFromKeyActivation(IAction action) {
			if (action.getActionDefinitionId() != null)
				fKeyBindingService.unregisterAction(action);
		}

		/**
		 * Sets the keybindings scopes for this editor.
		 * @param keyBindingScopes the keybinding scopes
		 * @since 2.1
		 */
		public void setScopes(String[] keyBindingScopes) {
			if (keyBindingScopes != null && keyBindingScopes.length > 0)
				fKeyBindingService.setScopes(keyBindingScopes);
		}
	};
	
	/**
	 * Representation of action activation codes.
	 */
	static class ActionActivationCode {

		/** The action id. */		
		public String fActionId;
		/** The character. */
		public char fCharacter;
		/** The key code. */
		public int fKeyCode= -1;
		/** The state mask. */
		public int fStateMask= SWT.DEFAULT;
		
		/**
		 * Creates a new action activation code for the given action id.
		 * @param actionId the action id
		 */
		public ActionActivationCode(String actionId) {
			fActionId= actionId;
		}
		
		/**
		 * Returns <code>true</code> if this activation code matches the given verify event.
		 * @param event the event to test for matching
		 */
		public boolean matches(VerifyEvent event) {
			return (event.character == fCharacter &&
						(fKeyCode == -1 || event.keyCode == fKeyCode) &&
						(fStateMask == SWT.DEFAULT || event.stateMask == fStateMask));
		}		
	};
	
	/**
	 * Internal part and shell activation listener for triggering state validation.
	 * @since 2.0
	 */
	class ActivationListener extends ShellAdapter implements IPartListener {
		
		/** Cache of the active workbench part. */
		private IWorkbenchPart fActivePart;
		/** Indicates whether activation handling is currently be done. */
		private boolean fIsHandlingActivation= false;
		
		/*
		 * @see IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partActivated(IWorkbenchPart part) {
			fActivePart= part;
			handleActivation();
		}
	
		/*
		 * @see IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partBroughtToTop(IWorkbenchPart part) {
		}
	
		/*
		 * @see IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partClosed(IWorkbenchPart part) {
		}
	
		/*
		 * @see IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partDeactivated(IWorkbenchPart part) {
			fActivePart= null;
		}
	
		/*
		 * @see IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
		 */
		public void partOpened(IWorkbenchPart part) {
		}
	
		/*
		 * @see ShellListener#shellActivated(org.eclipse.swt.events.ShellEvent)
		 */
		public void shellActivated(ShellEvent e) {
			/*
			 * Workaround for problem described in 
			 * http://dev.eclipse.org/bugs/show_bug.cgi?id=11731
			 * Will be removed when SWT has solved the problem.
			 */
			e.widget.getDisplay().asyncExec(new Runnable() {
				public void run() {
					handleActivation();
				}
			});
		}
		
		/**
		 * Handles the activation triggering a element state check in the editor.
		 */
		private void handleActivation() {
			if (fIsHandlingActivation)
				return;
				
			if (fActivePart == AbstractTextEditor.this) {
				fIsHandlingActivation= true;
				try {
					safelySanityCheckState(getEditorInput());
				} finally {
					fIsHandlingActivation= false;
				}
			}
		}
	};
	
	/**
	 * Internal interface for a cursor listener. I.e. aggregation 
	 * of mouse and key listener.
	 * @since 2.0
	 */
	interface ICursorListener extends MouseListener, KeyListener {
	};
	
	/**
	 * Maps an action definition id to an StyledText action.
	 * @since 2.0
	 */
	static class IdMapEntry {
		
		/** The action id. */
		private String fActionId;
		/** The StyledText action. */
		private int fAction;
		
		/**
		 * Creates a new mapping. 
		 * @param actionId the action id
		 * @param action the StyledText action
		 */
		public IdMapEntry(String actionId, int action) {
			fActionId= actionId;
			fAction= action;
		}
		
		/**
		 * Returns the action id.
		 * @return the action id
		 */
		public String getActionId() {
			return fActionId;
		}
		
		/**
		 * Returns the action.
		 * @return the action
		 */
		public int getAction() {
			return fAction;
		}
	};
	
	/**
	 * Internal action to scroll the editor's viewer by a specified number of lines.
	 * @since 2.0
	 */
	class ScrollLinesAction extends Action {
		
		/** Number of lines to scroll. */
		private int fScrollIncrement;
		
		/** 
		 * Creates a new scroll action that scroll the given number of lines. If the
		 * increment is &lt 0, it's scrolling up, if &gt 0 it's scrolling down.
		 * @param scrollIncrement the number of lines to scroll
		 */
		public ScrollLinesAction(int scrollIncrement) {
			fScrollIncrement= scrollIncrement;
		}
		
		/*
		 * @see IAction#run()
		 */
		public void run() {
			ISourceViewer viewer= getSourceViewer();
			int topIndex= viewer.getTopIndex();
			int newTopIndex= Math.max(0, topIndex + fScrollIncrement);
			viewer.setTopIndex(newTopIndex);
		}
	};
	
	/**
	 * Action to toggle the insert mode.
	 *  @since 2.1
	 */
	class ToggleInsertModeAction extends TextNavigationAction {
	
		public ToggleInsertModeAction(StyledText textWidget) {
			super(textWidget, ST.TOGGLE_OVERWRITE);
		}
		
		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {	
			switchToNextInsertMode();
		}
	};

	/**
	 * This action implements smart end.
	 * Instead of going to the end of a line it does the following:
	 * - if smart home/end is enabled and the caret is before the line's last non-whitespace and then the caret is moved directly after it 
	 * - if the caret is after last non-whitespace the caret is moved at the end of the line
	 * - if the caret is at the end of the line the caret is moved directly after the line's last non-whitespace character
	 * @since 2.1
	 */
	class LineEndAction extends TextNavigationAction {

		/** boolean flag which tells if the text up to the line end should be selected. */
		private boolean fDoSelect;
		
		/**
		 * Create a new line end action.
		 * 
		 * @param textWidget the styled text widget
		 * @param doSelect a boolean flag which tells if the text up to the line end should be selected
		 */
		public LineEndAction(StyledText textWidget, boolean doSelect) {
			super(textWidget, ST.LINE_END);
			fDoSelect= doSelect;
		}
		
		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			boolean isSmartHomeEndEnabled= false;
			IPreferenceStore store= getPreferenceStore();
			if (store != null)
				isSmartHomeEndEnabled= store.getBoolean(AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END);

			StyledText st= getSourceViewer().getTextWidget();
			if (st == null || st.isDisposed())
				return;
			int caretOffset= st.getCaretOffset();
			int lineNumber= st.getLineAtOffset(caretOffset);
			int lineOffset= st.getOffsetAtLine(lineNumber);
			
			int lineLength;
			try {
				int caretOffsetInDocument= widgetOffset2ModelOffset(getSourceViewer(), caretOffset);
				lineLength= getSourceViewer().getDocument().getLineInformationOfOffset(caretOffsetInDocument).getLength();
			} catch (BadLocationException ex) {
				return;
			}
			int lineEndOffset= lineOffset + lineLength;
			
			int delta= lineEndOffset - st.getCharCount();
			if (delta > 0) {
				lineEndOffset -= delta;
				lineLength -= delta;
			}
			
			String line= ""; //$NON-NLS-1$
			if (lineLength > 0)
				line= st.getText(lineOffset, lineEndOffset - 1);
			int i= lineLength - 1;
			while (i > -1 && Character.isWhitespace(line.charAt(i))) {
				i--;
			}
			i++;

			// Remember current selection
			Point oldSelection= st.getSelection();

			// Compute new caret position
			int newCaretOffset= -1;
			
			if (isSmartHomeEndEnabled) {
				
				if (caretOffset - lineOffset == i)
					// to end of line
					newCaretOffset= lineEndOffset;
				else
					// to end of text
					newCaretOffset= lineOffset + i;
										
			} else {
				
				if (caretOffset < lineEndOffset)
					// to end of line
					newCaretOffset= lineEndOffset;
				
			}
			
			if (newCaretOffset == -1)
				newCaretOffset= caretOffset;
			else
				st.setCaretOffset(newCaretOffset);

			st.setCaretOffset(newCaretOffset);
			if (fDoSelect) {
				if (caretOffset < oldSelection.y)	
					st.setSelection(oldSelection.y, newCaretOffset);
				else
					st.setSelection(oldSelection.x, newCaretOffset);
			} else
				st.setSelection(newCaretOffset);

			// send selection changed event
			Event event= new Event();
			event.x= st.getSelection().x;
			event.y= st.getSelection().y;
			st.notifyListeners(SWT.Selection, event);
		}
	};

	/**
	 * This action implements smart home.
	 * Instead of going to the start of a line it does the following:
	 * - if smart home/end is enabled and the caret is after the line's first non-whitespace then the caret is moved directly before it
	 * - if the caret is before the line's first non-whitespace the caret is moved to the beginning of the line 
	 * - if the caret is at the beginning of the line the caret is moved directly before the line's first non-whitespace character
	 * @since 2.1
	 */
	class LineStartAction extends TextNavigationAction {
		
		/** boolean flag which tells if the text up to the beginning of the line should be selected. */
		private boolean fDoSelect;
		
		/**
		 * Creates a new line start action.
		 * 
		 * @param textWidget the styled text widget
		 * @param doSelect a boolean flag which tells if the text up to the beginning of the line should be selected
		 */
		public LineStartAction(StyledText textWidget, boolean doSelect) {
			super(textWidget, ST.LINE_START);
			fDoSelect= doSelect;
		}
		
		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			boolean isSmartHomeEndEnabled= false;
			IPreferenceStore store= getPreferenceStore();
			if (store != null)
				isSmartHomeEndEnabled= store.getBoolean(AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END);

			StyledText st= getSourceViewer().getTextWidget();
			if (st == null || st.isDisposed())
				return;
		
			int caretOffset= st.getCaretOffset();
			int lineNumber= st.getLineAtOffset(caretOffset);
			int lineOffset= st.getOffsetAtLine(lineNumber);
			
			int lineLength;
			try {
				int caretOffsetInDocument= widgetOffset2ModelOffset(getSourceViewer(), caretOffset);
				lineLength= getSourceViewer().getDocument().getLineInformationOfOffset(caretOffsetInDocument).getLength();
			} catch (BadLocationException ex) {
				return;
			}
			
			String line= ""; //$NON-NLS-1$
			if (lineLength > 0) {
				int end= lineOffset + lineLength - 1;
				end= Math.min(end, st.getCharCount() -1);
				line= st.getText(lineOffset, end);
			}
			int i= 0;
			while (i < lineLength && Character.isWhitespace(line.charAt(i)))
				i++;


			// Remember current selection
			Point oldSelection= st.getSelection();
				
			// Compute new caret position
			int newCaretOffset= -1;
			if (isSmartHomeEndEnabled) {
				
				if (caretOffset - lineOffset == i)
					// to beginning of line
					newCaretOffset= lineOffset;
				else
					// to beginning of text
					newCaretOffset= lineOffset + i;
									
			} else {
				
				if (caretOffset > lineOffset)
					// to beginning of line
					newCaretOffset= lineOffset;
			}
			
			if (newCaretOffset == -1)
				newCaretOffset= caretOffset;
			else
				st.setCaretOffset(newCaretOffset);

			if (fDoSelect) {
				if (caretOffset < oldSelection.y)	
					st.setSelection(oldSelection.y, newCaretOffset);
				else
					st.setSelection(oldSelection.x, newCaretOffset);
			} else
				st.setSelection(newCaretOffset);

			// send selection changed event
			Event event= new Event();
			event.x= st.getSelection().x;
			event.y= st.getSelection().y;
			st.notifyListeners(SWT.Selection, event);
		}

	};
	
	/**
	 * Internal action to show the editor's ruler context menu (accessibility).
	 * @since 2.0
	 */		
	class ShowRulerContextMenuAction extends Action {
		/*
		 * @see IAction#run()
		 */
		public void run() {
			if (fSourceViewer == null)
				return;

			StyledText text= fSourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
					
			Point location= text.getLocationAtOffset(text.getCaretOffset());
			location.x= 0;

			if (fVerticalRuler instanceof IVerticalRulerExtension)
			((IVerticalRulerExtension) fVerticalRuler).setLocationOfLastMouseButtonActivity(location.x, location.y);

			location= text.toDisplay(location);
			fRulerContextMenu.setLocation(location.x, location.y);
			fRulerContextMenu.setVisible(true);
		}		
	};
	
	
	/** 
	 * Editor specific selection provider which wraps the source viewer's selection provider.
	 * @since 2.1
	 */
	class SelectionProvider implements ISelectionProvider, IPostSelectionProvider {
	
		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
		 */
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			if (fSourceViewer != null)
				fSourceViewer.getSelectionProvider().addSelectionChangedListener(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
		 */
		public ISelection getSelection() {
			return doGetSelection();				
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
		 */
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			if (fSourceViewer != null)
				fSourceViewer.getSelectionProvider().removeSelectionChangedListener(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(ISelection)
		 */
		public void setSelection(ISelection selection) {
			doSetSelection(selection);
		}

		/*
		 * @see org.eclipse.jface.text.IPostSelectionProvider#addPostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
			if (fSourceViewer != null) {
				if (fSourceViewer.getSelectionProvider() instanceof IPostSelectionProvider)  {
					IPostSelectionProvider provider= (IPostSelectionProvider) fSourceViewer.getSelectionProvider();
					provider.addPostSelectionChangedListener(listener);
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.IPostSelectionProvider#removePostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
			if (fSourceViewer != null)  {
				if (fSourceViewer.getSelectionProvider() instanceof IPostSelectionProvider)  {
					IPostSelectionProvider provider= (IPostSelectionProvider) fSourceViewer.getSelectionProvider();
					provider.removePostSelectionChangedListener(listener);
				}
			}
		}
	};
	
	/**
	 * Key used to look up font preference.
	 * Value: <code>"org.eclipse.jface.textfont"</code>
	 * 
	 * @deprecated As of 2.1, replaced by {@link JFaceResources#TEXT_FONT}
	 */
	public final static String PREFERENCE_FONT= JFaceResources.TEXT_FONT;
	/** 
	 * Key used to look up foreground color preference.
	 * Value: <code>AbstractTextEditor.Color.Foreground</code>
	 * @since 2.0
	 */
	public final static String PREFERENCE_COLOR_FOREGROUND= "AbstractTextEditor.Color.Foreground"; //$NON-NLS-1$
	/** 
	 * Key used to look up background color preference.
	 * Value: <code>AbstractTextEditor.Color.Background</code>
	 * @since 2.0
	 */
	public final static String PREFERENCE_COLOR_BACKGROUND= "AbstractTextEditor.Color.Background"; //$NON-NLS-1$	
	/** 
	 * Key used to look up foreground color system default preference.
	 * Value: <code>AbstractTextEditor.Color.Foreground.SystemDefault</code>
	 * @since 2.0
	 */
	public final static String PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT= "AbstractTextEditor.Color.Foreground.SystemDefault"; //$NON-NLS-1$
	/** 
	 * Key used to look up background color system default preference.
	 * Value: <code>AbstractTextEditor.Color.Background.SystemDefault</code>
	 * @since 2.0
	 */
	public final static String PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT= "AbstractTextEditor.Color.Background.SystemDefault"; //$NON-NLS-1$	
	/** 
	 * Key used to look up find scope background color preference.
	 * Value: <code>AbstractTextEditor.Color.FindScope</code>
	 * @since 2.0
	 */
	public final static String PREFERENCE_COLOR_FIND_SCOPE= "AbstractTextEditor.Color.FindScope"; //$NON-NLS-1$	
	/** 
	 * Key used to look up smart home/end preference.
	 * Value: <code>AbstractTextEditor.Navigation.SmartHomeEnd</code>
	 * @since 2.1
	 */
	public final static String PREFERENCE_NAVIGATION_SMART_HOME_END= "AbstractTextEditor.Navigation.SmartHomeEnd"; //$NON-NLS-1$	

	
	/** Menu id for the editor context menu. */
	public final static String DEFAULT_EDITOR_CONTEXT_MENU_ID= "#EditorContext"; //$NON-NLS-1$
	/** Menu id for the ruler context menu. */
	public final static String DEFAULT_RULER_CONTEXT_MENU_ID= "#RulerContext"; //$NON-NLS-1$
	
	/** The width of the vertical ruler. */
	protected final static int VERTICAL_RULER_WIDTH= 12;
	
	/** 
	 * The complete mapping between action definition ids used by eclipse and StyledText actions.
	 * @since 2.0
	 */
	protected final static IdMapEntry[] ACTION_MAP= new IdMapEntry[] {
		// navigation
		new IdMapEntry(ITextEditorActionDefinitionIds.LINE_UP, ST.LINE_UP),
		new IdMapEntry(ITextEditorActionDefinitionIds.LINE_DOWN, ST.LINE_DOWN),
		new IdMapEntry(ITextEditorActionDefinitionIds.LINE_START, ST.LINE_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.LINE_END, ST.LINE_END),
		new IdMapEntry(ITextEditorActionDefinitionIds.COLUMN_PREVIOUS, ST.COLUMN_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.COLUMN_NEXT, ST.COLUMN_NEXT),
		new IdMapEntry(ITextEditorActionDefinitionIds.PAGE_UP, ST.PAGE_UP),
		new IdMapEntry(ITextEditorActionDefinitionIds.PAGE_DOWN, ST.PAGE_DOWN),
		new IdMapEntry(ITextEditorActionDefinitionIds.WORD_PREVIOUS, ST.WORD_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.WORD_NEXT, ST.WORD_NEXT),
		new IdMapEntry(ITextEditorActionDefinitionIds.TEXT_START, ST.TEXT_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.TEXT_END, ST.TEXT_END),
		new IdMapEntry(ITextEditorActionDefinitionIds.WINDOW_START, ST.WINDOW_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.WINDOW_END, ST.WINDOW_END),
		// selection
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_LINE_UP, ST.SELECT_LINE_UP),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_LINE_DOWN, ST.SELECT_LINE_DOWN),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_LINE_START, ST.SELECT_LINE_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_LINE_END, ST.SELECT_LINE_END),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_COLUMN_PREVIOUS, ST.SELECT_COLUMN_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_COLUMN_NEXT, ST.SELECT_COLUMN_NEXT),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_PAGE_UP, ST.SELECT_PAGE_UP),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_PAGE_DOWN, ST.SELECT_PAGE_DOWN),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, ST.SELECT_WORD_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT,  ST.SELECT_WORD_NEXT),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_TEXT_START, ST.SELECT_TEXT_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_TEXT_END, ST.SELECT_TEXT_END),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_WINDOW_START, ST.SELECT_WINDOW_START),
		new IdMapEntry(ITextEditorActionDefinitionIds.SELECT_WINDOW_END, ST.SELECT_WINDOW_END),
		// modification
		new IdMapEntry(ITextEditorActionDefinitionIds.CUT, ST.CUT),
		new IdMapEntry(ITextEditorActionDefinitionIds.COPY, ST.COPY),
		new IdMapEntry(ITextEditorActionDefinitionIds.PASTE, ST.PASTE),
		new IdMapEntry(ITextEditorActionDefinitionIds.DELETE_PREVIOUS, ST.DELETE_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.DELETE_NEXT, ST.DELETE_NEXT),
		new IdMapEntry(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, ST.DELETE_WORD_PREVIOUS),
		new IdMapEntry(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, ST.DELETE_WORD_NEXT),
		// miscellaneous
		new IdMapEntry(ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE, ST.TOGGLE_OVERWRITE)
	};	
	
	private final String fReadOnlyLabel = EditorMessages.getString("Editor.statusline.state.readonly.label"); //$NON-NLS-1$
	private final String fWritableLabel = EditorMessages.getString("Editor.statusline.state.writable.label"); //$NON-NLS-1$
	private final String fInsertModeLabel = EditorMessages.getString("Editor.statusline.mode.insert.label"); //$NON-NLS-1$
	private final String fOverwriteModeLabel = EditorMessages.getString("Editor.statusline.mode.overwrite.label"); //$NON-NLS-1$
	private final String fSmartInsertModeLabel= EditorMessages.getString("Editor.statusline.mode.smartinsert.label"); //$NON-NLS-1$
	
	/** The error message shown in the status line in case of failed information look up. */
	protected final String fErrorLabel= EditorMessages.getString("Editor.statusline.error.label"); //$NON-NLS-1$

	/**
	 * Data structure for the position label value.
	 */
	private static class PositionLabelValue {
		
		public int fValue;
		
		public String toString() {
			return String.valueOf(fValue);
		}
	};
	/** The pattern used to show the position label in the status line. */
	private final String fPositionLabelPattern= EditorMessages.getString("Editor.statusline.position.pattern"); //$NON-NLS-1$
	/** The position label value of the current line. */
	private final PositionLabelValue fLineLabel= new PositionLabelValue();
	/** The position label value of the current column. */
	private final PositionLabelValue fColumnLabel= new PositionLabelValue();
	/** The arguments for the position label pattern. */
	private final Object[] fPositionLabelPatternArguments= new Object[] { fLineLabel, fColumnLabel };

	
	
	
	
	/** The editor's explicit document provider. */
	private IDocumentProvider fExplicitDocumentProvider;
	/** The editor's preference store. */
	private IPreferenceStore fPreferenceStore;
	/** The editor's range indicator. */
	private Annotation fRangeIndicator;
	/** The editor's source viewer configuration. */
	private SourceViewerConfiguration fConfiguration;
	/** The editor's source viewer. */
	private ISourceViewer fSourceViewer;
	/**
	 * The editor's selection provider.
	 * @since 2.1
	 */
	private SelectionProvider fSelectionProvider= new SelectionProvider();
	/** The editor's font. */
	private Font fFont;	/** 
	 * The editor's foreground color.
	 * @since 2.0
	 */
	private Color fForegroundColor;
	/** 
	 * The editor's background color.
	 * @since 2.0
	 */
	private Color fBackgroundColor;
	/** 
	 * The find scope's highlight color.
	 * @since 2.0
	 */
	private Color fFindScopeHighlightColor;

	/**
	 * The editor's status line.
	 * @since 2.1
	 */
	private IEditorStatusLine fEditorStatusLine;
	/** The editor's vertical ruler. */
	private IVerticalRuler fVerticalRuler;
	/** The editor's context menu id. */
	private String fEditorContextMenuId;
	/** The ruler's context menu id. */
	private String fRulerContextMenuId;
	/** The editor's help context id. */
	private String fHelpContextId;
	/** The editor's presentation mode. */
	private boolean fShowHighlightRangeOnly;
	/** The actions registered with the editor. */	
	private Map fActions= new HashMap(10);
	/** The actions marked as selection dependent. */
	private List fSelectionActions= new ArrayList(5);
	/** The actions marked as content dependent. */
	private List fContentActions= new ArrayList(5);
	/** 
	 * The actions marked as property dependent.
	 * @since 2.0
	 */
	private List fPropertyActions= new ArrayList(5);
	/** 
	 * The actions marked as state dependent.
	 * @since 2.0
	 */
	private List fStateActions= new ArrayList(5);
	/** The editor's action activation codes. */
	private List fActivationCodes= new ArrayList(2);
	/** The verify key listener for activation code triggering. */
	private ActivationCodeTrigger fActivationCodeTrigger= new ActivationCodeTrigger();
	/** Context menu listener. */
	private IMenuListener fMenuListener;
	/** Vertical ruler mouse listener. */
	private MouseListener fMouseListener;
	/** Selection changed listener. */
	private ISelectionChangedListener fSelectionChangedListener;
	/** Title image to be disposed. */
	private Image fTitleImage;
	/** The text context menu to be disposed. */
	private Menu fTextContextMenu;
	/** The ruler context menu to be disposed. */
	private Menu fRulerContextMenu;
	/** The editor's element state listener. */
	private IElementStateListener fElementStateListener= new ElementStateListener();
	/**
	 * The editor's text input listener.
	 * @since 2.1
	 */
	private TextInputListener fTextInputListener= new TextInputListener();
	/** The editor's text listener. */
	private ITextListener fTextListener= new TextListener();
	/** The editor's property change listener. */
	private IPropertyChangeListener fPropertyChangeListener= new PropertyChangeListener();
	/**
	 * The editor's font properties change listener. 
	 * @since 2.1
	 */
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();
	
	/** 
	 * The editor's activation listener.
	 * @since 2.0
	 */
	private ActivationListener fActivationListener= new ActivationListener();
	/** 
	 * The map of the editor's status fields.
	 * @since 2.0
	 */
	private Map fStatusFields;
	/** 
	 * The editor's cursor listener.
	 * @since 2.0
	 */
	private ICursorListener fCursorListener;
	/** 
	 * The editor's remembered text selection.
	 * @since 2.0
	 */
	private ISelection fRememberedSelection;
	/** 
	 * Indicates whether the editor runs in 1.0 context menu registration compatibility mode.
	 * @since 2.0
	 */
	private boolean fCompatibilityMode= true;
	/** 
	 * The number of reentrances into error correction code while saving.
	 * @since 2.0
	 */
	private int fErrorCorrectionOnSave;
	/**
	 * The delete line target.
	 * @since 2.1
	 */
	private DeleteLineTarget fDeleteLineTarget;
	/** 
	 * The incremental find target.
	 * @since 2.0
	 */
	private IncrementalFindTarget fIncrementalFindTarget;
	/** 
	 * The mark region target.
	 * @since 2.0
	 */
	private IMarkRegionTarget fMarkRegionTarget;
	/** 
	 * Cached modification stamp of the editor's input.
	 * @since 2.0
	 */
	private long fModificationStamp= -1;
	/** 
	 * Ruler context menu listeners.
	 * @since 2.0
	 */	
	private List fRulerContextMenuListeners= new ArrayList();
	/** 
	 * Indicates whether sanity checking in enabled.
	 * @since 2.0
	 */
	private boolean fIsSanityCheckEnabled= true;
	/**
	 * The find replace target.
	 * @since 2.1
	 */
	private FindReplaceTarget fFindReplaceTarget;
	/**
	 * Indicates whether state validation is enabled.
	 * @since 2.1
	 */
	private boolean fIsStateValidationEnabled= true;
	/**
	 * The key binding scopes of this editor.
	 * @since 2.1
	 */
	private String[] fKeyBindingScopes;
	/** 
	 * The editor's insert mode.
	 * @since 3.0
	 */
	private InsertMode fInsertMode= SMART_INSERT;
	/**
	 * The sequence of legal editor insert modes.
	 * @since 3.0
	 */
	private List fLegalInsertModes= null;
	/**
	 * The caret used in overwrite mode.
	 * @since 3.0
	 */
	private Caret fOverwriteModeCaret;
	/**
	 * The caret used in insert mode.
	 * @since 3.0
	 */
	private Caret fInsertModeCaret;
	/**
	 * The caret used in smart insert mode.
	 * @since 3.0
	 */
	private Caret fSmartInsertModeCaret;
	
	
	/**
	 * Creates a new text editor. If not explicitly set, this editor uses
	 * a <code>SourceViewerConfiguration</code> to configure its
	 * source viewer. This viewer does not have a range indicator installed,
	 * nor any menu id set. By default, the created editor runs in 1.0 context
	 * menu registration compatibility mode.
	 */
	protected AbstractTextEditor() {
		super();
		fEditorContextMenuId= null;
		fRulerContextMenuId= null;
		fHelpContextId= null;
	}
	
	/*
	 * @see ITextEditor#getDocumentProvider()
	 */
	public IDocumentProvider getDocumentProvider() {
		return fExplicitDocumentProvider;
	}
		
	/** 
	 * Returns the editor's range indicator. 
	 *
	 * @return the editor's range indicator
	 */
	protected final Annotation getRangeIndicator() {
		return fRangeIndicator;
	}
	
	/** 
	 * Returns the editor's source viewer configuration.
	 *
	 * @return the editor's source viewer configuration
	 */
	protected final SourceViewerConfiguration getSourceViewerConfiguration() {
		return fConfiguration;
	}
	
	/** 
	 * Returns the editor's source viewer.
	 *
	 * @return the editor's source viewer
	 */
	protected final ISourceViewer getSourceViewer() {
		return fSourceViewer;
	}
	
	/** 
	 * Returns the editor's vertical ruler.
	 * 
	 * @return the editor's vertical ruler
	 */
	protected final IVerticalRuler getVerticalRuler() {
		return fVerticalRuler;
	}
	
	/** 
	 * Returns the editor's context menu id.
	 *
	 * @return the editor's context menu id
	 */
	protected final String getEditorContextMenuId() {
		return fEditorContextMenuId;
	}
	
	/** 
	 * Returns the ruler's context menu id.
	 *
	 * @return the ruler's context menu id
	 */
	protected final String getRulerContextMenuId() {
		return fRulerContextMenuId;
	}
	
	/** 
	 * Returns the editor's help context id.
	 *
	 * @return the editor's help context id
	 */
	protected final String getHelpContextId() {
		return fHelpContextId;
	}
	
	/**
	 * Returns this editor's preference store.
	 * 
	 * @return this editor's preference store
	 */
	protected final IPreferenceStore getPreferenceStore() {
		return fPreferenceStore;
	}
	
	/**
	 * Sets this editor's document provider. This method must be 
	 * called before the editor's control is created.
	 *
	 * @param provider the document provider
	 */
	protected void setDocumentProvider(IDocumentProvider provider) {
		Assert.isNotNull(provider);
		fExplicitDocumentProvider= provider;
	}
		
	/**
	 * Sets this editor's source viewer configuration used to configure its
	 * internal source viewer. This method must be called before the editor's
	 * control is created. If not, this editor uses a <code>SourceViewerConfiguration</code>.
	 *
	 * @param configuration the source viewer configuration object
	 */
	protected void setSourceViewerConfiguration(SourceViewerConfiguration configuration) {
		Assert.isNotNull(configuration);
		fConfiguration= configuration;
	}
	
	/**
	 * Sets the annotation which this editor uses to represent the highlight
	 * range if the editor is configured to show the entire document. If the
	 * range indicator is not set, this editor uses a <code>DefaultRangeIndicator</code>.
	 *
	 * @param rangeIndicator the annotation
	 */
	protected void setRangeIndicator(Annotation rangeIndicator) {
		Assert.isNotNull(rangeIndicator);
		fRangeIndicator= rangeIndicator;
	}
	
	/**
	 * Sets this editor's context menu id.
	 *
	 * @param contextMenuId the context menu id
	 */
	protected void setEditorContextMenuId(String contextMenuId) {
		Assert.isNotNull(contextMenuId);
		fEditorContextMenuId= contextMenuId;
	}
	
	/**
	 * Sets the ruler's context menu id.
	 *
	 * @param contextMenuId the context menu id
	 */
	protected void setRulerContextMenuId(String contextMenuId) {
		Assert.isNotNull(contextMenuId);
		fRulerContextMenuId= contextMenuId;
	}
	
	/**
	 * Sets the context menu registration 1.0 compatibility mode. (See class
	 * description for more details.)
	 * 
	 * @param compatible <code>true</code> if compatibility mode is enabled
	 * @since 2.0
	 */
	protected final void setCompatibilityMode(boolean compatible) {
		fCompatibilityMode= compatible;
	}
	
	/**
	 * Sets the editor's help context id.
	 *
	 * @param helpContextId the help context id
	 */
	protected void setHelpContextId(String helpContextId) {
		Assert.isNotNull(helpContextId);
		fHelpContextId= helpContextId;
	}
	
	/**
	 * Sets the keybinding scopes for this editor.
	 * 
	 * @param scopes the scopes 
	 * @since 2.1
	 */
	protected void setKeyBindingScopes(String[] scopes) {
		Assert.isTrue(scopes != null && scopes.length > 0);
		fKeyBindingScopes= scopes;
	}
	
	/**
	 * Sets this editor's preference store. This method must be
	 * called before the editor's control is created.
	 * 
	 * @param store the new preference store
	 */
	protected void setPreferenceStore(IPreferenceStore store) {
		if (fPreferenceStore != null)
			fPreferenceStore.removePropertyChangeListener(fPropertyChangeListener);
			
		fPreferenceStore= store;
		
		if (fPreferenceStore != null)
			fPreferenceStore.addPropertyChangeListener(fPropertyChangeListener);
	}
		
	/*
	 * @see ITextEditor#isEditable()
	 */
	public boolean isEditable() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension) {
			IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;
			return extension.isModifiable(getEditorInput());
		}
		return false;
	}
	
	/*
	 * @see ITextEditor#getSelectionProvider()
	 */
	public ISelectionProvider getSelectionProvider() {
		return fSelectionProvider;
	}
	
	/**
	 * Remembers the current selection of this editor. This method is called when, e.g., 
	 * the content of the editor is about to be reverted to the saved state. This method
	 * remembers the selection in a semantic format, i.e., in a format which allows to
	 * restore the selection even if the originally selected text is no longer part of the
	 * editor's content.
	 * <p>
	 * Subclasses should implement this method including all necessary state. This
	 * default implementation remembers the textual range only and is thus purely
	 * syntactic.</p>
	 * 
	 * @see #restoreSelection
	 * @since 2.0
	 */
	protected void rememberSelection() {
		fRememberedSelection= doGetSelection();
	}
	
	/**
	 * Returns the current selection.
	 * @return ISelection
	 * @since 2.1
	 */
	protected ISelection doGetSelection() {
		ISelectionProvider sp= null;
		if (fSourceViewer != null)
			sp= fSourceViewer.getSelectionProvider();
		return (sp == null ? null : sp.getSelection());
	}
	
	/**
	 * Restores a selection previously remembered by <code>rememberSelection</code>.
	 * Subclasses may reimplement this method and thereby semantically adapt the
	 * remembered selection. This default implementation just selects the
	 * remembered textual range. 
	 * 
	 * @see #rememberSelection
	 * @since 2.0
	 */
	protected void restoreSelection() {
		if (fRememberedSelection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection)fRememberedSelection;
			if (isValidSelection(textSelection.getOffset(), textSelection.getLength()))
				doSetSelection(fRememberedSelection);
		}
		fRememberedSelection= null;
	}
	
	/**
	 * Tells whether the given selection is valid.
	 * 
	 * @param offset the offset of the selection
	 * @param length the length of the selection
	 * @return <code>true</code> if the selection is valid
	 * @since 2.1
	 */
	private boolean isValidSelection(int offset, int length) {
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null) {
			IDocument document= provider.getDocument(getEditorInput());
			if (document != null) {
				int end= offset + length;
				int documentLength= document.getLength();
				return 0 <= offset  && offset <= documentLength && 0 <= end && end <= documentLength;
			}
		}
		return false;
	}
	
	/**
	 * Sets the given selection.
	 * @param selection
	 * @since 2.1
	 */
	protected void doSetSelection(ISelection selection) {
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection) selection;
			selectAndReveal(textSelection.getOffset(), textSelection.getLength());
		}
	}
	
	/**
	 * Creates and returns the listener on this editor's context menus.
	 *
	 * @return the menu listener
	 */
	protected final IMenuListener getContextMenuListener() {
		if (fMenuListener == null) {
			fMenuListener= new IMenuListener() {
				
				public void menuAboutToShow(IMenuManager menu) {
					String id= menu.getId();
					if (getRulerContextMenuId().equals(id)) {
						setFocus();
						rulerContextMenuAboutToShow(menu);
					} else if (getEditorContextMenuId().equals(id)) {
						setFocus();
						editorContextMenuAboutToShow(menu);
					}
				}
			};
		}
		return fMenuListener;
	}
	
	/**
	 * Creates and returns the listener on this editor's vertical ruler.
	 *
	 * @return the mouse listener
	 */
	protected final MouseListener getRulerMouseListener() {
		if (fMouseListener == null) {
			fMouseListener= new MouseListener() {
				
				private boolean fDoubleClicked= false;
				
				private void triggerAction(String actionID) {
					IAction action= getAction(actionID);
					if (action != null) {
						if (action instanceof IUpdate)
							((IUpdate) action).update();
						if (action.isEnabled())
							action.run();
					}
				}
				
				public void mouseUp(MouseEvent e) {
					setFocus();
					if (1 == e.button && !fDoubleClicked)
						triggerAction(ITextEditorActionConstants.RULER_CLICK);
					fDoubleClicked= false;
				}
				
				public void mouseDoubleClick(MouseEvent e) {
					if (1 == e.button) {
						fDoubleClicked= true;
						triggerAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK);
					}
				}
				
				public void mouseDown(MouseEvent e) {
					StyledText text= fSourceViewer.getTextWidget();
					if (text != null && !text.isDisposed()) {
							Display display= text.getDisplay();
							Point location= display.getCursorLocation();
							fRulerContextMenu.setLocation(location.x, location.y);
					}					
				}
			};
		}
		return fMouseListener;
	}

	/**
	 * Returns this editor's selection changed listener to be installed
	 * on the editor's source viewer.
	 *
	 * @return the listener
	 */
	protected final ISelectionChangedListener getSelectionChangedListener() {
		if (fSelectionChangedListener == null) {
			fSelectionChangedListener= new ISelectionChangedListener() {
				
				private Runnable fRunnable= new Runnable() {
					public void run() {
						// check whether editor has not been disposed yet
						if (fSourceViewer != null) {
							updateSelectionDependentActions();
						}
					}
				};
				
				private Display fDisplay;
				
				public void selectionChanged(SelectionChangedEvent event) {
					if (fDisplay == null)
						fDisplay= getSite().getShell().getDisplay();
					fDisplay.asyncExec(fRunnable);
					handleCursorPositionChanged();
				}
			};
		}
		
		return fSelectionChangedListener;
	}
	
	/**
	 * Returns this editor's "cursor" listener to be installed on the editor's
	 * source viewer. This listener is listening to key and mouse button events.
	 * It triggers the updating of the status line by calling
	 * <code>handleCursorPositionChanged()</code>.
	 * 
	 * @return the listener
	 * @since 2.0
	 */
	protected final ICursorListener getCursorListener() {
		if (fCursorListener == null) {
			fCursorListener= new ICursorListener() {
				
				public void keyPressed(KeyEvent e) {
					handleCursorPositionChanged();
				}
				
				public void keyReleased(KeyEvent e) {
				}
				
				public void mouseDoubleClick(MouseEvent e) {
				}
				
				public void mouseDown(MouseEvent e) {
				}
				
				public void mouseUp(MouseEvent e) {
					handleCursorPositionChanged();
				}
			};
		}
		return fCursorListener;
	}

	/*
	 * @see IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 * @since 2.1
	 */
	protected final void internalInit(IWorkbenchWindow window, final IEditorSite site, final IEditorInput input) throws PartInitException {
		
		final PartInitException[] exceptions= new PartInitException[1];
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					
					if (getDocumentProvider() instanceof IDocumentProviderExtension2) {
						IDocumentProviderExtension2 extension= (IDocumentProviderExtension2) getDocumentProvider();
						extension.setProgressMonitor(monitor);
					}
					
					doSetInput(input);
					
				} catch (CoreException x) {
					exceptions[0]= new PartInitException(x.getStatus());
				} finally {
					if (getDocumentProvider() instanceof IDocumentProviderExtension2) {
						IDocumentProviderExtension2 extension= (IDocumentProviderExtension2) getDocumentProvider();
						extension.setProgressMonitor(null);
					}
				}
			}
		};
					
		try {
			IRunnableContext context= (window instanceof IRunnableContext) ? (IRunnableContext) window : new ProgressMonitorDialog(window.getShell());
			context.run(false, true, runnable);
		} catch (InvocationTargetException x) {
		} catch (InterruptedException x) {
		}

		if (exceptions[0] != null)
			throw exceptions[0];
	}
	
	/*
	 * @see IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
		
		setSite(site);
		
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		internalInit(window, site, input);
		
		window.getPartService().addPartListener(fActivationListener);
		window.getShell().addShellListener(fActivationListener);
	}
	
	/**
	 * Creates the vertical ruler to be used by this editor.
	 * Subclasses may re-implement this method.
	 *
	 * @return the vertical ruler
	 */
	protected IVerticalRuler createVerticalRuler() {
		return new VerticalRuler(VERTICAL_RULER_WIDTH);
	}
	
	/**
	 * Creates the source viewer to be used by this editor.
	 * Subclasses may re-implement this method.
	 *
	 * @param parent the parent control
	 * @param ruler the vertical ruler
	 * @param styles style bits
	 * @return the source viewer
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		return new SourceViewer(parent, ruler, styles);
	}
	
	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>IWorkbenchPart</code> method creates the vertical ruler and
	 * source viewer.
	 * Subclasses may extend.
	 * 
	 * @param parent the parent composite
	 */
	public void createPartControl(Composite parent) {
		
		fVerticalRuler= createVerticalRuler();
		
		int styles= SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
		fSourceViewer= createSourceViewer(parent, fVerticalRuler, styles);
		
		if (fConfiguration == null)
			fConfiguration= new SourceViewerConfiguration();
		fSourceViewer.configure(fConfiguration);
		
		if (fRangeIndicator != null)
			fSourceViewer.setRangeIndicator(fRangeIndicator);
		
		fSourceViewer.addTextListener(fTextListener);
		getSelectionProvider().addSelectionChangedListener(getSelectionChangedListener());
				
		initializeViewerFont(fSourceViewer);
		initializeViewerColors(fSourceViewer);
		initializeFindScopeColor(fSourceViewer);
		
		StyledText styledText= fSourceViewer.getTextWidget();

		/* gestures commented out until proper solution (i.e. preference page) can be found
		 * for bug # 28417:
		 * 
		final Map gestureMap = new HashMap();
		
		gestureMap.put("E", "org.eclipse.ui.navigate.forwardHistory");
		gestureMap.put("N", "org.eclipse.ui.file.save");
		gestureMap.put("NW", "org.eclipse.ui.file.saveAll");
		gestureMap.put("S", "org.eclipse.ui.file.close");
		gestureMap.put("SW", "org.eclipse.ui.file.closeAll");
		gestureMap.put("W", "org.eclipse.ui.navigate.backwardHistory");
		gestureMap.put("EN", "org.eclipse.ui.edit.copy");
		gestureMap.put("ES", "org.eclipse.ui.edit.paste");
		gestureMap.put("EW", "org.eclipse.ui.edit.cut");

		Capture capture = Capture.create();
		capture.setControl(styledText);
		
		capture.addCaptureListener(new CaptureListener() { 
			public void gesture(Gesture gesture) {
				if (gesture.getPen() == 3) {
					String actionId = (String) gestureMap.get(Util.recognize(gesture.getPoints(), 20));
		
					if (actionId != null) {					
						IKeyBindingService keyBindingService = getEditorSite().getKeyBindingService();

						if (keyBindingService instanceof KeyBindingService) {
							IAction action = ((KeyBindingService) keyBindingService).getAction(actionId);
							
							if (action != null) {
								if (action instanceof IUpdate)
									((IUpdate) action).update();
								
								if (action.isEnabled())
									action.run();
							}
						}

						return;
					}

					fTextContextMenu.setVisible(true);
				}
			};
		});
		*/

		styledText.addMouseListener(getCursorListener());
		styledText.addKeyListener(getCursorListener());
		
		if (getHelpContextId() != null)
			WorkbenchHelp.setHelp(styledText, getHelpContextId());
			
		
		String id= fEditorContextMenuId != null ?  fEditorContextMenuId : DEFAULT_EDITOR_CONTEXT_MENU_ID;
		
		MenuManager manager= new MenuManager(id, id);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(getContextMenuListener());
		fTextContextMenu= manager.createContextMenu(styledText);
		
		// comment this line if using gestures, above.
		styledText.setMenu(fTextContextMenu);
		
		if (fEditorContextMenuId != null)
			getSite().registerContextMenu(fEditorContextMenuId, manager, getSelectionProvider());
		else if (fCompatibilityMode)
			getSite().registerContextMenu(DEFAULT_EDITOR_CONTEXT_MENU_ID, manager, getSelectionProvider());
			
		if ((fEditorContextMenuId != null && fCompatibilityMode) || fEditorContextMenuId  == null) {
			String partId= getSite().getId();
			if (partId != null)
				getSite().registerContextMenu(partId + ".EditorContext", manager, getSelectionProvider()); //$NON-NLS-1$
		}
		
		if (fEditorContextMenuId == null)
			fEditorContextMenuId= DEFAULT_EDITOR_CONTEXT_MENU_ID;
		
		
		id= fRulerContextMenuId != null ? fRulerContextMenuId : DEFAULT_RULER_CONTEXT_MENU_ID;
		manager= new MenuManager(id, id);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(getContextMenuListener());		
		
		Control rulerControl= fVerticalRuler.getControl();
		fRulerContextMenu= manager.createContextMenu(rulerControl);
		rulerControl.setMenu(fRulerContextMenu);
		rulerControl.addMouseListener(getRulerMouseListener());
		
		if (fRulerContextMenuId != null)
			getSite().registerContextMenu(fRulerContextMenuId, manager, getSelectionProvider());
		else if (fCompatibilityMode)
			getSite().registerContextMenu(DEFAULT_RULER_CONTEXT_MENU_ID, manager, getSelectionProvider());
			
		if ((fRulerContextMenuId != null && fCompatibilityMode) || fRulerContextMenuId  == null) {
			String partId= getSite().getId();
			if (partId != null)
				getSite().registerContextMenu(partId + ".RulerContext", manager, getSelectionProvider()); //$NON-NLS-1$
		}
		
		if (fRulerContextMenuId == null)
			fRulerContextMenuId= DEFAULT_RULER_CONTEXT_MENU_ID;
			
		getSite().setSelectionProvider(getSelectionProvider());
		
		initializeActivationCodeTrigger();
		
		createNavigationActions();
		createAccessibilityActions();
		createActions();
		
		initializeSourceViewer(getEditorInput());
		
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
	}
	
	/**
	 * @since 2.1
	 */
	private void initializeActivationCodeTrigger() {
		fActivationCodeTrigger.install();
		fActivationCodeTrigger.setScopes(fKeyBindingScopes);
	}
	
	/**
	 * Initializes the given viewer's font.
	 * 
	 * @param viewer the viewer
	 * @since 2.0
	 */
	private void initializeViewerFont(ISourceViewer viewer) {

		boolean isSharedFont= true;
		Font font= null;
		String symbolicFontName= getSymbolicFontName();

		if (symbolicFontName != null)
			font= JFaceResources.getFont(symbolicFontName);
		else if (fPreferenceStore != null) {
			// Backward compatibility
			if (fPreferenceStore.contains(JFaceResources.TEXT_FONT) && !fPreferenceStore.isDefault(JFaceResources.TEXT_FONT)) {
				FontData data= PreferenceConverter.getFontData(fPreferenceStore, JFaceResources.TEXT_FONT);
			
				if (data != null) {
					isSharedFont= false;
					font= new Font(viewer.getTextWidget().getDisplay(), data);
				}
			}
		}
		if (font == null)
			font= JFaceResources.getTextFont();

		setFont(viewer, font);
		
		if (fFont != null) {
			fFont.dispose();
			fFont= null;
		}

		if (!isSharedFont)
			fFont= font;
	}
	
	/**
	 * Sets the font for the given viewer sustaining selection and scroll position.
	 * 
	 * @param sourceViewer the source viewer
	 * @param font the font
	 * @since 2.0
	 */
	private void setFont(ISourceViewer sourceViewer, Font font) {
		if (sourceViewer.getDocument() != null) {
		
			Point selection= sourceViewer.getSelectedRange();
			int topIndex= sourceViewer.getTopIndex();
			
			StyledText styledText= sourceViewer.getTextWidget();
			Control parent= styledText;
			if (sourceViewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) sourceViewer;
				parent= extension.getControl();
			}
			
			parent.setRedraw(false);
			
			styledText.setFont(font);
			
			if (fVerticalRuler instanceof IVerticalRulerExtension) {
				IVerticalRulerExtension e= (IVerticalRulerExtension) fVerticalRuler;
				e.setFont(font);
			}
			
			sourceViewer.setSelectedRange(selection.x , selection.y);
			sourceViewer.setTopIndex(topIndex);
			
			if (parent instanceof Composite) {
				Composite composite= (Composite) parent;
				composite.layout(true);
			}
			
			parent.setRedraw(true);
			
			
		} else {
			
			StyledText styledText= sourceViewer.getTextWidget();
			styledText.setFont(font);
			
			if (fVerticalRuler instanceof IVerticalRulerExtension) {
				IVerticalRulerExtension e= (IVerticalRulerExtension) fVerticalRuler;
				e.setFont(font);
			}
		}	
	}
	
	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 * 
	 * @param store the store to read from
	 * @param key the key used for the lookup in the preference store
	 * @param display the display used create the color
	 * @return the created color according to the specification in the preference store
	 * @since 2.0
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
	
		RGB rgb= null;		
		
		if (store.contains(key)) {
			
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		
			if (rgb != null)
				return new Color(display, rgb);
		}
		
		return null;
	}
	
	/**
	 * Initializes the given viewer's colors.
	 * 
	 * @param viewer the viewer to be initialized
	 * @since 2.0
	 */
	private void initializeViewerColors(ISourceViewer viewer) {
		
		IPreferenceStore store= getPreferenceStore();
		if (store != null) {
			
			StyledText styledText= viewer.getTextWidget();
			
			// ----------- foreground color --------------------
			Color color= store.getBoolean(PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
				? null
				: createColor(store, PREFERENCE_COLOR_FOREGROUND, styledText.getDisplay());
			styledText.setForeground(color);
				
			if (fForegroundColor != null)
				fForegroundColor.dispose();
			
			fForegroundColor= color;
			
			// ---------- background color ----------------------
			color= store.getBoolean(PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
				? null
				: createColor(store, PREFERENCE_COLOR_BACKGROUND, styledText.getDisplay());
			styledText.setBackground(color);
				
			if (fBackgroundColor != null)
				fBackgroundColor.dispose();
				
			fBackgroundColor= color;
		}
	}

	/**
	 * Initializes the background color used for highlighting the document ranges
	 * defining search scopes.
	 * @param viewer the viewer to initialize
	 * @since 2.0
	 */
	private void initializeFindScopeColor(ISourceViewer viewer) {

		IPreferenceStore store= getPreferenceStore();
		if (store != null) {

			StyledText styledText= viewer.getTextWidget();
			
			Color color= createColor(store, PREFERENCE_COLOR_FIND_SCOPE, styledText.getDisplay());	

			IFindReplaceTarget target= viewer.getFindReplaceTarget();
			if (target != null && target instanceof IFindReplaceTargetExtension)
				((IFindReplaceTargetExtension) target).setScopeHighlightColor(color);
			
			if (fFindScopeHighlightColor != null)
				fFindScopeHighlightColor.dispose();

			fFindScopeHighlightColor= color;				
		}			
	}

		
	/**
	 * Initializes the editor's source viewer based on the given editor input.
	 *
	 * @param input the editor input to be used to initialize the source viewer
	 */
	private void initializeSourceViewer(IEditorInput input) {
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(input);
		IDocument document= getDocumentProvider().getDocument(input);
		
		if (document != null) {
			fSourceViewer.setDocument(document, model);
			fSourceViewer.setEditable(isEditable());
			fSourceViewer.showAnnotations(model != null);			
		}
		
		if (fElementStateListener instanceof IElementStateListenerExtension) {
			IElementStateListenerExtension extension= (IElementStateListenerExtension) fElementStateListener;
			extension.elementStateValidationChanged(input, false);
		}
		
		createInsertModeCarets();
		if (getInsertMode() == OVERWRITE)
			fSourceViewer.getTextWidget().invokeAction(ST.TOGGLE_OVERWRITE);
		handleInsertModeChanged();
	}
	
	/**
	 * Initializes the editor's title based on the given editor input.
	 *
	 * @param input the editor input to be used
	 */
	private void initializeTitle(IEditorInput input) {
		
		Image oldImage= fTitleImage;
		fTitleImage= null;
		String title= ""; //$NON-NLS-1$
		
		if (input != null) {
			IEditorRegistry editorRegistry = getEditorSite().getPage().getWorkbenchWindow().getWorkbench().getEditorRegistry();
			IEditorDescriptor editorDesc= editorRegistry.findEditor(getSite().getId());			
			ImageDescriptor imageDesc= editorDesc != null ? editorDesc.getImageDescriptor() : null;

			fTitleImage= imageDesc != null ? imageDesc.createImage() : null;
			title= input.getName();
		}
		
		setTitleImage(fTitleImage);
		setTitle(title);
		
		firePropertyChange(PROP_DIRTY);
		
		if (oldImage != null && !oldImage.isDisposed())
			oldImage.dispose();
	}
	
	/**
	 * Hook method for settingthe document provider for the given input.
	 * This default implementation does notthing. Clients may
	 * reimplement.
	 * 
	 * @param input the input of this editor.
	 * @since 3.0
	 */
	protected void setDocumentProvider(IEditorInput input) {
	}
		
	/**
	 * If there is no explicit document provider set, the implicit one is
	 * re-initialized based on the given editor input.
	 *
	 * @param input the editor input.
	 */
	private void updateDocumentProvider(IEditorInput input) {
		
		IProgressMonitor rememberedProgressMonitor= null;
		
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null) {
			provider.removeElementStateListener(fElementStateListener);
			if (provider instanceof IDocumentProviderExtension2) {
				IDocumentProviderExtension2 extension= (IDocumentProviderExtension2) provider;
				rememberedProgressMonitor= extension.getProgressMonitor();
				extension.setProgressMonitor(null);
			}
		}
		
		setDocumentProvider(input);
				
		provider= getDocumentProvider();	
		if (provider != null) {
			provider.addElementStateListener(fElementStateListener);
			if (provider instanceof IDocumentProviderExtension2) {
				IDocumentProviderExtension2 extension= (IDocumentProviderExtension2) provider;
				extension.setProgressMonitor(rememberedProgressMonitor);
			}
		}
	}
	
	/**
	 * Internal <code>setInput</code> method.
	 *
	 * @param input the input to be set
	 * @exception CoreException if input cannot be connected to the document provider
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		
		if (input == null)
			
			close(isSaveOnCloseNeeded());
		
		else {
			
			IEditorInput oldInput= getEditorInput();
			if (oldInput != null)
				getDocumentProvider().disconnect(oldInput);
			
			super.setInput(input);
						
			updateDocumentProvider(input);
			
			IDocumentProvider provider= getDocumentProvider();
			if (provider == null) {
				IStatus s= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, EditorMessages.getString("Editor.error.no_provider"), null); //$NON-NLS-1$
				throw new CoreException(s);
			}
			
			provider.connect(input);
			
			initializeTitle(input);
			if (fSourceViewer != null)
				initializeSourceViewer(input);
				
			updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE);
		}
	}
	
	/*
	 * @see EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	public final void setInput(IEditorInput input) {
		
		try {
			
			doSetInput(input);
				
		} catch (CoreException x) {
			String title= EditorMessages.getString("Editor.error.setinput.title"); //$NON-NLS-1$
			String msg= EditorMessages.getString("Editor.error.setinput.message"); //$NON-NLS-1$
			Shell shell= getSite().getShell();
			ErrorDialog.openError(shell, title, msg, x.getStatus());
		}				
	}
	
	/*
	 * @see ITextEditor#close
	 */
	public void close(final boolean save) {
		
		enableSanityChecking(false);
		
		Display display= getSite().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				if (fSourceViewer != null) {
					// check whether editor has not been disposed yet
					getSite().getPage().closeEditor(AbstractTextEditor.this, save);
				}
			}
		});
	}
	
	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>IWorkbenchPart</code> method may be extended by subclasses.
	 * Subclasses must call <code>super.dispose()</code>.
	 */
	public void dispose() {
		
		if (fActivationListener != null) {
			IWorkbenchWindow window= getSite().getWorkbenchWindow();
			window.getPartService().removePartListener(fActivationListener);
			Shell shell= window.getShell();
			if (shell != null && !shell.isDisposed())
				shell.removeShellListener(fActivationListener);
			fActivationListener= null;
		}
		
		if (fTitleImage != null) {
			fTitleImage.dispose();
			fTitleImage= null;
		}

		if (fFont != null) {
			fFont.dispose();
			fFont= null;
		}
		
		if (fInsertModeCaret != null) {
			if (!fInsertModeCaret.isDisposed()) {
				// TODO remove parent check 
				Canvas parent= fInsertModeCaret.getParent();
				if (parent != null && ! parent.isDisposed())
					fInsertModeCaret.dispose();
			}
			fInsertModeCaret= null;
		}
		
		if (fOverwriteModeCaret != null) {
			if (!fOverwriteModeCaret.isDisposed()) {
				// TODO remove parent check 
				Canvas parent= fOverwriteModeCaret.getParent();
				if (parent != null && ! parent.isDisposed())
					fOverwriteModeCaret.dispose();
			}
			fOverwriteModeCaret= null;
		}
		
		if (fSmartInsertModeCaret != null) {
			if (!fSmartInsertModeCaret.isDisposed()) {
				// TODO remove parent check 
				Canvas parent= fSmartInsertModeCaret.getParent();
				if (parent != null && ! parent.isDisposed())
					fSmartInsertModeCaret.dispose();
			}
			fSmartInsertModeCaret= null;
		}
		
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
			fForegroundColor= null;
		}
		
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
			fBackgroundColor= null;
		}
		
		if (fFindScopeHighlightColor != null) {
			fFindScopeHighlightColor.dispose();
			fFindScopeHighlightColor= null;
		}

		if (fFontPropertyChangeListener != null) {
			JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
			fFontPropertyChangeListener= null;
		}
		
		if (fPropertyChangeListener != null) {
			if (fPreferenceStore != null) {
				fPreferenceStore.removePropertyChangeListener(fPropertyChangeListener);
				fPreferenceStore= null;
			}
			fPropertyChangeListener= null;
		}
		
		if (fActivationCodeTrigger != null) {
			fActivationCodeTrigger.uninstall();
			fActivationCodeTrigger= null;
		}
		
		disposeDocumentProvider();
		
		if (fSourceViewer != null) {
			
			if (fTextListener != null) {
				fSourceViewer.removeTextListener(fTextListener);
				fTextListener= null;
			}
			
			fTextInputListener= null;			
			fSelectionProvider= null;
			fSourceViewer= null;
		}
		
		if (fTextContextMenu != null) {
			fTextContextMenu.dispose();
			fTextContextMenu= null;
		}
		
		if (fRulerContextMenu != null) {
			fRulerContextMenu.dispose();
			fRulerContextMenu= null;
		}
		
		if (fActions != null) {
			fActions.clear();
			fActions= null;
		}
		
		if (fSelectionActions != null) {
			fSelectionActions.clear();
			fSelectionActions= null;
		}
		
		if (fContentActions != null) {
			fContentActions.clear();
			fContentActions= null;
		}
		
		if (fPropertyActions != null) {
			fPropertyActions.clear();
			fPropertyActions= null;
		}
		
		if (fStateActions != null) {
			fStateActions.clear();
			fStateActions= null;
		}
		
		if (fActivationCodes != null) {
			fActivationCodes.clear();
			fActivationCodes= null;
		}
		
		if (fEditorStatusLine != null)
			fEditorStatusLine= null;
		
		super.setInput(null);		
		
		super.dispose();
	}
	
	/**
	 * Disposes the connection with the document provider. Subclasses
	 * may extend.
	 * 
	 * @since 3.0
	 */
	protected void disposeDocumentProvider() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null) {
			
			IEditorInput input= getEditorInput();
			if (input != null)
				provider.disconnect(input);
			
			if (fElementStateListener != null) {
				provider.removeElementStateListener(fElementStateListener);
				fElementStateListener= null;
			}
			
			fExplicitDocumentProvider= null;
		}
	}

	/**
	 * Determines whether the given preference change affects the editor's
	 * presentation. This implementation always returns <code>false</code>.
	 * May be reimplemented by subclasses.
	 * 
	 * @param event the event which should be investigated
	 * @return <code>true</code> if the event describes a preference change affecting the editor's presentation
	 * @since 2.0
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return false;
	}

	/**
	 * Returns the symbolic font name for this
	 * editor as defined in XML.
	 * 
	 * @return a String with the symbolic font name or <code>null</code> if none is defined
	 * @since 2.1
	 */
	private String getSymbolicFontName() {
		if (getConfigurationElement() != null)
			return getConfigurationElement().getAttribute("symbolicFontName"); //$NON-NLS-1$
		else
			return null;
	}

	/**
	 * Returns the property preference key for the editor font.
	 * 
	 * @return a String with the key
	 * @since 2.1
	 */
	protected final String getFontPropertyPreferenceKey() {
		String symbolicFontName= getSymbolicFontName();

		if (symbolicFontName != null)
			return symbolicFontName;
		else
		 	return JFaceResources.TEXT_FONT;
	}
	
	/**
	 * Handles a property change event describing a change
	 * of the editor's preference store and updates the preference
	 * related editor properties.
	 * 
	 * @param event the property change event
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		if (fSourceViewer == null)
			return;
			
		String property= event.getProperty();
		
		if (getFontPropertyPreferenceKey().equals(property)) {
			// There is a separate handler for font preference changes
			return;
		} else if (PREFERENCE_COLOR_FOREGROUND.equals(property) || PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(property) ||
			PREFERENCE_COLOR_BACKGROUND.equals(property) ||	PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property))
		{
			initializeViewerColors(fSourceViewer);
		} else if (PREFERENCE_COLOR_FIND_SCOPE.equals(property)) {
			initializeFindScopeColor(fSourceViewer);
		}
			
		if (affectsTextPresentation(event))
			fSourceViewer.invalidateTextPresentation();
	}
	
	/**
	 * Returns the progress monitor related to this editor.
	 * 
	 * @return the progress monitor related to this editor
	 * @since 2.1
	 */
	protected IProgressMonitor getProgressMonitor() {
		
		IProgressMonitor pm= null;
		
		IStatusLineManager manager= getStatusLineManager();
		if (manager != null)
			pm= manager.getProgressMonitor();
			
		return pm != null ? pm : new NullProgressMonitor();
	}
	
		/**
		 * Handles an external change of the editor's input element.
		 */
		protected void handleEditorInputChanged() {
			
			String title;
			String msg;
			Shell shell= getSite().getShell();
			
			final IDocumentProvider provider= getDocumentProvider();
			if (provider == null) {
				// fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=15066
				close(false);
				return;
			}
			
			final IEditorInput input= getEditorInput();
			if (provider.isDeleted(input)) {
				
				if (isSaveAsAllowed()) {
				
					title= EditorMessages.getString("Editor.error.activated.deleted.save.title"); //$NON-NLS-1$
					msg= EditorMessages.getString("Editor.error.activated.deleted.save.message"); //$NON-NLS-1$
					
					String[] buttons= {
						EditorMessages.getString("Editor.error.activated.deleted.save.button.save"), //$NON-NLS-1$
						EditorMessages.getString("Editor.error.activated.deleted.save.button.close"), //$NON-NLS-1$
					};
						
					MessageDialog dialog= new MessageDialog(shell, title, null, msg, MessageDialog.QUESTION, buttons, 0);
					
					if (dialog.open() == 0) {
						IProgressMonitor pm= getProgressMonitor();
						performSaveAs(pm);
						if (pm.isCanceled())
							handleEditorInputChanged();
					} else {
						close(false);
					}
					
				} else {
					
					title= EditorMessages.getString("Editor.error.activated.deleted.close.title"); //$NON-NLS-1$
					msg= EditorMessages.getString("Editor.error.activated.deleted.close.message"); //$NON-NLS-1$
					if (MessageDialog.openConfirm(shell, title, msg))
						close(false);
				}
				
			} else {
				
				title= EditorMessages.getString("Editor.error.activated.outofsync.title"); //$NON-NLS-1$
				msg= EditorMessages.getString("Editor.error.activated.outofsync.message"); //$NON-NLS-1$
				
				if (MessageDialog.openQuestion(shell, title, msg)) {
					
					
					try {
						if (provider instanceof IDocumentProviderExtension) {
							IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;
							extension.synchronize(input);
						} else {
							doSetInput(input);
						} 
					} catch (CoreException x) {
						title= EditorMessages.getString("Editor.error.refresh.outofsync.title"); //$NON-NLS-1$
						msg= EditorMessages.getString("Editor.error.refresh.outofsync.message"); //$NON-NLS-1$
						ErrorDialog.openError(shell, title, msg, x.getStatus());
					}
				}
			}
		}

	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>IEditorPart</code> method calls <code>performSaveAs</code>. 
	 * Subclasses may reimplement.
	 */
	public void doSaveAs() {
		/*
		 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors.
		 * Changed Behavior to make sure that if called inside a regular save (because
		 * of deletion of input element) there is a way to report back to the caller.
		 */
		performSaveAs(getProgressMonitor());
	}
	
	/**
	 * Performs a save as and reports the result state back to the 
	 * given progress monitor. This default implementation does nothing.
	 * Subclasses may reimplement.
	 * 
	 * @param progressMonitor the progress monitor for communicating result state or <code>null</code>
	 */
	protected void performSaveAs(IProgressMonitor progressMonitor) {
	}
		
	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>IEditorPart</code> method may be extended by subclasses.
	 * 
	 * @param progressMonitor the progress monitor for communicating result state or <code>null</code>
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		IDocumentProvider p= getDocumentProvider();
		if (p == null)
			return;
			
		if (p.isDeleted(getEditorInput())) {
			
			if (isSaveAsAllowed()) {
				
				/*
				 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors.
				 * Changed Behavior to make sure that if called inside a regular save (because
				 * of deletion of input element) there is a way to report back to the caller.
				 */
				performSaveAs(progressMonitor);
			
			} else {
				
				Shell shell= getSite().getShell();
				String title= EditorMessages.getString("Editor.error.save.deleted.title"); //$NON-NLS-1$
				String msg= EditorMessages.getString("Editor.error.save.deleted.message"); //$NON-NLS-1$
				MessageDialog.openError(shell, title, msg);
			}
			
		} else {	
			performSave(false, progressMonitor);
		}
	}
	
	/**
	 * Enables/disables sanity checking.
	 * @param enable <code>true</code> if santity checking should be enabled, <code>false</code> otherwise
	 * @since 2.0
	 */
	protected void enableSanityChecking(boolean enable) {
		synchronized (this) {
			fIsSanityCheckEnabled= enable;
		}
	}
	
	/**
	 * Checks the state of the given editor input if sanity checking is enabled.
	 * @param input the editor input whose state is to be checked
	 * @since 2.0
	 */
	protected void safelySanityCheckState(IEditorInput input) {
		boolean enabled= false;
		
		synchronized (this) {
			enabled= fIsSanityCheckEnabled;
		}
		
		if (enabled)
			sanityCheckState(input);
	}
	
	/**
	 * Checks the state of the given editor input.
	 * @param input the editor input whose state is to be checked
	 * @since 2.0
	 */
	protected void sanityCheckState(IEditorInput input) {
		
		IDocumentProvider p= getDocumentProvider();
		if (p == null)
			return;
		
		if (fModificationStamp == -1) 
			fModificationStamp= p.getSynchronizationStamp(input);
			
		long stamp= p.getModificationStamp(input);
		if (stamp != fModificationStamp) {
			fModificationStamp= stamp;
			if (stamp != p.getSynchronizationStamp(input))
				handleEditorInputChanged();
		} 
		
		updateState(getEditorInput());
		updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE);
	}
	
	/**
	 * Enables/disables state validation.
	 * @param enable <code>true</code> if state validation should be enabled, <code>false</code> otherwise
	 * @since 2.1
	 */
	protected void enableStateValidation(boolean enable) {
		synchronized (this) {
			fIsStateValidationEnabled= enable;
		}
	}
	
	/**
	 * Validates the state of the given editor input. The predominate intent
	 * of this method is to take any action propably necessary to ensure that
	 * the input can persistently be changed.
	 * 
	 * @param input the input to be validated
	 * @since 2.0
	 */
	protected void validateState(IEditorInput input) {		
		
		IDocumentProvider provider= getDocumentProvider();
		if (! (provider instanceof IDocumentProviderExtension))
			return;
			
		IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;	
				
		try {
			extension.validateState(input, getSite().getShell());	
		} catch (CoreException exception) {
		
			ILog log= Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog();		
			log.log(exception.getStatus());

			Shell shell= getSite().getShell();
			String title= EditorMessages.getString("Editor.error.validateEdit.title"); //$NON-NLS-1$
			String msg= EditorMessages.getString("Editor.error.validateEdit.message"); //$NON-NLS-1$			
			ErrorDialog.openError(shell, title, msg, exception.getStatus());

			return;
		}

		if (fSourceViewer != null)
			fSourceViewer.setEditable(isEditable());

		updateStateDependentActions();
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension2#validateEditorInputState()
	 * @since 2.1
	 */
	public boolean validateEditorInputState() {
		
		boolean enabled= false;
		
		synchronized (this) {
			enabled= fIsStateValidationEnabled;
		}
		
		if (enabled) {
			
			ISourceViewer viewer= getSourceViewer();
			fTextInputListener.inputChanged= false;
			viewer.addTextInputListener(fTextInputListener);
			try {			
				IEditorInput input= getEditorInput();
				validateState(input);
				sanityCheckState(input);
				return !isEditorInputReadOnly() && !fTextInputListener.inputChanged;
	
			} finally {
				viewer.removeTextInputListener(fTextInputListener);
			}
			
		}
		
		return !isEditorInputReadOnly();
	}
	
	/**
	 * Updates the state of the given editor input such as read-only flag.
	 * 
	 * @param input the input to be validated
	 * @since 2.0
	 */
	protected void updateState(IEditorInput input) {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension) {
			IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;
			try {
				
				boolean wasReadOnly= isEditorInputReadOnly();
				extension.updateStateCache(input);
				
				if (fSourceViewer != null)
					fSourceViewer.setEditable(isEditable());
				
				if (wasReadOnly != isEditorInputReadOnly())
					updateStateDependentActions();
				
			} catch (CoreException x) {
				ILog log= Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog();		
				log.log(x.getStatus());
			}
		}
	}
	
	/**
	 * Performs the save and handles errors appropriatly.
	 * 
	 * @param overwrite indicates whether or not overwrititng is allowed
	 * @param progressMonitor the monitor in which to run the operation
	 */
	protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
		
		IDocumentProvider provider= getDocumentProvider();
		if (provider == null)
			return;
		
		try {
		
			provider.aboutToChange(getEditorInput());
			IEditorInput input= getEditorInput();
			provider.saveDocument(progressMonitor, input, getDocumentProvider().getDocument(input), overwrite);
			editorSaved();
		
		} catch (CoreException x) {
			handleExceptionOnSave(x, progressMonitor);
		} finally {
			provider.changed(getEditorInput());
		}
	}
	
	/**
	 * Handles the given exception. If the exception reports an out-of-sync
	 * situation, this is reported to the user. Otherwise, the exception
	 * is generically reported.
	 * 
	 * @param exception the exception to handle
	 * @param progressMonitor the progress monitor
	 */
	protected void handleExceptionOnSave(CoreException exception, IProgressMonitor progressMonitor) {
		
		try {
			++ fErrorCorrectionOnSave;
			
			Shell shell= getSite().getShell();
			
			IDocumentProvider p= getDocumentProvider();
			long modifiedStamp= p.getModificationStamp(getEditorInput());
			long synchStamp= p.getSynchronizationStamp(getEditorInput());
			
			if (fErrorCorrectionOnSave == 1 && modifiedStamp != synchStamp) {
				
				String title= EditorMessages.getString("Editor.error.save.outofsync.title"); //$NON-NLS-1$
				String msg= EditorMessages.getString("Editor.error.save.outofsync.message"); //$NON-NLS-1$
				
				if (MessageDialog.openQuestion(shell, title, msg))
					performSave(true, progressMonitor);
				else {
					/*
					 * 1GEUPKR: ITPJUI:ALL - Loosing work with simultaneous edits
					 * Set progress monitor to canceled in order to report back 
					 * to enclosing operations. 
					 */
					if (progressMonitor != null)
						progressMonitor.setCanceled(true);
				}
			} else {
				
				String title= EditorMessages.getString("Editor.error.save.title"); //$NON-NLS-1$
				String msg= EditorMessages.getString("Editor.error.save.message"); //$NON-NLS-1$
				ErrorDialog.openError(shell, title, msg, exception.getStatus());
				
				/*
				 * 1GEUPKR: ITPJUI:ALL - Loosing work with simultaneous edits
				 * Set progress monitor to canceled in order to report back 
				 * to enclosing operations. 
				 */
				if (progressMonitor != null)
					progressMonitor.setCanceled(true);
			}
			
		} finally {
			-- fErrorCorrectionOnSave;
		}
	}
	
	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>IEditorPart</code> method returns <code>false</code>.
	 * Subclasses may override.
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/*
	 * @see EditorPart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		IDocumentProvider p= getDocumentProvider();
		return p == null ? false : p.mustSaveDocument(getEditorInput());
	}
	
	/*
	 * @see EditorPart#isDirty()
	 */
	public boolean isDirty() {
		IDocumentProvider p= getDocumentProvider();
		return p == null ? false : p.canSaveDocument(getEditorInput());
	}
	
	/**
	 * The <code>AbstractTextEditor</code> implementation of this 
	 * <code>ITextEditor</code> method may be extended by subclasses.
	 */
	public void doRevertToSaved() {
		IDocumentProvider p= getDocumentProvider();
		if (p == null)
			return;
			
		performRevert();
	}
	
	/**
	 * Performs revert and handles errors appropriatly.
	 * 
	 * @since 3.0
	 */
	protected void performRevert() {
		
		IDocumentProvider provider= getDocumentProvider();
		if (provider == null)
			return;
			
		try {
			
			provider.aboutToChange(getEditorInput());
			provider.resetDocument(getEditorInput());
			editorSaved();
			
		} catch (CoreException x) {
			Shell shell= getSite().getShell();
			String title= EditorMessages.getString("Editor.error.revert.title"); //$NON-NLS-1$
			String msg= EditorMessages.getString("Editor.error.revert.message"); //$NON-NLS-1$
			ErrorDialog.openError(shell, title, msg, x.getStatus());
		} finally {
			provider.changed(getEditorInput());
		}
	}
	
	/*
	 * @see ITextEditor#setAction(String, IAction)
	 */
	public void setAction(String actionID, IAction action) {
		Assert.isNotNull(actionID);
		if (action == null) {
			action= (IAction) fActions.remove(actionID);
			if (action != null)
				fActivationCodeTrigger.unregisterActionFromKeyActivation(action);
		} else {
			fActions.put(actionID, action);
			fActivationCodeTrigger.registerActionForKeyActivation(action);
		}
	}
	
	/*
	 * @see ITextEditor#setActionActivationCode(String, char, int, int)
	 */
	public void setActionActivationCode(String actionID, char activationCharacter, int activationKeyCode, int activationStateMask) {
		
		Assert.isNotNull(actionID);
		
		ActionActivationCode found= findActionActivationCode(actionID);
		if (found == null) {
			found= new ActionActivationCode(actionID);
			fActivationCodes.add(found);
		}
		
		found.fCharacter= activationCharacter;
		found.fKeyCode= activationKeyCode;
		found.fStateMask= activationStateMask;
	}
	
	/**
	 * Returns the activation code registered for the specified action.
	 * 
	 * @param actionID the action id
	 * @return the registered activation code or <code>null</code> if no code has been installed
	 */
	private ActionActivationCode findActionActivationCode(String actionID) {
		int size= fActivationCodes.size();
		for (int i= 0; i < size; i++) {
			ActionActivationCode code= (ActionActivationCode) fActivationCodes.get(i);
			if (actionID.equals(code.fActionId))
				return code;
		}
		return null;
	}
	
	/*
	 * @see ITextEditor#removeActionActivationCode(String)
	 */
	public void removeActionActivationCode(String actionID) {
		Assert.isNotNull(actionID);
		ActionActivationCode code= findActionActivationCode(actionID);
		if (code != null)
			fActivationCodes.remove(code);
	}
	
	/*
	 * @see ITextEditor#getAction(String)
	 */
	public IAction getAction(String actionID) {
		Assert.isNotNull(actionID);
		IAction action= (IAction) fActions.get(actionID);
		
		if (action == null) {
			action= findContributedAction(actionID);
			if (action != null)
				setAction(actionID, action);
		}
		
		return action;
	}
	
	/**
	 * Returns the action with the given action id that has been contributed via xml to this editor.
	 * The lookup honors the dependencies of plug-ins.
	 * 
	 * @param actionID the action id to look up
	 * @return the action that has been contributed
	 * @since 2.0
	 */
	private IAction findContributedAction(String actionID) {
		IExtensionPoint extensionPoint= Platform.getPluginRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID, "editorActions"); //$NON-NLS-1$
		if (extensionPoint != null) {
			IConfigurationElement[] elements= extensionPoint.getConfigurationElements();			

			List actions= new ArrayList();
			for (int i= 0; i < elements.length; i++) {
				IConfigurationElement element= elements[i];				
				if (TAG_CONTRIBUTION_TYPE.equals(element.getName())) {
					if (!getSite().getId().equals(element.getAttribute("targetID"))) //$NON-NLS-1$
						continue;

					IConfigurationElement[] children= element.getChildren("action"); //$NON-NLS-1$
					for (int j= 0; j < children.length; j++) {
						IConfigurationElement child= children[j];
						if (actionID.equals(child.getAttribute("actionID"))) //$NON-NLS-1$
							actions.add(child);
					}
				}
			}
			Collections.sort(actions, new ConfigurationElementComparator());

			if (actions.size() != 0) {
				IConfigurationElement element= (IConfigurationElement) actions.get(0);
				String defId = element.getAttribute(ActionDescriptor.ATT_DEFINITION_ID);
				return new EditorPluginAction(element, "class", this, defId, IAction.AS_UNSPECIFIED); //$NON-NLS-1$			
			}
		}
		
		return null;
	}
	
	/**
	 * Updates the specified action by calling <code>IUpdate.update</code>
	 * if applicable.
	 *
	 * @param actionId the action id
	 */
	private void updateAction(String actionId) {
		Assert.isNotNull(actionId);
		if (fActions != null) {
			IAction action= (IAction) fActions.get(actionId);
			if (action instanceof IUpdate)
				((IUpdate) action).update();
		}
	}
	
	/**
	 * Marks or unmarks the given action to be updated on text selection changes.
	 *
	 * @param actionId the action id
	 * @param mark <code>true</code> if the action is selection dependent
	 */
	public void markAsSelectionDependentAction(String actionId, boolean mark) {
		Assert.isNotNull(actionId);
		if (mark) {
			if (!fSelectionActions.contains(actionId))
				fSelectionActions.add(actionId);
		} else
			fSelectionActions.remove(actionId);
	}
		
	/**
	 * Marks or unmarks the given action to be updated on content changes.
	 *
	 * @param actionId the action id
	 * @param mark <code>true</code> if the action is content dependent
	 */
	public void markAsContentDependentAction(String actionId, boolean mark) {
		Assert.isNotNull(actionId);
		if (mark) {
			if (!fContentActions.contains(actionId))
				fContentActions.add(actionId);
		} else
			fContentActions.remove(actionId);
	}
	
	/**
	 * Marks or unmarks the given action to be updated on property changes.
	 *
	 * @param actionId the action id
	 * @param mark <code>true</code> if the action is property dependent
	 * @since 2.0
	 */
	public void markAsPropertyDependentAction(String actionId, boolean mark) {
		Assert.isNotNull(actionId);
		if (mark) {
			if (!fPropertyActions.contains(actionId))
				fPropertyActions.add(actionId);
		} else
			fPropertyActions.remove(actionId);
	}
	
	/**
	 * Marks or unmarks the given action to be updated on state changes.
	 *
	 * @param actionId the action id
	 * @param mark <code>true</code> if the action is state dependent
	 * @since 2.0
	 */
	public void markAsStateDependentAction(String actionId, boolean mark) {
		Assert.isNotNull(actionId);
		if (mark) {
			if (!fStateActions.contains(actionId))
				fStateActions.add(actionId);
		} else
			fStateActions.remove(actionId);
	}
	
	/**
	 * Updates all selection dependent actions.
	 */
	protected void updateSelectionDependentActions() {
		if (fSelectionActions != null) {
			Iterator e= fSelectionActions.iterator();
			while (e.hasNext())
				updateAction((String) e.next());
		}
	}
	
	/**
	 * Updates all content dependent actions.
	 */
	protected void updateContentDependentActions() {
		if (fContentActions != null) {
			Iterator e= fContentActions.iterator();
			while (e.hasNext())
				updateAction((String) e.next());
		}
	}
	
	/**
	 * Updates all property dependent actions.
	 * @since 2.0
	 */
	protected void updatePropertyDependentActions() {
		if (fPropertyActions != null) {
			Iterator e= fPropertyActions.iterator();
			while (e.hasNext())
				updateAction((String) e.next());
		}
	}
	
	/**
	 * Updates all state dependent actions.
	 * @since 2.0
	 */
	protected void updateStateDependentActions() {
		if (fStateActions != null) {
			Iterator e= fStateActions.iterator();
			while (e.hasNext())
				updateAction((String) e.next());
		}
	}
	
	/**
	 * Creates action entries for all SWT StyledText actions as defined in
	 * <code>org.eclipse.swt.custom.ST</code>. Overwrites and 
	 * extends the list of these actions afterwards.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 * @since 2.0
	 */
	protected void createNavigationActions() {
		
		IAction action;
		
		StyledText textWidget= getSourceViewer().getTextWidget();
		for (int i= 0; i < ACTION_MAP.length; i++) {
			IdMapEntry entry= (IdMapEntry) ACTION_MAP[i];
			action= new TextNavigationAction(textWidget, entry.getAction());
			action.setActionDefinitionId(entry.getActionId());
			setAction(entry.getActionId(), action);
		}
		
		action= new ToggleInsertModeAction(textWidget);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE);
		setAction(ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE, action);
		textWidget.setKeyBinding(SWT.INSERT, SWT.NULL);
		
		action=  new ScrollLinesAction(-1);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SCROLL_LINE_UP);
		setAction(ITextEditorActionDefinitionIds.SCROLL_LINE_UP, action);
		
		action= new ScrollLinesAction(1);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SCROLL_LINE_DOWN);
		setAction(ITextEditorActionDefinitionIds.SCROLL_LINE_DOWN, action);

		action= new LineEndAction(textWidget, false);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_END);
		setAction(ITextEditorActionDefinitionIds.LINE_END, action);

		action= new LineStartAction(textWidget, false);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_START);
		setAction(ITextEditorActionDefinitionIds.LINE_START, action);

		action= new LineEndAction(textWidget, true);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_END);
		setAction(ITextEditorActionDefinitionIds.SELECT_LINE_END, action);

		action= new LineStartAction(textWidget, true);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_START);
		setAction(ITextEditorActionDefinitionIds.SELECT_LINE_START, action);
		
		setActionActivationCode(ITextEditorActionDefinitionIds.LINE_END, (char) 0, SWT.END, SWT.NONE);
		setActionActivationCode(ITextEditorActionDefinitionIds.LINE_START, (char) 0, SWT.HOME, SWT.NONE);
		setActionActivationCode(ITextEditorActionDefinitionIds.SELECT_LINE_END, (char) 0, SWT.END, SWT.SHIFT);
		setActionActivationCode(ITextEditorActionDefinitionIds.SELECT_LINE_START, (char) 0, SWT.HOME, SWT.SHIFT);
	}

	/**
	 * Creates this editor's accessibility actions.
	 * @since 2.0
	 */
	private void createAccessibilityActions() {
		IAction action= new ShowRulerContextMenuAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_RULER_CONTEXT_MENU);
		setAction(ITextEditorActionDefinitionIds.SHOW_RULER_CONTEXT_MENU, action);
	}
	
	/**
	 * Creates this editor's standard actions and connects them with the global
	 * workbench actions.
	 * <p>
	 * Subclasses may extend.</p>
	 */
	protected void createActions() {
		
		ResourceAction action;
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Undo.", this, ITextOperationTarget.UNDO); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.UNDO_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.UNDO);
		setAction(ITextEditorActionConstants.UNDO, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Redo.", this, ITextOperationTarget.REDO); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.REDO_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.REDO);
		setAction(ITextEditorActionConstants.REDO, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Cut.", this, ITextOperationTarget.CUT); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CUT);
		setAction(ITextEditorActionConstants.CUT, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Copy.", this, ITextOperationTarget.COPY, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY);
		setAction(ITextEditorActionConstants.COPY, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Paste.", this, ITextOperationTarget.PASTE); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.PASTE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.PASTE);
		setAction(ITextEditorActionConstants.PASTE, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Delete.", this, ITextOperationTarget.DELETE); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.DELETE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE);
		setAction(ITextEditorActionConstants.DELETE, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.DeleteLine.", this, DeleteLineAction.WHOLE, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.DELETE_LINE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_LINE);
		setAction(ITextEditorActionConstants.DELETE_LINE, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.CutLine.", this, DeleteLineAction.WHOLE, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_LINE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CUT_LINE);
		setAction(ITextEditorActionConstants.CUT_LINE, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.DeleteLineToBeginning.", this, DeleteLineAction.TO_BEGINNING, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.DELETE_LINE_TO_BEGINNING_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_LINE_TO_BEGINNING);
		setAction(ITextEditorActionConstants.DELETE_LINE_TO_BEGINNING, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.CutLineToBeginning.", this, DeleteLineAction.TO_BEGINNING, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_LINE_TO_BEGINNING_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CUT_LINE_TO_BEGINNING);
		setAction(ITextEditorActionConstants.CUT_LINE_TO_BEGINNING, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.DeleteLineToEnd.", this, DeleteLineAction.TO_END, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.DELETE_LINE_TO_END_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_LINE_TO_END);
		setAction(ITextEditorActionConstants.DELETE_LINE_TO_END, action);

		action= new DeleteLineAction(EditorMessages.getResourceBundle(), "Editor.CutLineToEnd.", this, DeleteLineAction.TO_END, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_LINE_TO_END_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CUT_LINE_TO_END);
		setAction(ITextEditorActionConstants.CUT_LINE_TO_END, action);
		
		action= new MarkAction(EditorMessages.getResourceBundle(), "Editor.SetMark.", this, MarkAction.SET_MARK); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SET_MARK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SET_MARK);
		setAction(ITextEditorActionConstants.SET_MARK, action);

		action= new MarkAction(EditorMessages.getResourceBundle(), "Editor.ClearMark.", this, MarkAction.CLEAR_MARK); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.CLEAR_MARK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CLEAR_MARK);
		setAction(ITextEditorActionConstants.CLEAR_MARK, action);

		action= new MarkAction(EditorMessages.getResourceBundle(), "Editor.SwapMark.", this, MarkAction.SWAP_MARK); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SWAP_MARK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SWAP_MARK);
		setAction(ITextEditorActionConstants.SWAP_MARK, action);

		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.SelectAll.", this, ITextOperationTarget.SELECT_ALL, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_ALL);
		setAction(ITextEditorActionConstants.SELECT_ALL, action);
		
		action= new ShiftAction(EditorMessages.getResourceBundle(), "Editor.ShiftRight.", this, ITextOperationTarget.SHIFT_RIGHT); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SHIFT_RIGHT_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHIFT_RIGHT);
		setAction(ITextEditorActionConstants.SHIFT_RIGHT, action);
		
		action= new ShiftAction(EditorMessages.getResourceBundle(), "Editor.ShiftLeft.", this, ITextOperationTarget.SHIFT_LEFT); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SHIFT_LEFT_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHIFT_LEFT);
		setAction(ITextEditorActionConstants.SHIFT_LEFT, action);
		
		action= new TextOperationAction(EditorMessages.getResourceBundle(), "Editor.Print.", this, ITextOperationTarget.PRINT, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.PRINT_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.PRINT);
		setAction(ITextEditorActionConstants.PRINT, action);
		
		action= new FindReplaceAction(EditorMessages.getResourceBundle(), "Editor.FindReplace.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_REPLACE);
		setAction(ITextEditorActionConstants.FIND, action);

		action= new FindNextAction(EditorMessages.getResourceBundle(), "Editor.FindNext.", this, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_NEXT_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_NEXT);
		setAction(ITextEditorActionConstants.FIND_NEXT, action);

		action= new FindNextAction(EditorMessages.getResourceBundle(), "Editor.FindPrevious.", this, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_PREVIOUS_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_PREVIOUS);
		setAction(ITextEditorActionConstants.FIND_PREVIOUS, action);

		action= new IncrementalFindAction(EditorMessages.getResourceBundle(), "Editor.FindIncremental.", this, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_INCREMENTAL_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_INCREMENTAL);
		setAction(ITextEditorActionConstants.FIND_INCREMENTAL, action);
		
		action= new IncrementalFindAction(EditorMessages.getResourceBundle(), "Editor.FindIncrementalReverse.", this, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_INCREMENTAL_REVERSE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_INCREMENTAL_REVERSE);
		setAction(ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE, action);
				
		action= new SaveAction(EditorMessages.getResourceBundle(), "Editor.Save.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SAVE_ACTION);
		// action.setActionDefinitionId(ITextEditorActionDefinitionIds.SAVE);
		setAction(ITextEditorActionConstants.SAVE, action);
		
		action= new RevertToSavedAction(EditorMessages.getResourceBundle(), "Editor.Revert.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.REVERT_TO_SAVED_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.REVERT_TO_SAVED);
		setAction(ITextEditorActionConstants.REVERT_TO_SAVED, action);
		
		action= new GotoLineAction(EditorMessages.getResourceBundle(), "Editor.GotoLine.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.GOTO_LINE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
		setAction(ITextEditorActionConstants.GOTO_LINE, action);
		
		action = new MoveLinesAction(EditorMessages.getResourceBundle(), "Editor.MoveLinesUp.", this, true, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
		setAction(ITextEditorActionConstants.MOVE_LINE_UP, action);
		
		action = new MoveLinesAction(EditorMessages.getResourceBundle(), "Editor.MoveLinesDown.", this, false, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
		setAction(ITextEditorActionConstants.MOVE_LINE_DOWN, action);
		
		action = new MoveLinesAction(EditorMessages.getResourceBundle(), "Editor.CopyLineUp.", this, true, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_UP);
		setAction(ITextEditorActionConstants.COPY_LINE_UP, action);
		
		action = new MoveLinesAction(EditorMessages.getResourceBundle(), "Editor.CopyLineDown.", this, false, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_DOWN);
		setAction(ITextEditorActionConstants.COPY_LINE_DOWN, action);
		
		action = new CaseAction(EditorMessages.getResourceBundle(), "Editor.UpperCase.", this, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.UPPER_CASE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.UPPER_CASE);
		setAction(ITextEditorActionConstants.UPPER_CASE, action);
		
		action = new CaseAction(EditorMessages.getResourceBundle(), "Editor.LowerCase.", this, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.LOWER_CASE_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LOWER_CASE);
		setAction(ITextEditorActionConstants.LOWER_CASE, action);
		
		action = new SmartEnterAction(EditorMessages.getResourceBundle(), "Editor.SmartEnter.", this, false); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SMART_ENTER_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SMART_ENTER);
		setAction(ITextEditorActionConstants.SMART_ENTER, action);
		
		action = new SmartEnterAction(EditorMessages.getResourceBundle(), "Editor.SmartEnterInverse.", this, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.SMART_ENTER_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SMART_ENTER_INVERSE);
		setAction(ITextEditorActionConstants.SMART_ENTER_INVERSE, action);
		
		
		markAsContentDependentAction(ITextEditorActionConstants.UNDO, true);
		markAsContentDependentAction(ITextEditorActionConstants.REDO, true);
		markAsContentDependentAction(ITextEditorActionConstants.FIND, true);
		markAsContentDependentAction(ITextEditorActionConstants.FIND_NEXT, true);
		markAsContentDependentAction(ITextEditorActionConstants.FIND_PREVIOUS, true);
		markAsContentDependentAction(ITextEditorActionConstants.FIND_INCREMENTAL, true);
		markAsContentDependentAction(ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE, true);
		
		markAsSelectionDependentAction(ITextEditorActionConstants.CUT, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.COPY, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.PASTE, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.DELETE, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.SHIFT_RIGHT, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.SHIFT_LEFT, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.UPPER_CASE, true);
		markAsSelectionDependentAction(ITextEditorActionConstants.LOWER_CASE, true);
		
		markAsPropertyDependentAction(ITextEditorActionConstants.REVERT_TO_SAVED, true);
		
		markAsStateDependentAction(ITextEditorActionConstants.UNDO, true);
		markAsStateDependentAction(ITextEditorActionConstants.REDO, true);
		markAsStateDependentAction(ITextEditorActionConstants.CUT, true);
		markAsStateDependentAction(ITextEditorActionConstants.PASTE, true);
		markAsStateDependentAction(ITextEditorActionConstants.DELETE, true);
		markAsStateDependentAction(ITextEditorActionConstants.SHIFT_RIGHT, true);
		markAsStateDependentAction(ITextEditorActionConstants.SHIFT_LEFT, true);
		markAsStateDependentAction(ITextEditorActionConstants.FIND, true);
		markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE, true);
		markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE_TO_BEGINNING, true);
		markAsStateDependentAction(ITextEditorActionConstants.DELETE_LINE_TO_END, true);
		markAsStateDependentAction(ITextEditorActionConstants.MOVE_LINE_UP, true);
		markAsStateDependentAction(ITextEditorActionConstants.MOVE_LINE_DOWN, true);
		markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE, true);
		markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE_TO_BEGINNING, true);
		markAsStateDependentAction(ITextEditorActionConstants.CUT_LINE_TO_END, true);
		
		setActionActivationCode(ITextEditorActionConstants.SHIFT_RIGHT,'\t', -1, SWT.NONE);
		setActionActivationCode(ITextEditorActionConstants.SHIFT_LEFT, '\t', -1, SWT.SHIFT);
	}
	
	/**
	 * Convenience method to add the action installed under the given action id to the given menu.
	 * @param menu the menu to add the action to
	 * @param actionId the id of the action to be added
	 */
	protected final void addAction(IMenuManager menu, String actionId) {
		IAction action= getAction(actionId);
		if (action != null) {
			if (action instanceof IUpdate)
				((IUpdate) action).update();
			menu.add(action);
		}
	}
	
	/**
	 * Convenience method to add the action installed under the given action id to the specified group of the menu.
	 * @param menu the menu to add the action to
	 * @param group the group in the menu
	 * @param actionId the id of the action to add
	 */
	protected final void addAction(IMenuManager menu, String group, String actionId) {
	 	IAction action= getAction(actionId);
	 	if (action != null) {
	 		if (action instanceof IUpdate)
	 			((IUpdate) action).update();
	 			
	 		IMenuManager subMenu= menu.findMenuUsingPath(group);
	 		if (subMenu != null)
	 			subMenu.add(action);
	 		else
	 			menu.appendToGroup(group, action);
	 	}
	}
	 
	/**
	 * Convenience method to add a new group after the specified group.
	 * @param menu the menu to add the new group to
	 * @param existingGroup the group after which to insert the new group
	 * @param newGroup the new group
	 */
	protected final void addGroup(IMenuManager menu, String existingGroup, String newGroup) {
 		IMenuManager subMenu= menu.findMenuUsingPath(existingGroup);
 		if (subMenu != null)
 			subMenu.add(new Separator(newGroup));
 		else
 			menu.appendToGroup(existingGroup, new Separator(newGroup));
	}
		
	/**
	 * Sets up the ruler context menu before it is made visible.
	 * <p>
	 * Subclasses may extend to add other actions.</p>
	 *
	 * @param menu the menu
	 */
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
		menu.add(new Separator(ITextEditorActionConstants.MB_ADDITIONS));

		for (Iterator i = fRulerContextMenuListeners.iterator(); i.hasNext();)
			((IMenuListener) i.next()).menuAboutToShow(menu);					
		
		addAction(menu, ITextEditorActionConstants.RULER_MANAGE_BOOKMARKS);
		addAction(menu, ITextEditorActionConstants.RULER_MANAGE_TASKS);
	}
	
	/**
	 * Sets up this editor's context menu before it is made visible.
	 * <p>
	 * Subclasses may extend to add other actions.</p>
	 *
	 * @param menu the menu
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_UNDO));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_COPY));		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_PRINT));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_FIND));	
		menu.add(new Separator(ITextEditorActionConstants.GROUP_ADD));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
		menu.add(new Separator(ITextEditorActionConstants.MB_ADDITIONS));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_SAVE));
		
		if (isEditable()) {
			addAction(menu, ITextEditorActionConstants.GROUP_UNDO, ITextEditorActionConstants.UNDO);
			addAction(menu, ITextEditorActionConstants.GROUP_UNDO, ITextEditorActionConstants.REVERT_TO_SAVED);			
			addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.CUT);
			addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
			addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.PASTE);
			addAction(menu, ITextEditorActionConstants.GROUP_SAVE, ITextEditorActionConstants.SAVE);
		} else {
			addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
		}
	}

	/**
	 * Returns the status line manager of this editor.
	 * @return the status line manager of this editor
	 * @since 2.0
	 */
	private IStatusLineManager getStatusLineManager() {

		IEditorActionBarContributor contributor= getEditorSite().getActionBarContributor();		
		if (!(contributor instanceof EditorActionBarContributor))
			return null;
			
		IActionBars actionBars= ((EditorActionBarContributor) contributor).getActionBars();
		if (actionBars == null)
			return null;
			
		return actionBars.getStatusLineManager();
	}
	
	/*
	 * @see IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class required) {
		
		if (IEditorStatusLine.class.equals(required)) {
			if (fEditorStatusLine == null) {
				IStatusLineManager statusLineManager= getStatusLineManager();
				ISelectionProvider selectionProvider= getSelectionProvider();
				if (statusLineManager != null && selectionProvider != null)
					fEditorStatusLine= new EditorStatusLine(statusLineManager, selectionProvider);
			}
			return fEditorStatusLine;
		}
		
		if (IVerticalRulerInfo.class.equals(required)) {
			if (fVerticalRuler  instanceof IVerticalRulerInfo)
				return fVerticalRuler;
		}
		
		if (IMarkRegionTarget.class.equals(required)) {
			if (fMarkRegionTarget == null) {
				IStatusLineManager manager= getStatusLineManager();
				if (manager != null)
					fMarkRegionTarget= (fSourceViewer == null ? null : new MarkRegionTarget(fSourceViewer, manager));
			}
			return fMarkRegionTarget;
		}
		
		if (DeleteLineTarget.class.equals(required)){
			if (fDeleteLineTarget == null) {
				fDeleteLineTarget = new DeleteLineTarget(fSourceViewer);
			}
			return fDeleteLineTarget;
		}

		if (IncrementalFindTarget.class.equals(required)) {
			if (fIncrementalFindTarget == null) {
				IStatusLineManager manager= getStatusLineManager();				
				if (manager != null)
					fIncrementalFindTarget= (fSourceViewer == null ? null : new IncrementalFindTarget(fSourceViewer, manager));
			}
			return fIncrementalFindTarget;
		}
		
		if (IFindReplaceTarget.class.equals(required)) {
			if (fFindReplaceTarget == null) {
				IFindReplaceTarget target= (fSourceViewer == null ? null : fSourceViewer.getFindReplaceTarget());
				if (target != null) {
					fFindReplaceTarget= new FindReplaceTarget(this, target);
					if (fFindScopeHighlightColor != null)
						fFindReplaceTarget.setScopeHighlightColor(fFindScopeHighlightColor);
				}
			}
			return fFindReplaceTarget;
		}
		
		if (ITextOperationTarget.class.equals(required))
			return (fSourceViewer == null ? null : fSourceViewer.getTextOperationTarget());
			
		if (IRewriteTarget.class.equals(required)) {
			if (fSourceViewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) fSourceViewer;
				return extension.getRewriteTarget();
			}
			return null;
		}
		
		return super.getAdapter(required);
	}
		
	/*
	 * @see IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		if (fSourceViewer != null && fSourceViewer.getTextWidget() != null)
			fSourceViewer.getTextWidget().setFocus();
	}
		
	/*
	 * @see ITextEditor#showsHighlightRangeOnly()
	 */
	public boolean showsHighlightRangeOnly() {
		return fShowHighlightRangeOnly;
	}
	
	/*
	 * @see ITextEditor#showHighlightRangeOnly(boolean)
	 */
	public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
		fShowHighlightRangeOnly= showHighlightRangeOnly;
	}
	
	/*
	 * @see ITextEditor#setHighlightRange(int, int, boolean)
	 */
	public void setHighlightRange(int start, int length, boolean moveCursor) {
		if (fSourceViewer == null)
			return;
			
		if (fShowHighlightRangeOnly) {
			if (moveCursor)
				fSourceViewer.setVisibleRegion(start, length);
		} else {
			IRegion rangeIndication= fSourceViewer.getRangeIndication();
			if (rangeIndication == null || start != rangeIndication.getOffset() || length != rangeIndication.getLength())
				fSourceViewer.setRangeIndication(start, length, moveCursor);
		}
	}
	
	/*
	 * @see ITextEditor#getHighlightRange()
	 */
	public IRegion getHighlightRange() {
		if (fSourceViewer == null)
			return null;
			
		if (fShowHighlightRangeOnly)
			return getCoverage(fSourceViewer);
			
		return fSourceViewer.getRangeIndication();
	}
	
	/*
	 * @see ITextEditor#resetHighlightRange	 
	 */
	public void resetHighlightRange() {
		if (fSourceViewer == null)
			return;
		
		if (fShowHighlightRangeOnly)
			fSourceViewer.resetVisibleRegion();
		else
			fSourceViewer.removeRangeIndication();
	}
	
	/**
	 * Adjusts the highlight range so that at least the specified range 
	 * is highlighted.
	 * <p>
	 * Subclasses may re-implement this method.</p>
	 *
	 * @param offset the offset of the range which at least should be highlighted
	 * @param length the length of the range which at least should be highlighted 
	 */
	protected void adjustHighlightRange(int offset, int length) {
		if (fSourceViewer == null)
			return;
		
		if (!isVisible(fSourceViewer, offset, length))
			fSourceViewer.resetVisibleRegion();
	}
	
	/*
	 * @see ITextEditor#selectAndReveal(int, int)
	 */
	public void selectAndReveal(int start, int length) {
		if (fSourceViewer == null)
			return;
			
		ISelection selection= getSelectionProvider().getSelection();
		if (selection instanceof TextSelection) {
			TextSelection textSelection= (TextSelection) selection;
			if (textSelection.getOffset() != 0 || textSelection.getLength() != 0)
				markInNavigationHistory();
		}
				
		StyledText widget= fSourceViewer.getTextWidget();
		widget.setRedraw(false);
		{
			adjustHighlightRange(start, length);
			
			fSourceViewer.revealRange(start, length);
			fSourceViewer.setSelectedRange(start, length);
			
			markInNavigationHistory();
		}
		widget.setRedraw(true);
	}
	
	/*
	 * @see org.eclipse.ui.INavigationLocationProvider#createNavigationLocation()
	 * @since 2.1
	 */
	public INavigationLocation createEmptyNavigationLocation() {
		return new TextSelectionNavigationLocation(this, false);
	}
	
	/*
	 * @see org.eclipse.ui.INavigationLocationProvider#createNavigationLocation()
	 */
	public INavigationLocation createNavigationLocation() {
		return new TextSelectionNavigationLocation(this, true);
	}
	
	/**
	 * Writes a check mark of the given situation into the navigation history.
	 * @since 2.1
	 */
	protected void markInNavigationHistory() {
		IWorkbenchPage page= getEditorSite().getPage();
		page.getNavigationHistory().markLocation(this);
	}
	
	/**
	 * Hook which gets called when the editor has been saved.
	 * Subclasses may extend.
	 * @since 2.1
	 */
	protected void editorSaved() {
		IWorkbenchPage page= getEditorSite().getPage();
		INavigationLocation[] locations= page.getNavigationHistory().getLocations();
		IEditorInput input= getEditorInput();		
		for (int i= 0; i < locations.length; i++) {
			if (locations[i] instanceof TextSelectionNavigationLocation) {
				if(input.equals(locations[i].getInput())) {
					TextSelectionNavigationLocation location= (TextSelectionNavigationLocation) locations[i];
					location.partSaved(this);
				}
			}
		}
	}
	
	/*
	 * @see WorkbenchPart#firePropertyChange(int)
	 */
	public void firePropertyChange(int property) {
		super.firePropertyChange(property);
		updatePropertyDependentActions();
	}
	
	/*
	 * @see ITextEditorExtension#setStatusField(IStatusField, String)
	 * @since 2.0
	 */
	public void setStatusField(IStatusField field, String category) {
		Assert.isNotNull(category);
		if (field != null) {
			
			if (fStatusFields == null)
				fStatusFields= new HashMap(3);			
			
			fStatusFields.put(category, field);
			updateStatusField(category);
			
		} else if (fStatusFields != null)
			fStatusFields.remove(category);
	}
	
	/**
	 * Returns the current status field for the given status category.
	 * 
	 * @param category the status category
	 * @return the current status field for the given status category
	 * @since 2.0
	 */
	protected IStatusField getStatusField(String category) {
		if (category != null && fStatusFields != null)
			return (IStatusField) fStatusFields.get(category);
		return null;
	}
	
	/**
	 * Returns whether this editor is in overwrite or insert mode.
	 * 
	 * @return <code>true</code> if in insert mode, <code>false</code> for overwrite mode
	 * @since 2.0
	 * @deprecated
	 */
	protected boolean isInInsertMode() {
		InsertMode mode= getInsertMode();
		return mode == INSERT || mode == SMART_INSERT;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#getInsertMode()
	 */
	public InsertMode getInsertMode() {
		return fInsertMode;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#setInsertMode(org.eclipse.ui.texteditor.ITextEditorExtension3.InsertMode)
	 */
	public void setInsertMode(InsertMode newMode) {
		List legalModes= getLegalInsertModes();
		if (!legalModes.contains(newMode))
			throw new IllegalArgumentException();
		
		InsertMode oldMode= fInsertMode;
		fInsertMode= newMode;
		
		if (getSourceViewer() != null && (oldMode == OVERWRITE || newMode == OVERWRITE)) {
			StyledText styledText= getSourceViewer().getTextWidget();
			styledText.invokeAction(ST.TOGGLE_OVERWRITE);
		}
		
		handleInsertModeChanged();
	}
	
	/**
	 * Returns the set of legal insert modes. If insert modes are configured all defined insert modes
	 * are legal.
	 * 
	 * @return the set of legal insert modes
	 */
	protected List getLegalInsertModes() {
		if (fLegalInsertModes == null) {
			fLegalInsertModes= new ArrayList();
			fLegalInsertModes.add(OVERWRITE);
			fLegalInsertModes.add(INSERT);
			fLegalInsertModes.add(SMART_INSERT);
		}
		return fLegalInsertModes;
	}
	
	private void switchToNextInsertMode() {
		
		InsertMode mode= getInsertMode();
		List legalModes= getLegalInsertModes();
			
		int i= 0;
		while (i < legalModes.size()) {
			if (legalModes.get(i) == mode) break;
			++ i;
		}
			
		i= (i + 1) % legalModes.size();
		InsertMode newMode= (InsertMode) legalModes.get(i);				
		setInsertMode(newMode);
	}

	
	/**
	 * Configures the given insert mode as legal or inlegal. This call is ignored if the set of legal
	 * input modes would be empty after the call.
	 * 
	 * @param mode the insert mode to be configured
	 * @param legal <code>true</code> if the given mode is legal, <code>false</code> otherwise
	 */
	protected void configureInsertMode(InsertMode mode, boolean legal) {
		List legalModes= getLegalInsertModes();		
		if (legal) {
			if (!legalModes.contains(mode))
				legalModes.add(mode);
		} else if (legalModes.size() > 1) {
			if (getInsertMode() == mode)
				switchToNextInsertMode();
			legalModes.remove(mode);
		}
	}

	private Image createOverwriteModeCaretImage(StyledText styledText) {

		if (!"win32".equals(SWT.getPlatform()))  //$NON-NLS-1$
			return null;

		PaletteData caretPalette= new PaletteData(new RGB[] {new RGB (0,0,0), new RGB (255,255,255)});
		ImageData imageData = new ImageData(100, styledText.getLineHeight(), 1, caretPalette);
		Display display = styledText.getDisplay();
		Image blockImage= new Image(display, imageData);
		GC gc = new GC (blockImage);
		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.setFont(styledText.getFont());
		Point extent= gc.stringExtent("l");  //$NON-NLS-1$
		gc.fillRectangle(0, 0,extent.x, imageData.height -1);
		gc.dispose();
			
		return blockImage;
	}
	
	private Caret createOverwriteCaret(StyledText styledText) {
		Caret caret= new Caret(styledText, SWT.NULL);
		Image image= createOverwriteModeCaretImage(styledText);
		if (image != null)
			caret.setImage(image);
		else
			caret.setSize(1, caret.getSize().y);
			
		return caret;
	}
	
	private Image createSmartInsertModeCaretImage(StyledText styledText) {
		
		if (!"win32".equals(SWT.getPlatform()))  //$NON-NLS-1$
			return null;
		
		PaletteData caretPalette= new PaletteData(new RGB[] {new RGB (0,0,0), new RGB (255,255,255)});
		ImageData imageData = new ImageData(4, styledText.getLineHeight(), 1, caretPalette);
		Display display = styledText.getDisplay();
		Image bracketImage= new Image(display, imageData);
		GC gc = new GC (bracketImage);
		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.drawLine(0, 0, imageData.width -1, 0);
		gc.drawLine(0, 0, 0, imageData.height -1);
		gc.drawLine(0, imageData.height -1, imageData.width -1, imageData.height -1);
		gc.dispose();
			
		return bracketImage;
	}
	
	private Caret createSmartInsertModeCaret(StyledText styledText) {
		Caret caret= new Caret(styledText, SWT.NULL);
		Image image= createSmartInsertModeCaretImage(styledText);
		if (image != null)
			caret.setImage(image);
		else
			caret.setSize(1, caret.getSize().y);
			
		return caret;
	}

	private void createInsertModeCarets() {
		StyledText styledText= getSourceViewer().getTextWidget();
		fInsertModeCaret= styledText.getCaret();
		fOverwriteModeCaret= createOverwriteCaret(styledText);
		fSmartInsertModeCaret= createSmartInsertModeCaret(styledText);
		styledText.setCaret(fInsertModeCaret);
	}
	
	private void updateCaret() {
		
		if (getSourceViewer() == null)
			return;
			
		StyledText styledText= getSourceViewer().getTextWidget();
		
		Caret caret= null;
		Image image= null;
		
		InsertMode mode= getInsertMode();
		if (OVERWRITE == mode) {
			caret= fOverwriteModeCaret;
			image= createOverwriteModeCaretImage(styledText);
		} else if (INSERT == mode) {
			caret= fInsertModeCaret;
		} else if (SMART_INSERT == mode) {
			caret= fSmartInsertModeCaret;
			image= createSmartInsertModeCaretImage(styledText);
		}
		
		if (caret != null) {
			if (image != null) {
				Image oldImage= caret.getImage();
				caret.setImage(image);
				if (oldImage != null)
					oldImage.dispose();
			}
			styledText.setCaret(caret);
		}
	}
		
	/**
	 * Handles a change of the editor's insert mode.
	 * Subclasses may extend.
	 * 
	 * @since 2.0
	 */
	protected void handleInsertModeChanged() {
		updateCaret();
		updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE);
	}
	
	/**
	 * Handles a potential change of the cursor position.
	 * Subclasses may extend.
	 * 
	 * @since 2.0
	 */
	protected void handleCursorPositionChanged() {
		updateStatusField(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION);
	}

	/**
	 * Updates the status fields for the given category.
	 * 
	 * @param category
	 * @since 2.0
	 */
	protected void updateStatusField(String category) {
		
		if (category == null)
			return;
			
		IStatusField field= getStatusField(category);
		if (field != null) {
	
			String text= null;
			
			if (ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION.equals(category))
				text= getCursorPosition();
			else if (ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE.equals(category))
				text= isEditorInputReadOnly() ? fReadOnlyLabel : fWritableLabel;
			else if (ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE.equals(category)) {
				InsertMode mode= getInsertMode();
				if (OVERWRITE == mode)
					text= fOverwriteModeLabel;
				else if (INSERT == mode)
					text= fInsertModeLabel;
				else if (SMART_INSERT == mode)
					text= fSmartInsertModeLabel;
			}
			
			field.setText(text == null ? fErrorLabel : text);
		}
	}
	
	/**
	 * Updates all status fields.
	 * 
	 * @since 2.0
	 */
	protected void updateStatusFields() {
		if (fStatusFields != null) {
			Iterator e= fStatusFields.keySet().iterator();
			while (e.hasNext())
				updateStatusField((String) e.next());
		}
	}
	
	/**
	 * Returns a description of the cursor position.
	 * 
	 * @return a description of the cursor position
	 * @since 2.0
	 */
	protected String getCursorPosition() {
		
		if (fSourceViewer == null)
			return fErrorLabel;
		
		StyledText styledText= fSourceViewer.getTextWidget();
		int caret= widgetOffset2ModelOffset(fSourceViewer, styledText.getCaretOffset());
		IDocument document= fSourceViewer.getDocument();

		if (document == null)
			return fErrorLabel;
	
		try {
			
			int line= document.getLineOfOffset(caret);

			int lineOffset= document.getLineOffset(line);
			int tabWidth= styledText.getTabs();
			int column= 0;
			for (int i= lineOffset; i < caret; i++)
				if ('\t' == document.getChar(i))
					column += tabWidth - (column % tabWidth);
				else
					column++;
					
			fLineLabel.fValue= line + 1;
			fColumnLabel.fValue= column + 1;
			return MessageFormat.format(fPositionLabelPattern, fPositionLabelPatternArguments);
			
		} catch (BadLocationException x) {
			return fErrorLabel;
		}
	}
	
	/*
	 * @see ITextEditorExtension#isEditorInputReadOnly()
	 * @since 2.0
	 */
	public boolean isEditorInputReadOnly() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension) {
			IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;
			return extension.isReadOnly(getEditorInput());
		}
		return true;
	}
	
	/*
	 * @see ITextEditorExtension2#isEditorInputModifiable()
	 * @since 2.1
	 */
	public boolean isEditorInputModifiable() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof IDocumentProviderExtension) {
			IDocumentProviderExtension extension= (IDocumentProviderExtension) provider;
			return extension.isModifiable(getEditorInput());
		}
		return true;
	}
	
	/*
	 * @see ITextEditorExtension#addRulerContextMenuListener(IMenuListener)
	 * @since 2.0
	 */
	public void addRulerContextMenuListener(IMenuListener listener) {
		fRulerContextMenuListeners.add(listener);	
	}
	
	/*
	 * @see ITextEditorExtension#removeRulerContextMenuListener(IMenuListener)
	 * @since 2.0
	 */
	public void removeRulerContextMenuListener(IMenuListener listener) {
		fRulerContextMenuListeners.remove(listener);
	}
	
	/**
	 * Returns wether this editor can handle the move of the original element
	 * so that it ends up being the moved element. By default this method returns
	 * <code>true</code>.
	 * Subclasses may reimplement.
	 *  
	 * @param originalElement the original element
	 * @param movedElement the moved element
	 * @since 2.0
	 */
	protected boolean canHandleMove(IEditorInput originalElement, IEditorInput movedElement) {
		return true;
	}
	
	/**
	 * Returns the offset of the given source viewer's document that corresponds
	 * to the given widget offset or <code>-1</code> if there is no such offset.
	 * 
	 * @param viewer the source viewer
	 * @param widgetOffset the widget offset
	 * @return the corresponding offset in the source viewer's document or <code>-1</code>
	 * @since 2.1
	 */
	protected final static int widgetOffset2ModelOffset(ISourceViewer viewer, int widgetOffset) {
		if (viewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
			return extension.widgetOffset2ModelOffset(widgetOffset);
		}
		return widgetOffset + viewer.getVisibleRegion().getOffset();
	}
	
	/**
	 * Returns the minimal region of the given source viewer's document that completely
	 * comprises everything that is visible in the viewer's widget.
	 * 
	 * @return the minimal region of the source viewer's document comprising the contents of the viewer's widget
	 * @since 2.1
	 */
	protected final static IRegion getCoverage(ISourceViewer viewer) {
		if (viewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
			return extension.getModelCoverage();
		}
		return viewer.getVisibleRegion();
	}
	
	/**
	 * Tells whether the given region is visible in the given source viewer.
	 * 
	 * @param viewer the source viewer
	 * @param offset the offset of the region
	 * @param length the length of the region
	 * @since 2.1
	 */
	protected final static boolean isVisible(ISourceViewer viewer, int offset, int length) {
		if (viewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
			IRegion overlap= extension.modelRange2WidgetRange(new Region(offset, length));
			return overlap != null;
		}
		return viewer.overlapsWithVisibleRegion(offset, length);
	}
}