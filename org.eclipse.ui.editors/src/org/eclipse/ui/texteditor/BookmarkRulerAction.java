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


import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

/**
 * Adapter for the managing bookmark action.
 * @since 2.0
 */
public class BookmarkRulerAction extends AbstractRulerActionDelegate {
	
	/**
	 * @see AbstractRulerActionDelegate#createAction(ITextEditor, IVerticalRulerInfo)
	 */
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new MarkerRulerAction(EditorMessages.getResourceBundle(), "Editor.ManageBookmarks.", editor, rulerInfo, IMarker.BOOKMARK, true); //$NON-NLS-1$
	}
}
