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




import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;


/**
 * A vertical ruler which is connected to a text viewer. Single column standard 
 * implementation of <code>IVerticalRuler</code>. The same can be achieved by 
 * using <code>CompositeRuler</code> configured with an <code>AnnotationRulerColumn</code>.
 * Clients may use this class as is.
 *
 * @see ITextViewer
 */
public final class VerticalRuler implements IVerticalRuler, IVerticalRulerExtension {

	/**
	 * Internal listener class.
	 */
	class InternalListener implements IViewportListener, IAnnotationModelListener, ITextListener {
		
		/*
		 * @see IViewportListener#viewportChanged(int)
		 */
		public void viewportChanged(int verticalPosition) {
			if (verticalPosition != fScrollPos)
				redraw();
		}
		
		/*
		 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
		 */
		public void modelChanged(IAnnotationModel model) {
			update();
		}
		
		/*
		 * @see ITextListener#textChanged(TextEvent)
		 */
		public void textChanged(TextEvent e) {
			if (fTextViewer != null && e.getViewerRedrawState())
				redraw();
		}
	}
	
	/** The vertical ruler's text viewer */
	private ITextViewer fTextViewer;
	/** The ruler's canvas */
	private Canvas fCanvas;
	/** The vertical ruler's model */
	private IAnnotationModel fModel;
	/** Cache for the actual scroll position in pixels */
	private int fScrollPos;
	/** The drawable for double buffering */
	private Image fBuffer;
	/** The line of the last mouse button activity */
	private int fLastMouseButtonActivityLine= -1;
	/** The internal listener */
	private InternalListener fInternalListener= new InternalListener();
	/** The width of this vertical ruler */
	private int fWidth;
	/**
	 * The annotation access
	 * @since 3.0
	 */
	private IAnnotationAccess fAnnotationAccess;
	
	/**
	 * Constructs a vertical ruler with the given width.
	 *
	 * @param width the width of the vertical ruler
	 */
	public VerticalRuler(int width) {
		fWidth= width;
	}
	
	/**
	 * Sets the annotation access for this vertical ruler.
	 * 
	 * @param access the annotation access
	 * @since 3.0
	 */
	public void setAnnotationAccess(IAnnotationAccess access) {
		fAnnotationAccess= access;
	}
	
	/*
	 * @see IVerticalRuler#getControl()
	 */
	public Control getControl() {
		return fCanvas;
	}
	
	/*
	 * @see IVerticalRuler#createControl(Composite, ITextViewer)
	 */
	public Control createControl(Composite parent, ITextViewer textViewer) {
		
		fTextViewer= textViewer;
		
		fCanvas= new Canvas(parent, SWT.NO_BACKGROUND);
		
		fCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				if (fTextViewer != null && fAnnotationAccess != null)
					doubleBufferPaint(event.gc);
			}
		});
		
		fCanvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				handleDispose();
				fTextViewer= null;		
			}
		});
		
		fCanvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent event) {
			}
			
			public void mouseDown(MouseEvent event) {
				fLastMouseButtonActivityLine= toDocumentLineNumber(event.y);
			}
			
			public void mouseDoubleClick(MouseEvent event) {
				fLastMouseButtonActivityLine= toDocumentLineNumber(event.y);
			}
		});
		
		if (fTextViewer != null) {
			fTextViewer.addViewportListener(fInternalListener);
			fTextViewer.addTextListener(fInternalListener);
		}
		
		return fCanvas;
	}
	
	/**
	 * Disposes the ruler's resources.
	 */
	private void handleDispose() {
		
		if (fTextViewer != null) {
			fTextViewer.removeViewportListener(fInternalListener);
			fTextViewer.removeTextListener(fInternalListener);
			fTextViewer= null;
		}
		
		if (fModel != null)
			fModel.removeAnnotationModelListener(fInternalListener);
		
		if (fBuffer != null) {
			fBuffer.dispose();
			fBuffer= null;
		}
	}
	
	
	/**
	 * Double buffer drawing.
	 * 
	 * @param dest the gc to draw into
	 */
	private void doubleBufferPaint(GC dest) {
		
		Point size= fCanvas.getSize();
		
		if (size.x <= 0 || size.y <= 0)
			return;
		
		if (fBuffer != null) {
			Rectangle r= fBuffer.getBounds();
			if (r.width != size.x || r.height != size.y) {
				fBuffer.dispose();
				fBuffer= null;
			}
		}
		if (fBuffer == null)
			fBuffer= new Image(fCanvas.getDisplay(), size.x, size.y);
			
		GC gc= new GC(fBuffer);
		gc.setFont(fTextViewer.getTextWidget().getFont());
		try {
			gc.setBackground(fCanvas.getBackground());
			gc.fillRectangle(0, 0, size.x, size.y);
			
			if (fTextViewer instanceof ITextViewerExtension3)
				doPaint1(gc);
			else
				doPaint(gc);
				
		} finally {
			gc.dispose();
		}
		
		dest.drawImage(fBuffer, 0, 0);
	}
	
	/**
	 * Returns the document offset of the upper left corner of the
	 * widgets viewport, possibly including partially visible lines.
	 * 
	 * @return the document offset of the upper left corner including partially visible lines
	 * @since 2.0
	 */
	private int getInclusiveTopIndexStartOffset() {
		
		StyledText textWidget= fTextViewer.getTextWidget();
		if (textWidget != null && !textWidget.isDisposed()) {	
			int top= fTextViewer.getTopIndex();
			if ((textWidget.getTopPixel() % textWidget.getLineHeight()) != 0)
				top--;
			try {
				IDocument document= fTextViewer.getDocument();
				return document.getLineOffset(top);
			} catch (BadLocationException ex) {
			}
		}
		
		return -1;
	}
	
	/**
	 * Draws the vertical ruler w/o drawing the Canvas background.
	 * 
	 * @param gc  the gc to draw into
	 */
	protected void doPaint(GC gc) {
	
		if (fModel == null || fTextViewer == null)
			return;
		
		if ( !(fAnnotationAccess instanceof IAnnotationAccessExtension))
			return;
		IAnnotationAccessExtension annotationAccessExtension= (IAnnotationAccessExtension) fAnnotationAccess;
		
		
		StyledText styledText= fTextViewer.getTextWidget();
		IDocument doc= fTextViewer.getDocument();

		int topLeft= getInclusiveTopIndexStartOffset();
		int bottomRight= fTextViewer.getBottomIndexEndOffset();
		int viewPort= bottomRight - topLeft;
		
		Point d= fCanvas.getSize();
		fScrollPos= styledText.getTopPixel();
		int lineheight= styledText.getLineHeight();
			
		int shift= fTextViewer.getTopInset();
		
		int topLine= -1, bottomLine= -1;
		try {
			IRegion region= fTextViewer.getVisibleRegion();
			topLine= doc.getLineOfOffset(region.getOffset());
			bottomLine= doc.getLineOfOffset(region.getOffset() + region.getLength());
		} catch (BadLocationException x) {
			return;
		}
				
		// draw Annotations
		Rectangle r= new Rectangle(0, 0, 0, 0);
		int maxLayer= 1;	// loop at least once thru layers.
		
		for (int layer= 0; layer < maxLayer; layer++) {
			Iterator iter= fModel.getAnnotationIterator();
			while (iter.hasNext()) {
				Annotation annotation= (Annotation) iter.next();
				
				int lay= annotationAccessExtension.getLayer(annotation);
				maxLayer= Math.max(maxLayer, lay+1);	// dynamically update layer maximum
				if (lay != layer)	// wrong layer: skip annotation
					continue;
				
				Position position= fModel.getPosition(annotation);
				if (position == null)
					continue;
					
				if (!position.overlapsWith(topLeft, viewPort))
					continue;
					
				try {
					
					int offset= position.getOffset();
					int length= position.getLength();
					
					int startLine= doc.getLineOfOffset(offset);
					if (startLine < topLine)
						startLine= topLine;
					
					int endLine= startLine;
					if (length > 0)
						endLine= doc.getLineOfOffset(offset + length - 1);
					if (endLine > bottomLine)
						endLine= bottomLine;
					
					startLine -= topLine;
					endLine -= topLine;
					
					r.x= 0;
					r.y= (startLine * lineheight) - fScrollPos + shift;
					r.width= d.x;
					int lines= endLine - startLine;
					if (lines < 0)
						lines= -lines;
					r.height= (lines+1) * lineheight;
					
					if (r.y < d.y)  // annotation within visible area
						annotationAccessExtension.paint(annotation, gc, fCanvas, r);
					
				} catch (BadLocationException e) {
				}
			}
		}
	}
	
	/**
	 * Draws the vertical ruler w/o drawing the Canvas background. Uses
	 * <code>ITExtViewerExtension3</code> for its implementation. Will replace
	 * <code>doPaint(GC)</code>.
	 * 
	 * @param gc  the gc to draw into
	 */
	protected void doPaint1(GC gc) {

		if (fModel == null || fTextViewer == null)
			return;

		if ( !(fAnnotationAccess instanceof IAnnotationAccessExtension))
			return;
		IAnnotationAccessExtension annotationAccessExtension= (IAnnotationAccessExtension) fAnnotationAccess;
		
		ITextViewerExtension3 extension= (ITextViewerExtension3) fTextViewer;
		StyledText textWidget= fTextViewer.getTextWidget();
		
		fScrollPos= textWidget.getTopPixel();
		int lineheight= textWidget.getLineHeight();
		Point dimension= fCanvas.getSize();
		int shift= fTextViewer.getTopInset();

		// draw Annotations
		Rectangle r= new Rectangle(0, 0, 0, 0);
		int maxLayer= 1;	// loop at least once thru layers.

		for (int layer= 0; layer < maxLayer; layer++) {
			Iterator iter= fModel.getAnnotationIterator();
			while (iter.hasNext()) {
				Annotation annotation= (Annotation) iter.next();

				int lay= annotationAccessExtension.getLayer(annotation);
				maxLayer= Math.max(maxLayer, lay+1);	// dynamically update layer maximum
				if (lay != layer)	// wrong layer: skip annotation
					continue;

				Position position= fModel.getPosition(annotation);
				if (position == null)
					continue;

				IRegion widgetRegion= extension.modelRange2WidgetRange(new Region(position.getOffset(), position.getLength()));
				if (widgetRegion == null)
					continue;

				int startLine= extension.widgetLineOfWidgetOffset(widgetRegion.getOffset());
				if (startLine == -1)
					continue;

				int endLine= extension.widgetLineOfWidgetOffset(widgetRegion.getOffset() + Math.max(widgetRegion.getLength() -1, 0));
				if (endLine == -1)
					continue;

				r.x= 0;
				r.y= (startLine * lineheight) - fScrollPos + shift;
				r.width= dimension.x;
				int lines= endLine - startLine;
				if (lines < 0)
					lines= -lines;
				r.height= (lines+1) * lineheight;

				if (r.y < dimension.y)  // annotation within visible area
					annotationAccessExtension.paint(annotation, gc, fCanvas, r);
			}
		}
	}
		
	/**
	 * Thread-safe implementation.
	 * Can be called from any thread.
	 */
	/*
	 * @see IVerticalRuler#update()
	 */
	public void update() {
		if (fCanvas != null && !fCanvas.isDisposed()) {
			Display d= fCanvas.getDisplay();
			if (d != null) {
				d.asyncExec(new Runnable() {
					public void run() {
						redraw();
					}
				});
			}	
		}
	}
		
	/**
	 * Redraws the vertical ruler.
	 */
	private void redraw() {
		if (fCanvas != null && !fCanvas.isDisposed() && fAnnotationAccess != null) {
			GC gc= new GC(fCanvas);
			doubleBufferPaint(gc);
			gc.dispose();
		}
	}
	
	/*
	 * @see IVerticalRuler#setModel(IAnnotationModel)
	 */
	public void setModel(IAnnotationModel model) {
		if (model != fModel) {
			
			if (fModel != null)
				fModel.removeAnnotationModelListener(fInternalListener);
			
			fModel= model;
			
			if (fModel != null)
				fModel.addAnnotationModelListener(fInternalListener);
			
			update();
		}
	}
		
	/*
	 * @see IVerticalRuler#getModel()
	 */
	public IAnnotationModel getModel() {
		return fModel;
	}
	
	/*
	 * @see IVerticalRulerInfo#getWidth()
	 */
	public int getWidth() {
		return fWidth;
	}
	
	/*
	 * @see IVerticalRulerInfo#getLineOfLastMouseButtonActivity()
	 */
	public int getLineOfLastMouseButtonActivity() {
		return fLastMouseButtonActivityLine;
	}
	
	/*
	 * @see IVerticalRulerInfo#toDocumentLineNumber(int)
	 */
	public int toDocumentLineNumber(int y_coordinate) {
		
		if (fTextViewer == null)
			return -1;
			
		StyledText text= fTextViewer.getTextWidget();
		int line= ((y_coordinate + fScrollPos) / text.getLineHeight());
		return widgetLine2ModelLine(fTextViewer, line);
	}
	
	/**
	 * Returns the line of the viewer's document that corresponds to the given widget line.
	 * 
	 * @param viewer the viewer
	 * @param widgetLine the widget line
	 * @return the corresponding line of the viewer's document
	 * @since 2.1
	 */
	protected final static int widgetLine2ModelLine(ITextViewer viewer, int widgetLine) {
		
		if (viewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
			return extension.widgetlLine2ModelLine(widgetLine);
		}
		
		try {
			IRegion r= viewer.getVisibleRegion();
			IDocument d= viewer.getDocument();
			return widgetLine += d.getLineOfOffset(r.getOffset());
		} catch (BadLocationException x) {
		}
		return widgetLine;
	}
	
	/*
	 * @see IVerticalRulerExtension#setFont(Font)
	 * @since 2.0
	 */
	public void setFont(Font font) {
	}

	/*
	 * @see IVerticalRulerExtension#setLocationOfLastMouseButtonActivity(int, int)
	 * @since 2.0
	 */
	public void setLocationOfLastMouseButtonActivity(int x, int y) {
		fLastMouseButtonActivityLine= toDocumentLineNumber(y);
	}
	
	/**
	 * @deprecated will be removed
	 * @since 2.0
	 */
	public void addMouseListener(MouseListener listener) {
		if (fCanvas != null && !fCanvas.isDisposed())
			fCanvas.addMouseListener(listener);
	}

	/**
	 * @deprecated will be removed
	 * @since 2.0
	 */
	public void removeMouseListener(MouseListener listener) {
		if (fCanvas != null && !fCanvas.isDisposed())
			fCanvas.removeMouseListener(listener);
	}
}
