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

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.SearchResultView;
import org.eclipse.search.internal.ui.util.ListContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog telling the user that files are out of sync or matches
 * are stale and asks for confirmation to refresh/search again
 * @since 3.0
 */

public class SearchAgainConfirmationDialog extends Dialog {
	private List fOutOfSync;
	private List fOutOfDate;
	
	private static class ProxyLabelProvider extends LabelProvider {
		private ILabelProvider fLabelProvider;
		
		ProxyLabelProvider() {
			SearchResultView view= (SearchResultView) SearchPlugin.getSearchResultView();
			if (view != null)
				fLabelProvider= view.getLabelProvider();
			else
				fLabelProvider= null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (fLabelProvider != null)
				return fLabelProvider.getImage(element);
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (fLabelProvider != null)
				return fLabelProvider.getText(element);
			return null;
		}
		
	}
	
	SearchAgainConfirmationDialog(Shell shell, List outOfSync, List outOfDate) {
		super(shell);
		fOutOfSync= outOfSync;
		fOutOfDate= outOfDate;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite result= (Composite) super.createDialogArea(parent);
		
		if (fOutOfSync.size() > 0) {
			createLabel(result, SearchMessages.getString("SearchAgainConfirmationDialog.outofsync.message")); //$NON-NLS-1$
			
			createLabel(result, SearchMessages.getString("SearchAgainConfirmationDialog.outofsync.label")); //$NON-NLS-1$
			createTableViewer(fOutOfSync, result);
		} else {
			createLabel(result, SearchMessages.getString("SearchAgainConfirmationDialog.stale.message")); //$NON-NLS-1$
		}
		
		createLabel(result, SearchMessages.getString("SearchAgainConfirmationDialog.stale.label")); //$NON-NLS-1$
		createTableViewer(fOutOfDate, result);
		return result;
	}
	
	private void createLabel(Composite parent, String text) {
		Label message= new Label(parent, SWT.WRAP);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(40);
		message.setLayoutData(gd);
		message.setText(text);
	}
	
	private TableViewer createTableViewer(List input, Composite result) {
		TableViewer viewer= new TableViewer(result);
		viewer.setContentProvider(new ListContentProvider());
		viewer.setLabelProvider(new ProxyLabelProvider());
		viewer.setInput(input);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(40);
		gd.heightHint= convertHeightInCharsToPixels(5);
		viewer.getControl().setLayoutData(gd);
		return viewer;
	}
	
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SearchMessages.getString("SearchAgainConfirmationDialog.title")); //$NON-NLS-1$
	}
	
}
