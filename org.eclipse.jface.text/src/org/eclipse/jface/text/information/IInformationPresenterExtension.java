/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jface.text.information;

/**
 * Extension interface for <code>IInformationPresenter</code>.
 * Updates the information presenter to be aware of documents with multiple partitions.
 * 
 * @since 3.0
 */
public interface IInformationPresenterExtension {
	
	/**
	 * Returns the document partitioning this information presenter is using.
	 * 
	 * @return the document partitioning this information presenter is using
	 */
	String getDocumentPartitioning();
}
