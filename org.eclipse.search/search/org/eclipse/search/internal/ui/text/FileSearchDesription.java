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

/**
 * @author Thomas M�der
 *
 */
public class FileSearchDesription {
	private String fSearchString;
	private String fScopeDescription;

	public FileSearchDesription(String searchString, String scopeDescription) {
		super();
		fSearchString= searchString;
		fScopeDescription= scopeDescription;
	}

	public String getScopeDescription() {
		return fScopeDescription;
	}

	public String getSearchString() {
		return fSearchString;
	}

}
