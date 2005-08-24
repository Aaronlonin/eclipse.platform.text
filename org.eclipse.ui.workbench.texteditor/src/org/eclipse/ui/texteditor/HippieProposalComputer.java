/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.texteditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalComputer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.texteditor.HippieCompletionEngine;

/**
 * A completion proposal computer for hippie word completions. Clients
 * may instantiate. TODO API doc.
 * 
 * @since 3.2
 */
public final class HippieProposalComputer implements ICompletionProposalComputer {

	private static final class Proposal implements ICompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4 {

		private final String fString;
		private final String fPrefix;
		private final int fOffset;

		public Proposal(String string, String prefix, int offset) {
			fString= string;
			fPrefix= prefix;
			fOffset= offset;
		}

		public void apply(IDocument document) {
			apply(null, '\0', 0, fOffset);
		}

		public Point getSelection(IDocument document) {
			return new Point(fOffset + fString.length(), 0);
		}

		public String getAdditionalProposalInfo() {
			return null;
		}

		public String getDisplayString() {
			return fPrefix + fString;
		}

		public Image getImage() {
			return null;
		}

		public IContextInformation getContextInformation() {
			return null;
		}

		public void apply(IDocument document, char trigger, int offset) {
			try {
				String replacement= fString.substring(offset - fOffset);
				document.replace(offset, 0, replacement);
			} catch (BadLocationException x) {
				// TODO Auto-generated catch block
				x.printStackTrace();
			}
		}

		public boolean isValidFor(IDocument document, int offset) {
			return validate(document, offset, null);
		}

		public char[] getTriggerCharacters() {
			return null;
		}

		public int getContextInformationPosition() {
			return 0;
		}

		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			apply(viewer.getDocument(), trigger, offset);
		}

		public void selected(ITextViewer viewer, boolean smartToggle) {
		}

		public void unselected(ITextViewer viewer) {
		}

		public boolean validate(IDocument document, int offset, DocumentEvent event) {
			try {
				int prefixStart= fOffset - fPrefix.length();
				return offset >= fOffset && offset < fOffset + fString.length() && document.get(prefixStart, offset - (prefixStart)).equals((fPrefix + fString).substring(0, offset - prefixStart));
			} catch (BadLocationException x) {
				return false;
			} 
		}

		public IInformationControlCreator getInformationControlCreator() {
			return null;
		}

		public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
			return fPrefix + fString;
		}

		public int getPrefixCompletionStart(IDocument document, int completionOffset) {
			return fOffset - fPrefix.length();
		}

		public boolean isAutoInsertable() {
			return true;
		}

	}

	private final HippieCompletionEngine fEngine= new HippieCompletionEngine();

	/**
	 * Creates a new hippie completion proposal computer.
	 */
	public HippieProposalComputer() {
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeCompletionProposals(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		try {
			String prefix= context.computeIdentifierPrefix().toString();
			if (prefix.length() == 0)
				return Collections.EMPTY_LIST;
			
			String[] suggestions= getSuggestions(context.getViewer(), context.getInvocationOffset(), prefix);
			
			List result= new ArrayList();
			for (int i= 0; i < suggestions.length; i++) {
				String string= suggestions[i];
				if (string.length() > 0)
					result.add(createProposal(string, prefix, context.getInvocationOffset()));
			}
			
			return result;
			
		} catch (BadLocationException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	private ICompletionProposal createProposal(String string, String prefix, int offset) {
		return new Proposal(string, prefix, offset);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeContextInformation(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		// no context informations for hippie completions
		return Collections.EMPTY_LIST;
	}
	

	/**
	 * Return the list of suggestions from the current document. First the
	 * document is searched backwards from the caret position and then forwards.
	 * 
	 * @param offset 
	 * @param viewer 
	 * @param prefix the completion prefix
	 * @return all possible completions that were found in the current document
	 * @throws BadLocationException if accessing the document fails
	 */
	private ArrayList createSuggestionsFromOpenDocument(ITextViewer viewer, int offset, String prefix) throws BadLocationException {
		IDocument document= viewer.getDocument();
		ArrayList completions= new ArrayList();
		completions.addAll(fEngine.getCompletionsBackwards(document, prefix, offset));
		completions.addAll(fEngine.getCompletionsForward(document, prefix, offset));

		return completions;
	}

	/**
	 * Create the array of suggestions. It scans all open text editors and
	 * prefers suggestions from the currently open editor. It also adds the
	 * empty suggestion at the end.
	 * 
	 * @param viewer 
	 * @param offset 
	 * @param prefix the prefix to search for
	 * @return the list of all possible suggestions in the currently open
	 *         editors
	 * @throws BadLocationException if accessing the current document fails
	 */
	private String[] getSuggestions(ITextViewer viewer, int offset, String prefix) throws BadLocationException {

		ArrayList suggestions= createSuggestionsFromOpenDocument(viewer, offset, prefix);
		IDocument currentDocument= viewer.getDocument();

		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorReference editorReferences[]= window.getActivePage().getEditorReferences();

		for (int i= 0; i < editorReferences.length; i++) {
			IEditorPart editor= editorReferences[i].getEditor(false); // don't create!
			if (editor instanceof ITextEditor) {
				ITextEditor textEditor= (ITextEditor) editor;
				IEditorInput input= textEditor.getEditorInput();
				IDocument doc= textEditor.getDocumentProvider().getDocument(input);
				if (!currentDocument.equals(doc))
					suggestions.addAll(fEngine.getCompletions(doc, prefix));
			}
		}
		// add the empty suggestion
		suggestions.add(""); //$NON-NLS-1$

		List uniqueSuggestions= fEngine.makeUnique(suggestions);

		return (String[]) uniqueSuggestions.toArray(new String[0]);
	}
}
