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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.TextEditorAction;


/**
 * Action group for encoding actions.
 * @since 2.0
 */
public class EncodingActionGroup extends ActionGroup {
	
	/**
	 * Action for setting the encoding of the editor to the value this action has 
	 * been initialized with.
	 */
	static class PredefinedEncodingAction extends TextEditorAction {
		
		/** The target encoding of this action. */
		private String fEncoding;
		/** The action label. */
		private String fLabel;
		/** Indicates whether the target encoding is the default encoding. */
		private boolean fIsDefault;
		
		/**
		 * Creates a new action for the given specification.
		 * 
		 * @param bundle the resource bundle
		 * @param prefix the prefix for lookups from the resource bundle
		 * @param encoding the target encoding
		 * @param editor the target editor
		 */
		public PredefinedEncodingAction(ResourceBundle bundle, String prefix, String encoding, ITextEditor editor) {
       		super(bundle, prefix, editor);
			fEncoding= encoding;
			if (prefix == null)
				setText(encoding);
			fLabel= getText();
		}
		
		/**
		 * Creates a new action for the given specification.
		 * 
		 * @param bundle the resource bundle
		 * @param encoding the target encoding
		 * @param editor the target editor
		 */
		public PredefinedEncodingAction(ResourceBundle bundle, String encoding, ITextEditor editor) {
       		super(bundle, null, editor);
			fEncoding= encoding;
			setText(encoding);
			fLabel= getText();
		}
		
		/**
		 * Returns the encoding support of the action's editor.
		 * 
		 * @return the encoding support of the action's editor or <code>null</code> if none
		 */
		private IEncodingSupport getEncodingSupport() {
			ITextEditor editor= getTextEditor();
			if (editor != null)
				return (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);
			return null;
		}
		
		/*
		 * @see IAction#run()
		 */
		public void run() {
			IEncodingSupport s= getEncodingSupport();
			if (s != null)
				s.setEncoding(fIsDefault ? null : fEncoding);
		}
		
		/**
		 * Returns the encoding currently used in the given editor.
		 * 
		 * @param editor the editor
		 * @return the encoding currently used in the given editor or <code>null</code> if no encoding support is installed
		 */		
		private String getEncoding(ITextEditor editor) {
			IEncodingSupport s= getEncodingSupport();
			if (s != null)
				return s.getEncoding();
			return null;
		}
		
		/**
		 * Returns the default encoding for the given editor.
		 * 
		 * @param editor the editor
		 * @return the default encoding for the given editor or <code>null</code> if no encoding support is installed
		 */
		private String getDefaultEncoding(ITextEditor editor) {
			IEncodingSupport s= getEncodingSupport();
			if (s != null)
				return s.getDefaultEncoding();
			return null;
		}
		
		/*
		 * @see IUpdate#update()
		 */
		public void update() {
			
			if (fEncoding == null) {
				setEnabled(false);
				return;
			}
			
			ITextEditor editor= getTextEditor();
			if (editor == null) {
				setEnabled(false);
				return;
			}
			
			// update label
			String encoding= getDefaultEncoding(editor);
			if (encoding != null) {
				fIsDefault= fEncoding.equals(encoding);
				setText(fIsDefault ? fLabel + DEFAULT_SUFFIX : fLabel);
			}
			
			// update enable state
			if (editor.isDirty())
				setEnabled(false);
			else
				setEnabled(true);

			// update checked state
			String current= getEncoding(editor);
			if (fIsDefault)
				setChecked(current == null);
			else
				setChecked(fEncoding.equals(current));
		}
	}
	
	/**
	 * Sets the encoding of an  editor to the value that has interactively been defined.
	 */
	static class CustomEncodingAction extends TextEditorAction {
		
		
		/*
		 * @see org.eclipse.ui.texteditor.TextEditorAction#TextEditorAction(ResourceBundle, String, ITextEditor)
		 */
		protected CustomEncodingAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
			super(bundle, prefix, editor);
		}
		
		/*
		 * @see IUpdate#update()
		 */
		public void update() {
			ITextEditor editor= getTextEditor();
			setEnabled(editor != null && !editor.isDirty());
		}

		/*
		 * @see IAction#run()
		 */
		public void run() {
			
			ITextEditor editor= getTextEditor();
			if (editor == null)
				return;
			
			IEncodingSupport encodingSupport= (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);
			if (encodingSupport == null)
				return;
			
			String title= TextEditorMessages.getString("Editor.ConvertEncoding.Custom.dialog.title"); //$NON-NLS-1$
			String message= TextEditorMessages.getString("Editor.ConvertEncoding.Custom.dialog.message");  //$NON-NLS-1$
			IInputValidator inputValidator = new IInputValidator() {
				public String isValid(String newText) { 
					return (newText == null || newText.length() == 0) ? " " : null; //$NON-NLS-1$
				}
			};

			String initialValue= encodingSupport.getEncoding();
			if (initialValue == null)
				initialValue= encodingSupport.getDefaultEncoding();
			if (initialValue == null)
				initialValue= ""; //$NON-NLS-1$
			
			InputDialog d= new InputDialog(editor.getSite().getShell(), title, message, initialValue, inputValidator); //$NON-NLS-1$
			if (d.open() == Window.OK)
				encodingSupport.setEncoding(d.getValue());
		}
	}
	
		
	/** Suffix added to the default encoding action. */
	private static final String DEFAULT_SUFFIX= " " + TextEditorMessages.getString("Editor.ConvertEncoding.default_suffix"); //$NON-NLS-1$ //$NON-NLS-2$

	/** List of predefined encodings. */
	private static final String[][] ENCODINGS;
	
	/** The default encoding. */
	private static final String SYSTEM_ENCODING;
	
	/**
	 * Initializer: computes the set of predefined encoding actions.
	 */
	static {
		
		String[][] encodings= {
			{ IEncodingActionsConstants.US_ASCII, IEncodingActionsHelpContextIds.US_ASCII, IEncodingActionsDefinitionIds.US_ASCII },
			{ IEncodingActionsConstants.ISO_8859_1, IEncodingActionsHelpContextIds.ISO_8859_1, IEncodingActionsDefinitionIds.ISO_8859_1 },
			{ IEncodingActionsConstants.UTF_8, IEncodingActionsHelpContextIds.UTF_8, IEncodingActionsDefinitionIds.UTF_8 },
			{ IEncodingActionsConstants.UTF_16BE, IEncodingActionsHelpContextIds.UTF_16BE, IEncodingActionsDefinitionIds.UTF_16BE },
			{ IEncodingActionsConstants.UTF_16LE, IEncodingActionsHelpContextIds.UTF_16LE, IEncodingActionsDefinitionIds.UTF_16LE },
			{ IEncodingActionsConstants.UTF_16, IEncodingActionsHelpContextIds.UTF_16, IEncodingActionsDefinitionIds.UTF_16 }
		};	
			
		String system= System.getProperty("file.encoding"); //$NON-NLS-1$
		if (system != null) { 
			
			int i;
			for (i= 0; i < encodings.length; i++) {
				if (encodings[i][0].equals(system))
					break;
			}
			
			if (i != encodings.length) {
				// bring default in first position
				String[] s= encodings[i];
				encodings[i]= encodings[0];
				encodings[0]= s;
				// forget default encoding as it's already in the list
				system= null;
			}
		}
		
		SYSTEM_ENCODING= system;
		ENCODINGS= encodings;
	}
	
	
	
	/** List of encoding actions of this group. */
	private List fRetargetActions= new ArrayList();
	
	/**
	 * Creates a new encoding action group for an action bar contributor.
	 */
	public EncodingActionGroup() {
		
		ResourceBundle b= TextEditorMessages.getResourceBundle();
		
		if (SYSTEM_ENCODING != null)
			fRetargetActions.add(new RetargetTextEditorAction(b, "Editor.ConvertEncoding.System.", IEncodingActionsConstants.SYSTEM, IAction.AS_RADIO_BUTTON)); //$NON-NLS-1$
		
		for (int i= 0; i < ENCODINGS.length; i++)
			fRetargetActions.add(new RetargetTextEditorAction(b, "Editor.ConvertEncoding." + ENCODINGS[i][0] + ".", ENCODINGS[i][0], IAction.AS_RADIO_BUTTON)); //$NON-NLS-1$ //$NON-NLS-2$
		
		fRetargetActions.add(new RetargetTextEditorAction(b, "Editor.ConvertEncoding.Custom.", IEncodingActionsConstants.CUSTOM, IAction.AS_PUSH_BUTTON)); //$NON-NLS-1$
	}
	
	/*
	 * @see ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		IMenuManager menuManager= actionBars.getMenuManager(); 
		IMenuManager editMenu= menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			MenuManager subMenu= new MenuManager(TextEditorMessages.getString("Editor.ConvertEncoding.submenu.label"));  //$NON-NLS-1$

			Iterator e= fRetargetActions.iterator();
			while (e.hasNext())
				subMenu.add((IAction) e.next());
				
			editMenu.add(subMenu);
		}
	}
	
	/**
	 * Retargets this action group to the given editor.
	 * 
	 * @param editor the text editor to which the group should be retargeted
	 */
	public void retarget(ITextEditor editor) {
		Iterator e= fRetargetActions.iterator();
		while (e.hasNext()) {
			RetargetTextEditorAction a= (RetargetTextEditorAction) e.next();
			a.setAction(editor == null ? null : editor.getAction(a.getId()));
		}
	}
	
	
	//------------------------------------------------------------------------------------------
		
	
	/** Text editor this group is associated with. */
	private ITextEditor fTextEditor;
	
	/**
	 * Creates a new encoding action group for the given editor.
	 * 
	 * @param editor the text editor
	 */
	public EncodingActionGroup(ITextEditor editor) {
		
		fTextEditor= editor;
		ResourceBundle b= TextEditorMessages.getResourceBundle();
		
		ResourceAction a; 
		if (SYSTEM_ENCODING != null) {
			a= new PredefinedEncodingAction(b, SYSTEM_ENCODING, editor);
			a.setHelpContextId(IEncodingActionsHelpContextIds.SYSTEM);
			a.setActionDefinitionId(IEncodingActionsDefinitionIds.SYSTEM);
			editor.setAction(IEncodingActionsConstants.SYSTEM, a);
		}
		
		for (int i= 0; i < ENCODINGS.length; i++) {
			a= new PredefinedEncodingAction(b, "Editor.ConvertEncoding." + ENCODINGS[i][0] + ".", ENCODINGS[i][0], editor); //$NON-NLS-1$ //$NON-NLS-2$
			a.setHelpContextId( ENCODINGS[i][1]);
			a.setActionDefinitionId( ENCODINGS[i][2]);
			editor.setAction(ENCODINGS[i][0], a);
		}

		a= new CustomEncodingAction(b, "Editor.ConvertEncoding." + IEncodingActionsConstants.CUSTOM + ".", editor); //$NON-NLS-1$ //$NON-NLS-2$
		a.setHelpContextId(IEncodingActionsHelpContextIds.CUSTOM);
		a.setActionDefinitionId(IEncodingActionsDefinitionIds.CUSTOM);
		editor.setAction(IEncodingActionsConstants.CUSTOM, a);
	}
		
	/**
	 * Updates all actions of this action group.
	 */
	public void update() {
		
		IAction a= fTextEditor.getAction(IEncodingActionsConstants.SYSTEM);
		if (a instanceof IUpdate)
			((IUpdate) a).update();
			
		for (int i= 0; i < ENCODINGS.length; i++) {
			a= fTextEditor.getAction(ENCODINGS[i][0]);
			if (a instanceof IUpdate)
				((IUpdate) a).update();
		}
		
		a= fTextEditor.getAction(IEncodingActionsConstants.CUSTOM);
		if (a instanceof IUpdate)
			((IUpdate) a).update();
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {		
		if (fTextEditor != null) {
			fTextEditor.setAction(IEncodingActionsConstants.SYSTEM, null);
			for (int i= 0; i < ENCODINGS.length; i++)
				fTextEditor.setAction(ENCODINGS[i][0], null);
			fTextEditor.setAction(IEncodingActionsConstants.CUSTOM, null);
			
			fTextEditor= null;
		}
	}
}
