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
package org.eclipse.search.internal.ui.text;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.dialogs.PropertyDialogAction;

import org.eclipse.search.ui.IContextMenuConstants;

import org.eclipse.search.internal.ui.SearchMessages;

/**
 * Action group that adds the Text search actions to a context menu and
 * the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
class NewTextSearchActionGroup extends ActionGroup {

	private ISelectionProvider fSelectionProvider;		
	private IWorkbenchPage fPage;
	private PropertyDialogAction fOpenPropertiesDialog;

	public NewTextSearchActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		IWorkbenchPartSite site= part.getSite();
		fSelectionProvider= site.getSelectionProvider();
		fPage= site.getPage();
		fOpenPropertiesDialog= new PropertyDialogAction(site.getShell(), fSelectionProvider);

		ISelection selection= fSelectionProvider.getSelection();

		if (selection instanceof IStructuredSelection)
			fOpenPropertiesDialog.selectionChanged((IStructuredSelection)selection);
		else
			fOpenPropertiesDialog.selectionChanged(selection);
	}
	
	public void fillContextMenu(IMenuManager menu) {
		// view must exist if we create a context menu for it.
		
		ISelection selection= getContext().getSelection();
		if (selection instanceof IStructuredSelection) {
			addOpenWithMenu(menu, (IStructuredSelection) selection);
			if (fOpenPropertiesDialog != null && fOpenPropertiesDialog.isEnabled() && selection != null &&fOpenPropertiesDialog.isApplicableForSelection((IStructuredSelection) selection))
				menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fOpenPropertiesDialog);
		}
			
	}
	
	private void addOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {
		if (selection == null || selection.size() != 1)
			return;
	
		Object o= selection.getFirstElement();
	
		if (!(o instanceof IAdaptable))
			return; 
	
		// Create menu
		IMenuManager submenu= new MenuManager(SearchMessages.getString("OpenWithMenu.label")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(fPage, (IAdaptable)o));
	
		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	private void setGlobalActionHandlers(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PROPERTIES, fOpenPropertiesDialog);		
	}
}	
