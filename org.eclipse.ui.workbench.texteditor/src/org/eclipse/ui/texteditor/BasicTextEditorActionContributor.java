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

package org.eclipse.ui.texteditor;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;



/**
 * Manages the installation and deinstallation of global actions for 
 * the same type of editors.
 * <p>
 * If instantiated and used as-is, this contributor connects to all of the workbench defined
 * global editor actions the corresponding actions of the current editor. It also adds addition 
 * actions for searching and navigation (go to line) as well as a set of status fields.</p>
 * <p>
 * Subclasses may override the following methods:
 * <ul>
 *   <li><code>contributeToMenu</code> - extend to contribute to menu</li>
 *   <li><code>contributeToToolBar</code> - reimplement to contribute to toolbar</li>
 *   <li><code>contributeToStatusLine</code> - reimplement to contribute to status line</li>
 *   <li><code>setActiveEditor</code> - extend to react to editor changes</li>
 * </ul>
 * </p>
 * @see org.eclipse.ui.texteditor.ITextEditorActionConstants
 */
public class BasicTextEditorActionContributor extends EditorActionBarContributor {
	
	/** The global actions to be connected with editor actions */
	private final static String[] ACTIONS= {
		ITextEditorActionConstants.UNDO, 
		ITextEditorActionConstants.REDO,
		ITextEditorActionConstants.CUT,
		ITextEditorActionConstants.COPY,
		ITextEditorActionConstants.PASTE,
		ITextEditorActionConstants.DELETE,
		ITextEditorActionConstants.SELECT_ALL,
		ITextEditorActionConstants.FIND,
		ITextEditorActionConstants.PRINT,
		ITextEditorActionConstants.REVERT
	};
	
	/** 
	 * The status fields to be set to the editor
	 * @since 2.0
	 */
	private final static String[][] STATUSFIELDS= {
		{ ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE, null },
		{ ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE, ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE },
		{ ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION, ITextEditorActionConstants.GOTO_LINE }
	};
	
	/**
	 * The active editor part.
	 */
	private IEditorPart fActiveEditorPart;
	/** 
	 * The find next action.
	 * @since 2.0
	 */
	private RetargetTextEditorAction fFindNext;
	/** 
	 * The find previous action.
	 * @since 2.0
	 */
	private RetargetTextEditorAction fFindPrevious;	
	/** 
	 * The incremental find action.
	 * @since 2.0
	 */
	private RetargetTextEditorAction fIncrementalFind;	
	/**
	 * The reverse incremental find action.
	 * @since 2.1
	 */
	private RetargetTextEditorAction fIncrementalFindReverse;	
	/**
	 * The go to line action.
	 */
	private RetargetTextEditorAction fGotoLine;
	/** 
	 * The map of status fields.
	 * @since 2.0
	 */
	private Map fStatusFields;
	
	
	/**
	 * Creates an empty editor action bar contributor. The action bars are
	 * furnished later via the <code>init</code> method.
	 *
	 * @see org.eclipse.ui.IEditorActionBarContributor#init(org.eclipse.ui.IActionBars, org.eclipse.ui.IWorkbenchPage)
	 */
	public BasicTextEditorActionContributor() {
		
		fFindNext= new RetargetTextEditorAction(EditorMessages.getResourceBundle(), "Editor.FindNext."); //$NON-NLS-1$
		fFindNext.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_NEXT); 
		fFindPrevious= new RetargetTextEditorAction(EditorMessages.getResourceBundle(), "Editor.FindPrevious."); //$NON-NLS-1$
		fFindPrevious.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_PREVIOUS); 
		fIncrementalFind= new RetargetTextEditorAction(EditorMessages.getResourceBundle(), "Editor.FindIncremental."); //$NON-NLS-1$
		fIncrementalFind.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_INCREMENTAL); 
		fIncrementalFindReverse= new RetargetTextEditorAction(EditorMessages.getResourceBundle(), "Editor.FindIncrementalReverse."); //$NON-NLS-1$
		fIncrementalFindReverse.setActionDefinitionId(ITextEditorActionDefinitionIds.FIND_INCREMENTAL_REVERSE);
		fGotoLine= new RetargetTextEditorAction(EditorMessages.getResourceBundle(), "Editor.GotoLine."); //$NON-NLS-1$
		fGotoLine.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
		
		fStatusFields= new HashMap(3);
		for (int i= 0; i < STATUSFIELDS.length; i++)
			fStatusFields.put(STATUSFIELDS[i], new StatusLineContributionItem(STATUSFIELDS[i][0]));
	}
	
	/**
	 * Returns the active editor part.
	 *
	 * @return the active editor part
	 */
	protected final IEditorPart getActiveEditorPart() {
		return fActiveEditorPart;
	}
	
	/**
	 * Returns the action registered with the given text editor.
	 *
	 * @param editor the editor, or <code>null</code>
	 * @param actionId the action id
	 * @return the action, or <code>null</code> if none
	 */
	protected final IAction getAction(ITextEditor editor, String actionId) {
		return (editor == null || actionId == null ? null : editor.getAction(actionId));
	}
	
	/**
	 * The method installs the global action handlers for the given text editor.
	 * <p>
	 * This method cannot be overridden by subclasses.</p>
	 * @since 2.0
	 */
	private void doSetActiveEditor(IEditorPart part) {
		
		if (fActiveEditorPart == part)
			return;
			
		if (fActiveEditorPart instanceof ITextEditorExtension) {
			ITextEditorExtension extension= (ITextEditorExtension) fActiveEditorPart;
			for (int i= 0; i < STATUSFIELDS.length; i++)
				extension.setStatusField(null, STATUSFIELDS[i][0]);
		}

		fActiveEditorPart= part;
		ITextEditor editor= (part instanceof ITextEditor) ? (ITextEditor) part : null;
		
		IActionBars actionBars= getActionBars();
		if (actionBars != null) {
			for (int i= 0; i < ACTIONS.length; i++)
				actionBars.setGlobalActionHandler(ACTIONS[i], getAction(editor, ACTIONS[i]));
		}
		
		fFindNext.setAction(getAction(editor, ITextEditorActionConstants.FIND_NEXT));
		fFindPrevious.setAction(getAction(editor, ITextEditorActionConstants.FIND_PREVIOUS));
		fIncrementalFind.setAction(getAction(editor, ITextEditorActionConstants.FIND_INCREMENTAL));
		fIncrementalFindReverse.setAction(getAction(editor, ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE));
		fGotoLine.setAction(getAction(editor, ITextEditorActionConstants.GOTO_LINE));
		
		for (int i= 0; i < STATUSFIELDS.length; i++) {
			StatusLineContributionItem statusField= (StatusLineContributionItem) fStatusFields.get(STATUSFIELDS[i]);
			statusField.setActionHandler(getAction(editor, STATUSFIELDS[i][1]));
			if (fActiveEditorPart instanceof ITextEditorExtension) {
				ITextEditorExtension extension= (ITextEditorExtension) fActiveEditorPart;
				extension.setStatusField((IStatusField) statusField, STATUSFIELDS[i][0]);
			}
		}
	}
	
	/**
	 * The <code>BasicTextEditorActionContributor</code> implementation of this 
	 * <code>IEditorActionBarContributor</code> method installs the global 
	 * action handler for the given text editor by calling a private helper 
	 * method.
	 * <p>
	 * Subclasses may extend.</p>
	 */
	public void setActiveEditor(IEditorPart part) {
		doSetActiveEditor(part);
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {

		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fFindNext);
			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fFindPrevious);
			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fIncrementalFind);
			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fIncrementalFindReverse);
		}
		
		IMenuManager navigateMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		if (navigateMenu != null) {
			navigateMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fGotoLine);
		}
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToStatusLine(org.eclipse.jface.action.IStatusLineManager)
	 * @since 2.0
	 */
	public void contributeToStatusLine(IStatusLineManager statusLineManager) {
		super.contributeToStatusLine(statusLineManager);
		for (int i= 0; i < STATUSFIELDS.length; i++)
			statusLineManager.add((IContributionItem) fStatusFields.get(STATUSFIELDS[i]));
	}
	
	/*
	 * @see org.eclipse.ui.IEditorActionBarContributor#dispose()
	 * @since 2.0
	 */
	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}
	
	
//	/* (non-Javadoc)
//	 * @see org.eclipse.ui.IEditorActionBarContributor#init(org.eclipse.ui.IActionBars, org.eclipse.ui.IWorkbenchPage)
//	 */
//	public void init(IActionBars bars, IWorkbenchPage page) {
//		
//		if (page != null) {
//			IWorkbenchWindow window= page.getWorkbenchWindow();
//			if (window instanceof WorkbenchWindow) {
//				WorkbenchWindow wWindow= (WorkbenchWindow) window;
//				WWinKeyBindingService service= wWindow.getKeyBindingService();
//				if (service != null) {
//					service.registerGlobalAction(fFindNext);
//					service.registerGlobalAction(fFindPrevious);
//					service.registerGlobalAction(fIncrementalFind);
//					service.registerGlobalAction(fIncrementalFindReverse);
//					service.registerGlobalAction(fGotoLine);
//				}
//			}
//		}
//		super.init(bars, page);
//	}

}
