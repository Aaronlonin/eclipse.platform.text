/*******************************************************************************
 * Copyright (c) 2006 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Markus Schorn - initial API and implementation 
 *******************************************************************************/

package org.eclipse.search2.internal.ui.text2;

import org.eclipse.search.internal.core.text.FileNamePatternSearchScope;

import org.eclipse.search2.internal.ui.SearchMessages;

public class FindInWorkspaceActionDelegate extends FindInRecentScopeActionDelegate {

	public FindInWorkspaceActionDelegate() {
		super(SearchMessages.FindInWorkspaceActionDelegate_text);
	}

	protected boolean modifyQuery(RetrieverQuery query) {
		if (super.modifyQuery(query)) {
			query.setSearchScope(new WorkspaceScopeDescription());
			return true;
		}
		return false;
	}
	
	protected FileNamePatternSearchScope getOldSearchScope(boolean includeDerived) {
		return FileNamePatternSearchScope.newWorkspaceScope(includeDerived);
	}
}
