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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * @since 3.0
 */
public abstract class _AbstractAnnotationPresentation implements IAnnotationAdapter, _IAnnotationPresentation {
	
	/**
	 * Convenience method for drawing an image aligned inside a rectangle.
	 *
	 * @param image the image to be drawn
	 * @param GC the drawing GC
	 * @param canvas the canvas on which to draw
	 * @param r the clipping rectangle
	 * @param halign the horizontal alignment of the image to be drawn
	 * @param valign the vertical alignment of the image to be drawn
	 */
	protected static void drawImage(Image image, GC gc, Canvas canvas, Rectangle r, int halign, int valign) {
		if (image != null) {
			
			Rectangle bounds= image.getBounds();
			
			int x= 0;
			switch(halign) {
				case SWT.LEFT:
					break;
				case SWT.CENTER:
					x= (r.width - bounds.width) / 2;
					break;
				case SWT.RIGHT:
					x= r.width - bounds.width;
					break;
			}
			
			int y= 0;
			switch (valign) {
				case SWT.TOP: {
					FontMetrics fontMetrics= gc.getFontMetrics();
					y= (fontMetrics.getHeight() - bounds.height)/2;
					break;
				}
				case SWT.CENTER:
					y= (r.height - bounds.height) / 2;
					break;
				case SWT.BOTTOM: {
					FontMetrics fontMetrics= gc.getFontMetrics();
					y= r.height - (fontMetrics.getHeight() + bounds.height)/2;
					break;
				}
			}
			
			gc.drawImage(image, r.x+x, r.y+y);
		}
	}
	
	/**
	 * Convenience method for drawing an image aligned inside a rectangle.
	 *
	 * @param image the image to be drawn
	 * @param GC the drawing GC
	 * @param canvas the canvas on which to draw
	 * @param r the clipping rectangle
	 * @param align the alignment of the image to be drawn
	 */
	protected static void drawImage(Image image, GC gc, Canvas canvas, Rectangle r, int align) {
		drawImage(image, gc, canvas, r, align, SWT.CENTER);
	}	
	
	
	private String fAnnotationType;
	private Object fAdapterKey;
	
	
	protected _AbstractAnnotationPresentation(String annotationType, Object adapterKey) {
		fAnnotationType= annotationType;
		fAdapterKey= adapterKey;
	}
	
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAdapter#annotationDataChanged(org.eclipse.jface.text.source.Annotation)
	 */
	public void annotationChanged(Annotation annotation) {
		if (!fAnnotationType.equals(annotation.getAnnotationType()))
			annotation.setAnnotationAdapter(fAdapterKey, null);
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAdapter#annotationDisposed(org.eclipse.jface.text.source.Annotation)
	 */
	public void annotationDisposed(Annotation annotation) {
	}
}
