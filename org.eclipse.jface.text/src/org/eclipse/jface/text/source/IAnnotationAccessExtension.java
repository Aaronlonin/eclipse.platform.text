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

package org.eclipse.jface.text.source;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;


/**
 * Extension interface for <code>IAnnotationAccess</code>. Provides the
 * following information and functions in addition to <code>IAnnotationAccess</code>.
 * <ul>
 * <li>a label for the type of a given annotation type</li>
 * <li>the presentation of a given annotation</li>
 * </ul>
 * 
 * @since 3.0
 */
public interface IAnnotationAccessExtension {
	
	/**
	 * The default annotation layer.
	 */
	static final int DEFAULT_LAYER= 0;

	/**
	 * Returns the label for the given annotation's type.
	 * 
	 * @param annotation the annotation
	 * @return the label the given annotation's type or <code>null</code> if no such label exists
	 */
	String getTypeLabel(Annotation annotation);
	
	/**
	 * Returns the layer for given annotation. Annotations are considered
	 * being located at layers and are considered being painted starting with
	 * layer 0 upwards. Thus an annotation at layer 5 will be drawn on top of
	 * all co-located annotations at the layers 4 - 0.
	 * 
	 * @param annotation the annotation
	 * @return the layer of the given annotation
	 */
	int getLayer(Annotation annotation);
	
	/**
	 * Draws a graphical representation of the given annotation within the given bounds.
	 * 
	 * @param annotation the given annotation
	 * @param GC the drawing GC
	 * @param canvas the canvas to draw on
	 * @param bounds the bounds inside the canvas to draw on
	 */
	void paint(Annotation annotation, GC gc, Canvas canvas, Rectangle bounds);
	
	/**
	 * Returns the text associated with the given annoattion.
	 * 
	 * @return the text associated with the given annotation or <code>null</code> if none
	 */
	String getText(Annotation annotation);
}
