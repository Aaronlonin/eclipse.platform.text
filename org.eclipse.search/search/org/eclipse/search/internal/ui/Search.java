/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.search.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.search.internal.ui.util.ExceptionHandler;
import org.eclipse.search.ui.IContextMenuContributor;
import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultViewEntry;

class Search extends Object {
	private String fPageId;
	private String fDescription;
	private ImageDescriptor fImageDescriptor;
	private ILabelProvider fLabelProvider;
	private ArrayList fResults;
	private IAction fGotoMarkerAction;
	private IContextMenuContributor fContextMenuContributor;
	private IGroupByKeyComputer	fGroupByKeyComputer;
	private IRunnableWithProgress fOperation;


	public Search(String pageId, String description, ILabelProvider labelProvider, ImageDescriptor imageDescriptor, IAction gotoMarkerAction, IContextMenuContributor contextMenuContributor, IGroupByKeyComputer groupByKeyComputer, IRunnableWithProgress operation) {
		fPageId= pageId;
		fDescription= description;
		fImageDescriptor= imageDescriptor;
		fLabelProvider= labelProvider;
		fGotoMarkerAction= gotoMarkerAction;
		fContextMenuContributor= contextMenuContributor;
		fGroupByKeyComputer= groupByKeyComputer;
		fOperation= operation;
	}
	/**
	 * Returns the full description of the search.
	 * The description set by the client where
	 * {0} will be replaced by the match count.
	 */
	String getFullDescription() {
		if (fDescription == null)
			return ""; //$NON-NLS-1$

		// try to replace "{0}" with the match count
		int i= fDescription.lastIndexOf("{0}"); //$NON-NLS-1$
		if (i < 0)
			return fDescription;
		else
			return fDescription.substring(0, i) + getItemCount()+ fDescription.substring(Math.min(i + 3, fDescription.length()));
	}
	/**
	 * Returns a short description of the search.
	 * Cuts off after 30 characters and adds ...
	 * The description set by the client where
	 * {0} will be replaced by the match count.
	 */
	String getShortDescription() {
		if (fDescription == null)
			return ""; //$NON-NLS-1$
		String text= getFullDescription();
		int separatorPos= text.indexOf(" - "); //$NON-NLS-1$
		if (separatorPos < 1)
			return text.substring(0, Math.min(50, text.length())) + "..."; // use first 50 characters //$NON-NLS-1$
		if (separatorPos < 30)
			return text;	// don't cut
		if (text.charAt(0) == '"')  //$NON-NLS-1$
			return text.substring(0, Math.min(30, text.length())) + "...\" - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
		else
			return text.substring(0, Math.min(30, text.length())) + "... - " + text.substring(Math.min(separatorPos + 3, text.length())); //$NON-NLS-1$
	}

	/** Image used when search is displayed in a list */
	ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}

	int getItemCount() {
		int count= 0;
		Iterator iter= getResults().iterator();
		while (iter.hasNext())
			count += ((ISearchResultViewEntry)iter.next()).getMatchCount();
		return count;
	}

	List getResults() {
		if (fResults == null)
			return new ArrayList();
		return fResults;
	}

	ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}

	void searchAgain() {
		if (fOperation == null)
			return;
		Shell shell= SearchPlugin.getActiveWorkbenchShell();
		IWorkspaceDescription workspaceDesc= SearchPlugin.getWorkspace().getDescription();
		boolean isAutoBuilding= workspaceDesc.isAutoBuilding();
		if (isAutoBuilding)
			// disable auto-build during search operation
			SearchPlugin.setAutoBuilding(false);
		try {
			new ProgressMonitorDialog(shell).run(true, true, fOperation);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-2$ //$NON-NLS-1$
		} catch(InterruptedException e) {
		} finally {
			if (isAutoBuilding)
				// enable auto-building again
				SearchPlugin.setAutoBuilding(true);
		}
	}
	
	boolean isSameSearch(Search search) {
		return search != null && search.getOperation() == fOperation;
	}
	
	void backupMarkers() {
		Iterator iter= getResults().iterator();
		while (iter.hasNext()) {
			((SearchResultViewEntry)iter.next()).backupMarkers();
		}
	}

	String getPageId() {
		return fPageId;
	}
	
	IGroupByKeyComputer getGroupByKeyComputer() {
		return fGroupByKeyComputer;
	}

	IRunnableWithProgress getOperation() {
		return fOperation;
	}

	IAction getGotoMarkerAction() {
		return fGotoMarkerAction;
	}

	IContextMenuContributor getContextMenuContributor() {
		return fContextMenuContributor;
	}
	
	public void removeResults() {
		fResults= null;
	}
	
	void setResults(ArrayList results) {
		Assert.isNotNull(results);
		fResults= results;
	}
}

