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


import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
 

/**
 * Defines the names of those actions which are preregistered with the
 * <code>AbstractTextEditor</code>. <code>RULER_DOUBLE_CLICK</code> defines
 * the action which is registered as being executed when the editor's
 * ruler has been double clicked. This interface extends the set of names 
 * available from <code>IWorkbenchActionConstants</code>. It also defines the
 * names of the menu groups in a text editor's context menu.
 */
public interface ITextEditorActionConstants /*extends IWorkbenchActionConstants*/ {
	
	/** 
	 * Context menu group for undo/redo related actions. 
	 * Value: <code>"group.undo"</code>
	 */
	static final String GROUP_UNDO= "group.undo"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for copy/paste related actions. 
	 * Value: <code>"group.copy"</code>
	 */
	static final String GROUP_COPY= "group.copy"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for text manipulation actions. 
	 * Value: <code>"group.edit"</code>
	 */
	static final String GROUP_EDIT= "group.edit"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for print related actions. 
	 * Value: <code>"group.print"</code>
	 */
	static final String GROUP_PRINT= "group.print"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for find/replace related actions. 
	 * Value: <code>"group.find"</code>
	 */
	static final String GROUP_FIND= "group.find"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for save related actions. 
	 * Value: <code>"group.save"</code>
	 */
	static final String GROUP_SAVE= "group.save"; //$NON-NLS-1$
	
	/** 
	 * Context menu group for actions which do not fit in one of the other categories. 
	 * Value: <code>"group.rest"</code>
	 */
	static final String GROUP_REST= "group.rest"; //$NON-NLS-1$
	
	/** 
	 * Name of the action for shifting text blocks to the right. 
	 * Value: <code>"ShiftRight"</code>
	 */
	static final String SHIFT_RIGHT= "ShiftRight"; //$NON-NLS-1$
	
	/** 
	 * Name of the action for shifting text blocks to the left. 
	 * Value: <code>"ShiftLeft"</code>
	 */
	static final String SHIFT_LEFT= "ShiftLeft"; //$NON-NLS-1$
		
	/** 
	 * Name of the action to delete the current line. 
	 * Value: <code>"DeleteLine"</code>
	 * @since 2.0
	 */
	static final String DELETE_LINE= "DeleteLine"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to cut the current line. 
	 * Value: <code>"CutLine"</code>
	 * @since 2.1
	 */
	static final String CUT_LINE= "CutLine"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to delete line to beginning. 
	 * Value: <code>"DeleteLineToBeginning"</code>
	 * @since 2.0
	 */
	static final String DELETE_LINE_TO_BEGINNING= "DeleteLineToBeginning"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to cut line to beginning. 
	 * Value: <code>"CutLineToBeginning"</code>
	 * @since 2.1
	 */
	static final String CUT_LINE_TO_BEGINNING= "CutLineToBeginning"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to delete line to end. 
	 * Value: <code>"DeleteLineToEnd"</code>
	 * @since 2.0
	 */
	static final String DELETE_LINE_TO_END= "DeleteLineToEnd"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to cut line to end. 
	 * Value: <code>"CutLineToEnd"</code>
	 * @since 2.1
	 */
	static final String CUT_LINE_TO_END= "CutLineToEnd"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to set the mark.
	 * Value: <code>"SetMark"</code>
	 * @since 2.0
	 */
	static final String SET_MARK= "SetMark"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to set the mark.
	 * Value: <code>"ClearMark"</code>
	 * @since 2.0
	 */
	static final String CLEAR_MARK= "ClearMark"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to swap the mark with the cursor position. 
	 * Value: <code>"SwapMark"</code>
	 * @since 2.0
	 */
	static final String SWAP_MARK= "SwapMark"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to jump to a certain text line. 
	 * Value: <code>"GotoLine"</code>
	 */
	static final String GOTO_LINE= "GotoLine"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to insert a new line below the current position. 
	 * Value: <code>"SmartEnter"</code>
	 * @since 3.0
	 */
	static final String SMART_ENTER= "SmartEnter"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to insert a new line above the current position. 
	 * Value: <code>"SmartEnterInverse"</code>
	 * @since 3.0
	 */
	static final String SMART_ENTER_INVERSE= "SmartEnterInverse"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to move lines upwards 
	 * Value: <code>"MoveLineUp"</code>
	 * @since 3.0
	 */
	static final String MOVE_LINE_UP= "MoveLineUp"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to move lines downwards 
	 * Value: <code>"MoveLineDown"</code>
	 * @since 3.0
	 */
	static final String MOVE_LINE_DOWN= "MoveLineDown"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to copy lines upwards 
	 * Value: <code>"CopyLineUp"</code>
	 * @since 3.0
	 */
	static final String COPY_LINE_UP= "CopyLineUp"; //$NON-NLS-1$;

	/** 
	 * Name of the action to copy lines downwards 
	 * Value: <code>"CopyLineDown"</code>
	 * @since 3.0
	 */
	static final String COPY_LINE_DOWN= "CopyLineDown"; //$NON-NLS-1$;

	/** 
	 * Name of the action to turn a selection to upper case 
	 * Value: <code>"UpperCase"</code>
	 * @since 3.0
	 */
	static final String UPPER_CASE= "UpperCase"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to turn a selection to lower case 
	 * Value: <code>"LowerCase"</code>
	 * @since 3.0
	 */
	static final String LOWER_CASE= "LowerCase"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to find next. 
	 * Value: <code>"FindNext"</code>
	 * @since 2.0
	 */
	static final String FIND_NEXT= "FindNext"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to find previous. 
	 * Value: <code>"FindPrevious"</code>
	 * @since 2.0
	 */
	static final String FIND_PREVIOUS= "FindPrevious"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to incremental find. 
	 * Value: <code>"FindIncremental"</code>
	 * @since 2.0
	 */
	static final String FIND_INCREMENTAL= "FindIncremental"; //$NON-NLS-1$

	/** 
	 * Name of the action to incremental find reverse. 
	 * Value: <code>"FindIncrementalReverse"</code>
	 * @since 2.1
	 */
	static final String FIND_INCREMENTAL_REVERSE= "FindIncrementalReverse"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to convert line delimiters to Windows. 
	 * Value: <code>"ConvertLineDelimitersToWindows"</code>
	 * @since 2.0
	 */
	static final String CONVERT_LINE_DELIMITERS_TO_WINDOWS= "ConvertLineDelimitersToWindows"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to convert line delimiters to UNIX. 
	 * Value: <code>"ConvertLineDelimitersToUNIX"</code>
	 * @since 2.0
	 */
	static final String CONVERT_LINE_DELIMITERS_TO_UNIX= "ConvertLineDelimitersToUNIX"; //$NON-NLS-1$
	
	/** 
	 * Name of the action to convert line delimiters to MAC. 
	 * Value: <code>"ConvertLineDelimitersToMAC"</code>
	 * @since 2.0
	 */
	static final String CONVERT_LINE_DELIMITERS_TO_MAC= "ConvertLineDelimitersToMAC"; //$NON-NLS-1$
	
	/** 
	 * Name of the ruler action performed when double clicking the editor's vertical ruler. 
	 * Value: <code>"RulerDoubleClick"</code>
	 */
	static final String RULER_DOUBLE_CLICK= "RulerDoubleClick"; //$NON-NLS-1$
	
	/** 
	 * Name of the ruler action performed when clicking the editor's vertical ruler. 
	 * Value: <code>"RulerClick"</code>
	 * @since 2.0
	 */
	static final String RULER_CLICK= "RulerClick"; //$NON-NLS-1$
	
	/** 
	 * Name of the ruler action to manage tasks.
	 * Value: <code>"ManageTasks"</code>
	 */
	static final String RULER_MANAGE_TASKS= "ManageTasks"; //$NON-NLS-1$
	
	/** 
	 * Name of the ruler action to manage bookmarks. 
	 * Value: <code>"ManageBookmarks"</code>
	 */
	static final String RULER_MANAGE_BOOKMARKS= "ManageBookmarks"; //$NON-NLS-1$
	
	
	/**
	 * Status line category "input position".
	 * Value: <code>"InputPosition"</code>
	 * @since 2.0
	 */
	static final String STATUS_CATEGORY_INPUT_POSITION= "InputPosition"; //$NON-NLS-1$

	/**
	 * Status line category "input mode".
	 * Value: <code>"InputMode"</code>
	 * @since 2.0
	 */
	static final String STATUS_CATEGORY_INPUT_MODE= "InputMode"; //$NON-NLS-1$

	/**
	 * Status line category "element state".
	 * Value: <code>"ElementState"</code>
	 * @since 2.0
	 */
	static final String STATUS_CATEGORY_ELEMENT_STATE= "ElementState"; //$NON-NLS-1$
	
	
	// -------------------------------------
	
	static final String COPY= ActionFactory.COPY.getId();
	static final String CUT= ActionFactory.CUT.getId();
	static final String DELETE= ActionFactory.DELETE.getId();
	static final String FIND= ActionFactory.FIND.getId();
	static final String PASTE= ActionFactory.PASTE.getId();
	static final String PRINT= ActionFactory.PRINT.getId();
	static final String REDO= ActionFactory.REDO.getId();
	static final String UNDO= ActionFactory.UNDO.getId();
	static final String SAVE= ActionFactory.SAVE.getId();
	static final String SELECT_ALL= ActionFactory.SELECT_ALL.getId();
	static final String REVERT= ActionFactory.REVERT.getId();
	static final String GROUP_ADD= IWorkbenchActionConstants.GROUP_ADD;
	static final String MB_ADDITIONS= IWorkbenchActionConstants.MB_ADDITIONS;
	
	/** 
	 * Name of the action for re-establishing the state after the 
	 * most recent save operation. 
	 * Value: <code>"IWorkbenchActionConstants.REVERT"</code>
	 */
	static final String REVERT_TO_SAVED= REVERT;

}
