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
package org.eclipse.jface.text.contentassist;



import org.eclipse.jface.text.ITextViewer;


/**
 * A default implementation of the <code>IContextInfomationValidator</code> interface.
 * This implementation determines whether the information is valid by asking the content 
 * assist processor for all  context information objects for the current position. If the 
 * currently displayed information is in the result set, the context information is 
 * considered valid.
 */
public final class ContextInformationValidator implements IContextInformationValidator, IContextInformationValidatorExtension {
	
	/** The content assist processor */
	private IContentAssistProcessor fProcessor;
	/** The context information to be validated */
	private IContextInformation fContextInformation;
	/** The associated text viewer */
	private ITextViewer fViewer;
	/**
	 * The content assist subject.
	 * 
	 * @since 3.0
	 */
	private IContentAssistSubject fContentAssistSubject;

	/**
	 * Creates a new context information validator which is ready to be installed on
	 * a particular context information.
	 * 
	 * @param processor the processor to be used for validation
	 */
	public ContextInformationValidator(IContentAssistProcessor processor) {
		fProcessor= processor;
	}

	/*
	 * @see IContextInformationValidator#install(IContextInformation, ITextViewer, int)
	 */
	public void install(IContextInformation contextInformation, ITextViewer viewer, int position) {
		fContextInformation= contextInformation;
		fViewer= viewer;
	}

	public void install(IContextInformation contextInformation, IContentAssistSubject contentAssistSubject, int position) {
		fContextInformation= contextInformation;
		fContentAssistSubject= contentAssistSubject;
	}

	/*
	 * @see IContentAssistTipCloser#isContextInformationValid(int)
	 */
	public boolean isContextInformationValid(int position) {
		IContextInformation[] infos= null;
		if (fContentAssistSubject != null) {
			if (fProcessor instanceof IContentAssistProcessorExtension)
			infos= ((IContentAssistProcessorExtension)fProcessor).computeContextInformation(fContentAssistSubject, position);
		} else
			infos= fProcessor.computeContextInformation(fViewer, position);
		if (infos != null && infos.length > 0) {
			for (int i= 0; i < infos.length; i++)
				if (fContextInformation.equals(infos[i]))
					return true;
		}
		return false;
	}
}
