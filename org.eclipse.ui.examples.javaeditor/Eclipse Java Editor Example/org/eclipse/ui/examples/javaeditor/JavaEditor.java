package org.eclipse.ui.examples.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ResourceBundle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Java specific text editor.
 */
public class JavaEditor extends TextEditor {

	/** The outline page */
	private JavaContentOutlinePage fOutlinePage;

	/**
	 * Default constructor.
	 */
	public JavaEditor() {
		super();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method extend the 
	 * actions to add those specific to the receiver
	 */
	protected void createActions() {
		super.createActions();
		setAction("ContentAssistProposal", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS)); //$NON-NLS-1$ //$NON-NLS-2$
		setAction("ContentAssistTip", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION)); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * disposal actions required by the java editor.
	 */
	public void dispose() {
		JavaEditorEnvironment.disconnect(this);
		if (fOutlinePage != null)
			fOutlinePage.setInput(null);
		super.dispose();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * revert behavior required by the java editor.
	 */
	public void doRevertToSaved() {
		super.doRevertToSaved();
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save behavior required by the java editor.
	 */
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save as behavior required by the java editor.
	 */
	public void doSaveAs() {
		super.doSaveAs();
		if (fOutlinePage != null)
			fOutlinePage.update();
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs sets the 
	 * input of the outline page after AbstractTextEditor has set input.
	 */ 
	public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(input);
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method adds any 
	 * JavaEditor specific entries.
	 */ 
	public void editorContextMenuAboutToShow(MenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal"); //$NON-NLS-1$
		addAction(menu, "ContentAssistTip"); //$NON-NLS-1$
	}
	
	/** The <code>JavaEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs gets
	 * the java content outline page if request is for a an 
	 * outline page.
	 */ 
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null) {
				fOutlinePage= new JavaContentOutlinePage(getDocumentProvider(), this);
				if (getEditorInput() != null)
					fOutlinePage.setInput(getEditorInput());
			}
			return fOutlinePage;
		}
		return super.getAdapter(required);
	}
		
	/* (non-Javadoc)
	 * Method declared on AbstractTextEditor
	 */
	protected void initializeEditor() {

		JavaEditorEnvironment.connect(this);

		setSourceViewerConfiguration(new JavaSourceViewerConfiguration());
		setRangeIndicator(new DefaultRangeIndicator());
		setEditorContextMenuId("#JavaEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#JavaRulerContext"); //$NON-NLS-1$
	}
}
