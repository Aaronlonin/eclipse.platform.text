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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.text.AbstractHoverInformationControlManager;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension3;


/**
 * This manager controls the layout, content, and visibility of an information
 * control in reaction to mouse hover events issued by the vertical ruler of a
 * source viewer.
 * @since 2.0
 */
public class AnnotationBarHoverManager extends AbstractHoverInformationControlManager {
	public static final boolean USE_RADIAL_LAYOUT= false;

	/**
	 * The  information control closer for the hover information. Closes the information control as 
	 * soon as the mouse pointer leaves the subject area, a mouse button is pressed, the user presses a key,
	 * or the subject control is resized or moved.
	 */
	protected class Closer extends MouseTrackAdapter 
	implements IInformationControlCloser, MouseListener, MouseMoveListener, ControlListener, KeyListener, DisposeListener {
		
		/** The closer's subject control */
		private Control fSubjectControl;
		/** The subject area */
		private Rectangle fSubjectArea;
		/** Indicates whether this closer is active */
		private boolean fIsActive= false;
		/** The information control. */
		private IInformationControl fInformationControl;
		boolean fIsInformationListener;
		
		/**
		 * Creates a new information control closer.
		 */
		public Closer() {
		}
		
		/*
		 * @see IInformationControlCloser#setSubjectControl(Control)
		 */
		public void setSubjectControl(Control control) {
			fSubjectControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#setHoverControl(IHoverControl)
		 */
		public void setInformationControl(IInformationControl control) {
			fInformationControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#start(Rectangle)
		 */
		public void start(Rectangle subjectArea) {
			
			if (fIsActive) return;
			fIsActive= true;
			
			fSubjectArea= subjectArea;
		
			// if this is a popup that takes mouse events and will allow hovering, then
			// don't install the mouse listeners but just trust the information control to
			// dispose of itself once its no longer used.
			if (fInformationControl instanceof IInformationControlExtension3 && ((IInformationControlExtension3)fInformationControl).isMouseController()) {
				// transfer mouse handling to the information control
				fInformationControl.addDisposeListener(this);
			} else if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addMouseListener(this);
				fSubjectControl.addMouseMoveListener(this);
				fSubjectControl.addMouseTrackListener(this);
			}
			
			// install control and key listeners on subject control in any case 
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addControlListener(this);
				fSubjectControl.addKeyListener(this);
			}
		}
		
		/*
		 * @see IInformationControlCloser#stop()
		 */
		public void stop() {
			stop(false);
		}
		
		/**
		 * Stops the information control and if <code>delayRestart</code> is set
		 * allows restart only after a certain delay.
		 * 
		 * @param delayRestart <code>true</code> if restart should be delayed
		 */
		protected void stop(boolean delayRestart) {
			
			if (!fIsActive) return;
			fIsActive= false;
			
			hideInformationControl();
			
			if (fInformationControl instanceof IInformationControlExtension3 && ((IInformationControlExtension3)fInformationControl).isMouseController()) {
				fInformationControl.removeDisposeListener(this);
			} else if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseListener(this);
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.removeMouseTrackListener(this);
			}
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeControlListener(this);
				fSubjectControl.removeKeyListener(this);
			}			
		}
		
		/*
		 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseMove(MouseEvent event) {
			if (!fSubjectArea.contains(event.x, event.y))
				stop();
		}
		
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseUp(MouseEvent event) {
		}
		
		/*
		 * @see MouseListener#mouseDown(MouseEvent)
		 */
		public void mouseDown(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseListener#mouseDoubleClick(MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseTrackAdapter#mouseExit(MouseEvent)
		 */
		public void mouseExit(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlResized(ControlEvent)
		 */
		public void controlResized(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlMoved(ControlEvent)
		 */
		public void controlMoved(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see KeyListener#keyReleased(KeyEvent)
		 */
		public void keyReleased(KeyEvent event) {
		}
		
		/*
		 * @see KeyListener#keyPressed(KeyEvent)
		 */
		public void keyPressed(KeyEvent event) {
			stop(true);
		}

		/*
		 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
		 */
		public void widgetDisposed(DisposeEvent e) {
			stop();
		}
	}
	
	/** The source viewer the manager is connected to */
	private ISourceViewer fSourceViewer;
	/** The vertical ruler the manager is registered with */
	private IVerticalRulerInfo fVerticalRulerInfo;
	/** The annotation hover the manager uses to retrieve the information to display */
	private IAnnotationHover fAnnotationHover;

	/**
	 * Creates an annotation hover manager with the given parameters. In addition,
	 * the hovers anchor is RIGHT and the margin is 5 points to the right.
	 *
	 * @param sourceViewer the source viewer this manager connects to
	 * @param ruler the vertical ruler this manager connects to
	 * @param annotationHover the annotation hover providing the information to be displayed
	 * @param creator the information control creator
	 * @deprecated As of 2.1, replaced by {@link AnnotationBarHoverManager#AnnotationBarHoverManager(IVerticalRulerInfo, ISourceViewer, IAnnotationHover, IInformationControlCreator)}
	 */
	public AnnotationBarHoverManager(ISourceViewer sourceViewer, IVerticalRuler ruler, IAnnotationHover annotationHover, IInformationControlCreator creator) {
		this(ruler, sourceViewer, annotationHover, creator);
	}
	
	/**
	 * Creates an annotation hover manager with the given parameters. In addition,
	 * the hovers anchor is RIGHT and the margin is 5 points to the right.
	 *
	 * @param ruler the vertical ruler this manager connects to
	 * @param sourceViewer the source viewer this manager connects to
	 * @param annotationHover the annotation hover providing the information to be displayed
	 * @param creator the information control creator
	 * @since 2.1
	 */
	public AnnotationBarHoverManager(IVerticalRulerInfo rulerInfo, ISourceViewer sourceViewer, IAnnotationHover annotationHover, IInformationControlCreator creator) {
		super(creator);
		
		Assert.isNotNull(sourceViewer);
		Assert.isNotNull(annotationHover);
		
		fSourceViewer= sourceViewer;
		fVerticalRulerInfo= rulerInfo;
		fAnnotationHover= annotationHover;
		
		setAnchor(ANCHOR_RIGHT);
		setMargins(5, 0);
		setCloser(new Closer());
	}	
	
	/*
	 * @see AbstractHoverInformationControlManager#computeInformation()
	 */
	protected void computeInformation() {
		MouseEvent event= getHoverEvent();
		IAnnotationHover hover= getHover(event);

		int line= getHoverLine(event);
		
		if (hover instanceof IAnnotationHoverExtension2) {
			IAnnotationHoverExtension2 expandHover= (IAnnotationHoverExtension2) hover;
			setCustomInformationControlCreator(expandHover.getInformationControlCreator());
			setInformation(expandHover.getHoverInfo2(fSourceViewer, line), computeArea(line));
		} else if (hover instanceof IAnnotationHoverExtension) {
			IAnnotationHoverExtension extension= (IAnnotationHoverExtension) hover;
			setCustomInformationControlCreator(extension.getInformationControlCreator());
			setInformation(extension.getHoverInfo(fSourceViewer, line, fSourceViewer.getTopIndex(), fSourceViewer.getBottomIndex()), computeArea(line));
		} else {
			setCustomInformationControlCreator(null);
			setInformation(hover.getHoverInfo(fSourceViewer, line), computeArea(line));
		}
			
	}
	
	/**
	 * Determines the hover to be used to display information based on the source of the
	 * mouse hover event. If <code>fVerticalRulerInfo</code> is not a composite ruler, the 
	 * standard hover is returned.
	 * 
	 * @param source the source of the mouse hover event
	 * @return the hover depending on <code>source</code>, or <code>fAnnotationHover</code> if none can be found.
	 * @since 3.0
	 */
	private IAnnotationHover getHover(MouseEvent event) {
		if (event == null || event.getSource() == null)
			return fAnnotationHover;
			
		if (fVerticalRulerInfo instanceof CompositeRuler) {
			CompositeRuler comp= (CompositeRuler) fVerticalRulerInfo;
			for (Iterator it= comp.getDecoratorIterator(); it.hasNext();) {
				Object o= it.next();
				if (o instanceof IVerticalRulerInfoExtension && o instanceof IVerticalRulerInfo) {
					if (((IVerticalRulerInfo) o).getControl() == event.getSource()) {
						IAnnotationHover hover= ((IVerticalRulerInfoExtension) o).getHover();
						if (hover != null) 
							return hover;
					}
				} 
			}
		}
		return fAnnotationHover;
	}


	/**
	 * Returns the line of interest deduced from the mouse hover event.
	 * 
	 * @param event a mouse hover event that triggered hovering
	 * @return the document model line number on which the hover event occurred or <code>-1</code> if there is no event
	 * @since 3.0
	 */
	private int getHoverLine(MouseEvent event) {
		return event == null ? -1 : fVerticalRulerInfo.toDocumentLineNumber(event.y);
	}

	/**
	 * Returns for the widget line number for the given document line number.
	 * 
	 * @param line the absolute line number
	 * @return the line number relative to the viewer's visible region
	 * @throws BadLocationException if <code>line</code> is not valid in the viewer's document
	 */
	private int getWidgetLineNumber(int line) throws BadLocationException {
		if (fSourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) fSourceViewer;
			return extension.modelLine2WidgetLine(line);
		}
		
		IRegion region= fSourceViewer.getVisibleRegion();
		int firstLine= fSourceViewer.getDocument().getLineOfOffset(region.getOffset());
		return line - firstLine;
	}
	
	/**
	 * Determines graphical area covered by the given line.
	 *
	 * @param line the number of the line in the viewer whose graphical extend in the vertical ruler must be computed
	 * @return the graphical extend of the given line
	 */
	private Rectangle computeArea(int line) {
		try {
			StyledText text= fSourceViewer.getTextWidget();
			int lineHeight= text.getLineHeight();
			int y= getWidgetLineNumber(line) * lineHeight - text.getTopPixel();
			Point size= fVerticalRulerInfo.getControl().getSize();
			return new Rectangle(0, y, size.x, lineHeight);
		} catch (BadLocationException x) {
		}
		return null;
	}
	
	/**
	 * Returns the annotation hover for this hover manager.
	 * 
	 * @return the annotation hover for this hover manager
	 * @since 2.1
	 */
	protected IAnnotationHover getAnnotationHover() {
		return fAnnotationHover;
	}

	/**
	 * Returns the source viewer for this hover manager.
	 * 
	 * @return the source viewer for this hover manager
	 * @since 2.1
	 */
	protected ISourceViewer getSourceViewer() {
		return fSourceViewer;
	}

	/**
	 * Returns the vertical ruler info for this hover manager
	 * 
	 * @return the vertical ruler info for this hover manager
	 * @since 2.1
	 */
	protected IVerticalRulerInfo getVerticalRulerInfo() {
		return fVerticalRulerInfo;
	}

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControlManager#computeSizeConstraints(org.eclipse.swt.widgets.Control, org.eclipse.jface.text.IInformationControl)
	 * @since 3.0
	 */
	protected Point computeSizeConstraints(Control subjectControl, IInformationControl informationControl) {
		/* limit the hover to the size of the styled text's client area. */
		StyledText styledText= fSourceViewer.getTextWidget();
		if (styledText == null) 
			return super.computeSizeConstraints(subjectControl, informationControl); 
		
		Rectangle r= styledText.getClientArea();
		if (r == null)
			return super.computeSizeConstraints(subjectControl, informationControl);
		return new Point(r.width, r.height);
	}
	
	/*
	 * @see org.eclipse.jface.text.AbstractInformationControlManager#computeInformationControlLocation(org.eclipse.swt.graphics.Rectangle, org.eclipse.swt.graphics.Point)
	 * @since 3.0
	 */
	protected Point computeInformationControlLocation(Rectangle subjectArea, Point controlSize) {
		MouseEvent event= getHoverEvent();
		IAnnotationHover hover= getHover(event);

		if (hover instanceof IAnnotationHoverExtension)  {
			ITextSelection lineRange= ((IAnnotationHoverExtension) hover).getLineRange(fSourceViewer, getHoverLine(event), fSourceViewer.getTopIndex(), fSourceViewer.getBottomIndex());
			if (lineRange != null && !lineRange.isEmpty())
				return computeViewerRange(lineRange);
		}
		return super.computeInformationControlLocation(subjectArea, controlSize);
	}
	
	/*
	 * @see org.eclipse.jface.text.AbstractInformationControlManager#computeLocation(org.eclipse.swt.graphics.Rectangle, org.eclipse.swt.graphics.Point, org.eclipse.jface.text.AbstractInformationControlManager.Anchor)
	 */
	protected Point computeLocation(Rectangle subjectArea, Point controlSize, Anchor anchor) {
		MouseEvent event= getHoverEvent();
		IAnnotationHover hover= getHover(event);
		
		if (hover instanceof IAnnotationHoverExtension2) {
			Control subjectControl= getSubjectControl();
			
			// HACK for circular layout positioning (only works if there is enough space...)
			if (USE_RADIAL_LAYOUT) {
				// set center of radial control to center of annotation / subject area
				// HACK if other rulers than just the annotation ruler are displayed, the popup gets centered over all 
				// -> use 6 as we know that the annotation ruler is 12 wide.
				Point ret= subjectControl.toDisplay(subjectArea.x + 6 - controlSize.x / 2, subjectArea.y + subjectArea.height / 2 - controlSize.y / 2);
				ret.x= Math.max(0, ret.x);
				ret.y= Math.max(0, ret.y);
				return ret;
			}
			// return a location that just overlaps the annotation on the bar
			if (anchor == AbstractInformationControlManager.ANCHOR_RIGHT)
				return subjectControl.toDisplay(subjectArea.x - 4, subjectArea.y - 2);
			else if (anchor == AbstractHoverInformationControlManager.ANCHOR_LEFT)
				return subjectControl.toDisplay(subjectArea.x + subjectArea.width - controlSize.x + 4, subjectArea.y - 2);
		}
		return super.computeLocation(subjectArea, controlSize, anchor);
	}

	/*
	 * @see org.eclipse.jface.text.AbstractInformationControlManager#showInformationControl(org.eclipse.swt.graphics.Rectangle)
	 */
	protected void showInformationControl(Rectangle subjectArea) {
//		if (getHover(getHoverEvent()) instanceof AnnotationExpandHover) {
//			// set subject area to be the size of the popup
////			Control control= ((AnnotationExpansionControl) getInformationControl()).getControl();
////			Point point= control.getSize();
////			Point point= getInformationControl().computeSizeHint();
////			Rectangle rect= new Rectangle(0, 0, point.x, point.y);
//			
//			Rectangle rect= new Rectangle(0, 0, 50, 20);
//			super.showInformationControl(rect);
//			return;
//		}
		super.showInformationControl(subjectArea);
		
	}

	/**
	 * Computes the hover location for the given line range.
	 * 
	 * @param lineRange the first and last line covered by the hover, encoded as the <code>x</code> and <code>y</code> fields of a <code>Point</code>
	 * @return a <code>Point</code>containing the display coordinates of the hover location
	 * @since 3.0
	 */
	private Point computeViewerRange(ITextSelection lineRange) {
		final int topLine= fSourceViewer.getTopIndex();
		// compute pixel offset taking in account partially visible lines.
		int lineDelta= lineRange.getStartLine() - topLine;
		StyledText textWidget= fSourceViewer.getTextWidget();
		int lineHeight= textWidget.getLineHeight();
		// note that this works independently of the widget2model mapping, since we just get the 
		// pixels of the first paritally visible line, if there is one.
		int partial= (lineHeight - (textWidget.getTopPixel() % lineHeight)) % lineHeight;
		int y= lineDelta * lineHeight + partial;
		int x= 1; // avoids line overlay of the hover and the editor border.
		
		return textWidget.toDisplay(x, y);
	}
}

