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
package org.eclipse.core.filebuffers.annotations;



import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import org.eclipse.core.resources.IMarker;


/**
 * A marker updater is responsible for saving changes to markers.
 * Marker updaters either update markers of a specific types or 
 * any type. Also they either assume update responsibility for a 
 * specific set of marker attributes or any marker attribute.
 * Marker updater must be registered with an <code>AbstractMarkerAnnotationModel</code>.
 */
public interface IMarkerUpdater {
	
	/**
	 * Returns the marker type for which this updater is responsible. If
	 * the result is <code>null</code>, the updater assumes responsibility
	 * for any marker type.
	 * 
	 * @return the marker type or <code>null</code> for any marker type
	 */
	String getMarkerType();
	
	/**
	 * Returns the attributes for which this updater is responsible. If the
	 * result is <code>null</code>, the updater assumes responsibility for
	 * any attribute.
	 *
	 * @return the attributes or <code>null</code> for any attribute
	 */
	String[] getAttribute();
	
	/**
	 * Updates the given marker according to the position of the given document.
	 * If the given position is <code>null</code>, the marker is assumed to
	 * carry the correct positional information.
	 *
	 * @param marker the marker to be updated
	 * @param document the document into which the given position points
	 * @param position the current position of the marker inside the given document
	 * @return  <code>false</code> if the updater recognizes that the marker should be deleted
	 */
	boolean updateMarker(IMarker marker, IDocument document, Position position);
}
