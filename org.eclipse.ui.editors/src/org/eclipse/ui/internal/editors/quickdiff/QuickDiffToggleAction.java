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

package org.eclipse.ui.internal.editors.quickdiff;

import java.util.Iterator;

import org.eclipse.ui.internal.editors.quickdiff.restore.QuickDiffRestoreAction;
import org.eclipse.ui.internal.editors.quickdiff.restore.RestoreAction;
import org.eclipse.ui.internal.editors.quickdiff.restore.RevertBlockAction;
import org.eclipse.ui.internal.editors.quickdiff.restore.RevertLineAction;
import org.eclipse.ui.internal.editors.quickdiff.restore.RevertSelectionAction;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ILineDiffer;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.IVerticalRulerInfoExtension;
import org.eclipse.jface.text.source.LineNumberRulerColumn;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.quickdiff.*;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.ui.internal.editors.quickdiff.engine.DocumentLineDiffer;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;

/**
 * Action to toggle the line number bar's quick diff display. When turned on, quick diff shows
 * the changes relative to the saved version of the file.
 * @since 3.0
 */
public class QuickDiffToggleAction implements IEditorActionDelegate, IUpdate {
	
	// TODO: make preferences visible in Preferences
	/** Preference key for the preferred reference. */
	private static final String PREF_PREFERRED_REFERENCE= "PREFERRED_REFERENCE"; //$NON-NLS-1$
	/** Preference key for the enable on open property. */
	private static final String PREF_ENABLE_ON_OPEN= "ENABLE_ON_OPEN"; //$NON-NLS-1$
	
	/** The editor we are working on. */
	ITextEditor fEditor= null;
	/** Our UI proxy action. */
	IAction fProxy;
	/** The restore actions associated with this toggle action. */
	QuickDiffRestoreAction[] fRestoreActions= 
		new QuickDiffRestoreAction[] { 
			new RevertSelectionAction(fEditor), 
			new RevertBlockAction(fEditor), 
			new RevertLineAction(fEditor), 
			new RestoreAction(fEditor)
		};
	/** The menu listener that adds the ruler context menu. */
	private IMenuListener fListener= new IMenuListener() {
		/** Group name for additions, in CompilationUnitEditor... */
		private static final String GROUP_ADD= "add"; //$NON-NLS-1$
		/** Group name for debug contributions */
		private static final String GROUP_DEBUB= "debug"; //$NON-NLS-1$
		private static final String GROUP_QUICKDIFF= "quickdiff"; //$NON-NLS-1$
		private static final String MENU_LABEL_KEY= "quickdiff.menu.label"; //$NON-NLS-1$
		private static final String MENU_ID= "quickdiff.menu"; //$NON-NLS-1$
		private static final String GROUP_RESTORE= "restore"; //$NON-NLS-1$

		public void menuAboutToShow(IMenuManager manager) {
			// update the toggle action itself
			update();

			IMenuManager menu= (IMenuManager)manager.find(MENU_ID);
			// only add menu if it isn't there yet
			if (menu == null) {
				/* HACK: preinstall menu groups
				 * This is needed since we get the blank context menu, but want to show up
				 * in the same position as the extension-added QuickDiffToggleAction.
				 * The extension is added at the end (naturally), but other menus (debug, add)
				 * don't add themselves to MB_ADDITIONS or alike, but rather to the end, too. So
				 * we preinstall their respective menu groups here.
				 */
				if (manager.find(GROUP_DEBUB) == null)
					manager.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, new Separator(GROUP_DEBUB));
				if (manager.find(GROUP_ADD) == null)
					manager.insertAfter(IWorkbenchActionConstants.MB_ADDITIONS, new Separator(GROUP_ADD));
				if (manager.find(GROUP_RESTORE) == null)
					manager.insertAfter(GROUP_ADD, new Separator(GROUP_RESTORE));
				if (manager.find(GROUP_QUICKDIFF) == null)
					manager.insertAfter(GROUP_RESTORE, new Separator(GROUP_QUICKDIFF));

				// create quickdiff menu
				menu= new MenuManager(QuickDiffTestPlugin.getResourceString(MENU_LABEL_KEY), MENU_ID);
				ReferenceProviderDescriptor[] descriptors= QuickDiffTestPlugin.getDefault().getExtensions();
				for (int i= 0; i < descriptors.length; i++) {
					ReferenceProviderDescriptor desc= descriptors[i];
					ReferenceSelectionAction action= new ReferenceSelectionAction(desc, fEditor);
					if (action.isEnabled())
						menu.add(action);
				}
				manager.appendToGroup(GROUP_QUICKDIFF, menu);

				// create restore menu if this action is enabled
				if (isConnected()) {
					for (int i= 0; i < fRestoreActions.length; i++) {
						fRestoreActions[i].update();
					}
					// only add block action if selection action is not enabled
					if (fRestoreActions[0].isEnabled())
						manager.appendToGroup(GROUP_RESTORE, fRestoreActions[0]);
					else if (fRestoreActions[1].isEnabled())
						manager.appendToGroup(GROUP_RESTORE, fRestoreActions[1]);
					if (fRestoreActions[2].isEnabled())
						manager.appendToGroup(GROUP_RESTORE, fRestoreActions[2]);
					if (fRestoreActions[3].isEnabled())
						manager.appendToGroup(GROUP_RESTORE, fRestoreActions[3]);
				}
			}
		}
	};

	/*
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fProxy= action;
		removePopupMenu();
		if (targetEditor instanceof ITextEditor) {
			fEditor= (ITextEditor)targetEditor;
		} else
			fEditor= null;
		for (int i= 0; i < fRestoreActions.length; i++) {
			fRestoreActions[i].setEditor(fEditor);
		}
		setPopupMenu();
		postAutoInit();
	}

	/**
	 * Requests the initialization of the quick diff if the preference <code>ENABLE_ON_OPEN</code> is set.
	 * The initialization is run via <code>asyncExec</code> because <code>setActiveEditor</code> is called
	 * before initializing the line number ruler column.
	 */
	private void postAutoInit() {
		Runnable runnable= new Runnable() {
			public void run() {
				boolean enable= TextEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREF_ENABLE_ON_OPEN);
				if (enable && !hasDiffer())
					QuickDiffToggleAction.this.run(fProxy);
			}

		};
		IWorkbench workbench= TextEditorPlugin.getDefault().getWorkbench();
		if (workbench == null)
			return;
		IWorkbenchWindow activeWorkbenchWindow= workbench.getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return;
		Shell shell= activeWorkbenchWindow.getShell();
		if (shell == null)
			return;
		Display display= shell.getDisplay();
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(runnable);
	}

	/**
	 * 
	 */
	private void removePopupMenu() {
		if (!(fEditor instanceof ITextEditorExtension))
			return;
		((ITextEditorExtension)fEditor).removeRulerContextMenuListener(fListener);
	}

	/**
	 * Installs a submenu with <code>fEditor</code>'s ruler context menu that contains the choices
	 * for the quick diff reference. This allows the toggle action to lazily install the menu once
	 * quick diff has been enabled. 
	 * @param editor the editor that the menu gets installed in.
	 * @see QuickDiffToggleAction
	 */
	private void setPopupMenu() {
		if (!(fEditor instanceof ITextEditorExtension))
			return;
		((ITextEditorExtension)fEditor).addRulerContextMenuListener(fListener);
	}

	/**
	 * States whether this toggle action has been installed and a incremental differ has been
	 * installed with the line number bar.
	 * @return <code>true</code> if a differ has been installed on <code>fEditor</code>.
	 */
	boolean isConnected() {
		IVerticalRulerColumn column= getColumn();
		if (column instanceof IVerticalRulerInfoExtension) {
			IAnnotationModel m= ((IVerticalRulerInfoExtension)column).getModel();
			if (m instanceof DocumentLineDiffer)
				return true;
		}
		return false;
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		fProxy= action;
		if (fEditor == null)
			return;
		IVerticalRulerColumn column= getColumn();
		if (column == null)
			return;
		IDocumentProvider provider= fEditor.getDocumentProvider();
		IEditorInput editorInput= fEditor.getEditorInput();
		IAnnotationModelExtension model= getModel();
		if (model == null)
			return;

		DocumentLineDiffer differ= getDiffer(model);
		IDocument actual= provider.getDocument(editorInput);
		if (actual == null)
			return;

		if (isConnected()) {
			column.setModel(null);
		} else {
			if (differ == null) {
				differ= createDiffer(model);
			}
			column.setModel(differ);
		}
	}

	boolean hasDiffer() {
		IAnnotationModelExtension model= getModel();
		return (model != null && getDiffer(model) != null);
	}

	private IAnnotationModelExtension getModel() {
		if (fEditor == null)
			return null;
		IDocumentProvider provider= fEditor.getDocumentProvider();
		IEditorInput editorInput= fEditor.getEditorInput();
		IAnnotationModel m= provider.getAnnotationModel(editorInput);
		if (m instanceof IAnnotationModelExtension) {
			return (IAnnotationModelExtension)m;
		} else {
			return null;
		}
	}

	/**
	 * Returns the linenumber ruler of <code>fEditor</code>, or <code>null</code> if it cannot be
	 * found.
	 * @return an instance of <code>LineNumberRulerColumn</code> or <code>null</code>.
	 */
	private IVerticalRulerColumn getColumn() {
		// HACK: we get the IVerticalRulerInfo and assume its a CompositeRuler.
		// will get broken if IVerticalRulerInfo implementation changes
		if (fEditor instanceof IAdaptable) {
			IVerticalRulerInfo info= (IVerticalRulerInfo) ((IAdaptable)fEditor).getAdapter(IVerticalRulerInfo.class);
			if (info instanceof CompositeRuler) {
				for (Iterator it= ((CompositeRuler)info).getDecoratorIterator(); it.hasNext();) {
					IVerticalRulerColumn c= (IVerticalRulerColumn)it.next();
					if (c instanceof LineNumberRulerColumn)
						return c;
				}
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fProxy= action;
	}

	/**
	 * Extracts the line differ from the annotation model.
	 * @param model the model
	 * @return the linediffer, or <code>null</code> if none found.
	 */
	private DocumentLineDiffer getDiffer(IAnnotationModelExtension model) {
		return (DocumentLineDiffer)model.getAnnotationModel(ILineDiffer.ID);
	}

	/**
	 * Creates a new <code>DocumentLineDiffer</code> and installs it with <code>model</code>.
	 * The default reference provider is installed with the newly created differ. 
	 * @param model the annotation model of the current document.
	 * @return a new <code>DocumentLineDiffer</code> instance.
	 */
	private DocumentLineDiffer createDiffer(IAnnotationModelExtension model) {
		DocumentLineDiffer differ;
		differ= new DocumentLineDiffer(new ProgressMonitorDialog(fEditor.getSite().getShell()));
		String defaultID= TextEditorPlugin.getDefault().getPreferenceStore().getString(PREF_PREFERRED_REFERENCE);
		ReferenceProviderDescriptor[] descs= QuickDiffTestPlugin.getDefault().getExtensions();
		IQuickDiffProviderImplementation provider= null;
		// try to fetch preferred provider; load if needed
		for (int i= 0; i < descs.length; i++) {
			if (descs[i].getId().equals(defaultID)) {
				provider= descs[i].createProvider();
				if (provider != null) {
					provider.setActiveEditor(fEditor);
					if (provider.isEnabled())
						break;
					provider.dispose();
					provider= null;
				}
			}
		}
		// if not found, get default provider
		if (provider == null) {
			provider= QuickDiffTestPlugin.getDefault().getDefaultProvider().createProvider();
			if (provider != null) {
				provider.setActiveEditor(fEditor);
				if (!provider.isEnabled()) {
					provider.dispose();
					provider= null;
				}
			}
		}
		if (provider != null)
			differ.setReferenceProvider(provider);
		model.addAnnotationModel(ILineDiffer.ID, differ);
		return differ;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		if (fProxy == null)
			return;
		if (isConnected())
			fProxy.setText(QuickDiffTestPlugin.getResourceString("quickdiff.toggle.disable")); //$NON-NLS-1$
		else
			fProxy.setText(QuickDiffTestPlugin.getResourceString("quickdiff.toggle.enable")); //$NON-NLS-1$
	}

}
