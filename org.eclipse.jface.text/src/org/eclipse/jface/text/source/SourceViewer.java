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

import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

import org.eclipse.jface.text.AbstractHoverInformationControlManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension2;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;

/**
 * SWT based implementation of <code>ISourceViewer</code>. The same rules apply 
 * as for <code>TextViewer</code>. A source viewer uses an <code>IVerticalRuler</code>
 * as its annotation presentation area. The vertical ruler is a small strip shown left
 * of the viewer's text widget. A source viewer uses an <code>IOverviewRuler</code>
 * as its presentation area for the annotation overview. The overview ruler is a small strip
 * shown right of the viewer's text widget.<p>
 * Clients are supposed to instantiate a source viewer and subsequently to communicate
 * with it exclusively using the <code>ISourceViewer</code> interface. Clients should not
 * subclass this class as it is rather likely that subclasses will be broken by future releases. 
 */
public class SourceViewer extends TextViewer implements ISourceViewer, ISourceViewerExtension {


	/**
	 * Layout of a source viewer. Vertical ruler, text widget, and overview ruler are shown side by side.
	 */
	class RulerLayout extends Layout {
		
		/** The gap between the text viewer and the vertical ruler. */
		protected int fGap;
		
		/** 
		 * Creates a new ruler layout with the given gap between text viewer and vertical ruler.
		 * 
		 * @param gap the gap between text viewer and vertical ruler
		 */
		protected RulerLayout(int gap) {
			fGap= gap;
		}
		
		/*
		 * @see Layout#computeSize(Composite, int, int, boolean)
		 */
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Control[] children= composite.getChildren();
			Point s= children[children.length - 1].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
			if (fVerticalRuler != null && fIsVerticalRulerVisible)
				s.x += fVerticalRuler.getWidth() + fGap;
			return s;
		}
		
		/*
		 * @see Layout#layout(Composite, boolean)
		 */
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle clArea= composite.getClientArea();
			if (fVerticalRuler != null && fIsVerticalRulerVisible) {
				
				Rectangle trim= getTextWidget().computeTrim(0, 0, 0, 0);
				int scrollbarHeight= trim.height;
				
				int verticalRulerWidth= fVerticalRuler.getWidth();
				int overviewRulerWidth= 0;
				if (fOverviewRuler != null && fIsOverviewRulerVisible) {
					overviewRulerWidth= fOverviewRuler.getWidth();
					fOverviewRuler.getControl().setBounds(clArea.width - overviewRulerWidth -1, scrollbarHeight, overviewRulerWidth, clArea.height - 3*scrollbarHeight);
					fOverviewRuler.getHeaderControl().setBounds(clArea.width - overviewRulerWidth -1, 0, overviewRulerWidth, scrollbarHeight);
				}				
				
				getTextWidget().setBounds(verticalRulerWidth + fGap, 0, clArea.width - verticalRulerWidth - overviewRulerWidth - 2*fGap, clArea.height);
				fVerticalRuler.getControl().setBounds(0, 0, verticalRulerWidth, clArea.height - scrollbarHeight);
			
			} else
				getTextWidget().setBounds(0, 0, clArea.width, clArea.height);
		}
	}
	
	
	/** The viewer's content assistant */
	protected IContentAssistant fContentAssistant;
	/** 
	 * Flag indicating whether the viewer's content assistant is installed
	 * @since 2.0
	 */
	protected boolean fContentAssistantInstalled;
	/** The viewer's content formatter */
	protected IContentFormatter fContentFormatter;
	/** The viewer's model reconciler */
	protected IReconciler fReconciler;
	/** The viewer's presentation reconciler */
	protected IPresentationReconciler fPresentationReconciler;
	/** The viewer's annotation hover */
	protected IAnnotationHover fAnnotationHover;
	/**
	 * Stack of saved selections in the underlying document
	 * @since 3.0
	 */
	protected final Stack fSelections= new Stack();
	/**
	 * Position category of saved selections
	 * @since 3.0
	 */
	protected final static String SELECTION_POSITION_CATEGORY= "__selection_category";
	/**
	 * Position updater for saved selections
	 * @since 3.0
	 */
	protected IPositionUpdater fSelectionUpdater= null;
	/** 
	 * The viewer's overview ruler annotation hover
	 * @since 3.0
	 */
	protected IAnnotationHover fOverviewRulerAnnotationHover;
	/** 
	 * The viewer's information presenter
	 * @since 2.0
	 */
	protected IInformationPresenter fInformationPresenter;
	
	/** Visual vertical ruler */
	private IVerticalRuler fVerticalRuler;
	/** Visibility of vertical ruler */
	private boolean fIsVerticalRulerVisible;
	/** The SWT widget used when supporting a vertical ruler */
	private Composite fComposite;
	/** The vertical ruler's annotation model */
	private VisualAnnotationModel fVisualAnnotationModel;
	/** The viewer's range indicator to be shown in the vertical ruler */
	private Annotation fRangeIndicator;
	/** The viewer's vertical ruler hovering controller */
	private AbstractHoverInformationControlManager fVerticalRulerHoveringController;
	/** 
	 * The viewer's overview ruler hovering controller
	 * @since 2.1
	 */
	private AbstractHoverInformationControlManager fOverviewRulerHoveringController;
	
	/**
	 * The overview ruler.
	 * @since 2.1
	 */
	private IOverviewRuler fOverviewRuler;
	/**
	 * The visibility of the overview ruler 
	 * @since 2.1
	 */
	private boolean fIsOverviewRulerVisible;
	
	
	/** The size of the gap between the vertical ruler and the text widget */
	protected final static int GAP_SIZE= 2;
	
	
	/**
	 * Constructs a new source viewer. The vertical ruler is initially visible.
	 * The viewer has not yet been initialized with a source viewer configuration.
	 *
	 * @param parent the parent of the viewer's control
	 * @param ruler the vertical ruler used by this source viewer
	 * @param styles the SWT style bits
	 */
	public SourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		this(parent, ruler, null, false, styles);
	}
	
	/**
	 * Constructs a new source viewer. The vertical ruler is initially visible. 
	 * The overview ruler visibility is controlled by the value of <code>showAnnotationsOverview</code>.
	 * The viewer has not yet been initialized with a source viewer configuration.
	 *
	 * @param parent the parent of the viewer's control
	 * @param verticalRuler the vertical ruler used by this source viewer
	 * @param overviewRuler the overview ruler
	 * @param showAnnotationsOverview <code>true</code> if the overview ruler should be visible, <code>false</code> otherwise
	 * @param styles the SWT style bits
	 * @since 2.1
	 */
	public SourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles) {
		super();
		
		fVerticalRuler= verticalRuler;
		fIsVerticalRulerVisible= (verticalRuler != null);
		fOverviewRuler= overviewRuler;
		fIsOverviewRulerVisible= (showAnnotationsOverview && overviewRuler != null);
		
		createControl(parent, styles);
	}
	
	/*
	 * @see TextViewer#createControl(Composite, int)
	 */
	protected void createControl(Composite parent, int styles) {
		
		if (fVerticalRuler != null || fOverviewRuler != null) {
			styles= (styles & ~SWT.BORDER);
			fComposite= new Canvas(parent, SWT.NONE);
			fComposite.setLayout(new RulerLayout(GAP_SIZE));
			parent= fComposite;
		}
		
		super.createControl(parent, styles);
					
		if (fVerticalRuler != null)
			fVerticalRuler.createControl(fComposite, this);
		if (fOverviewRuler != null)
			fOverviewRuler.createControl(fComposite, this);
	}
	
	/*
	 * @see TextViewer#getControl()
	 */
	public Control getControl() {
		if (fComposite != null)
			return fComposite;
		return super.getControl();
	}
	
	/*
	 * @see ISourceViewer#setAnnotationHover(IAnnotationHover)
	 */
	public void setAnnotationHover(IAnnotationHover annotationHover) {
		fAnnotationHover= annotationHover;
	}

	/**
	 * Sets the overview ruler's annotation hover of this source viewer.
	 * The annotation hover provides the information to be displayed in a hover
	 * popup window if requested over the overview rulers area. The annotation
	 * hover is assumed to be line oriented.
	 *
	 * @param annotationHover the hover to be used, <code>null</code> is a valid argument
	 * @since 3.0
	 */
	public void setOverviewRulerAnnotationHover(IAnnotationHover annotationHover) {
		fOverviewRulerAnnotationHover= annotationHover;
	}
	
	/*
	 * @see ISourceViewer#configure(SourceViewerConfiguration)
	 */
	public void configure(SourceViewerConfiguration configuration) {
		
		if (getTextWidget() == null)
			return;
			
		setDocumentPartitioning(configuration.getConfiguredDocumentPartitioning(this));
		
		// install content type independent plugins
		fPresentationReconciler= configuration.getPresentationReconciler(this);
		if (fPresentationReconciler != null)
			fPresentationReconciler.install(this);
								
		fReconciler= configuration.getReconciler(this);
		if (fReconciler != null)
			fReconciler.install(this);
			
		fContentAssistant= configuration.getContentAssistant(this);
		if (fContentAssistant != null) {
			fContentAssistant.install(this);
			fContentAssistantInstalled= true;
		}
			
		fContentFormatter= configuration.getContentFormatter(this);
		
		fInformationPresenter= configuration.getInformationPresenter(this);
		if (fInformationPresenter != null)
			fInformationPresenter.install(this);
		
		setUndoManager(configuration.getUndoManager(this));
		
		getTextWidget().setTabs(configuration.getTabWidth(this));
		
		setAnnotationHover(configuration.getAnnotationHover(this));
		setOverviewRulerAnnotationHover(configuration.getOverviewRulerAnnotationHover(this));
		
		setHoverControlCreator(configuration.getInformationControlCreator(this));
		
		// install content type specific plugins
		String[] types= configuration.getConfiguredContentTypes(this);
		for (int i= 0; i < types.length; i++) {
			
			String t= types[i];
				
			setAutoIndentStrategy(configuration.getAutoIndentStrategy(this, t), t);
			setTextDoubleClickStrategy(configuration.getDoubleClickStrategy(this, t), t);
			
			int[] stateMasks= configuration.getConfiguredTextHoverStateMasks(this, t);
			if (stateMasks != null) {
				for (int j= 0; j < stateMasks.length; j++)	{
					int stateMask= stateMasks[j];
					setTextHover(configuration.getTextHover(this, t, stateMask), t, stateMask);
				}
			} else {
				setTextHover(configuration.getTextHover(this, t), t, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
			}
			
			String[] prefixes= configuration.getIndentPrefixes(this, t);
			if (prefixes != null && prefixes.length > 0)
				setIndentPrefixes(prefixes, t);
			
			prefixes= configuration.getDefaultPrefixes(this, t);
			if (prefixes != null && prefixes.length > 0)
				setDefaultPrefixes(prefixes, t);
		}
		
		activatePlugins();
	}
	
	/**
	 * After this method has been executed the caller knows that any installed annotation hover has been installed.
	 */
	protected void ensureAnnotationHoverManagerInstalled() {
		if (fVerticalRuler != null && fAnnotationHover != null && fVerticalRulerHoveringController == null && fHoverControlCreator != null) {
			fVerticalRulerHoveringController= new AnnotationBarHoverManager(fVerticalRuler, this, fAnnotationHover, fHoverControlCreator);
			fVerticalRulerHoveringController.install(fVerticalRuler.getControl());
		}
	}
	
	/**
	 * After this method has been executed the caller knows that any installed overview hover has been installed.
	 */
	protected void ensureOverviewHoverManagerInstalled() {
		if (fOverviewRuler != null &&  fOverviewRulerAnnotationHover != null  && fOverviewRulerHoveringController == null && fHoverControlCreator != null)	{
			fOverviewRulerHoveringController= new OverviewRulerHoverManager(fOverviewRuler, this, fOverviewRulerAnnotationHover, fHoverControlCreator);
			fOverviewRulerHoveringController.install(fOverviewRuler.getControl());
		}
	}
	
	/*
	 * @see TextViewer#activatePlugins()
	 */
	public void activatePlugins() {
		ensureAnnotationHoverManagerInstalled();
		ensureOverviewHoverManagerInstalled();
		super.activatePlugins();
	}
	
	/*
	 * @see ISourceViewer#setDocument(IDocument, IAnnotationModel)
	 */
	public void setDocument(IDocument document) {
		setDocument(document, null, -1, -1);		
	}
	
	/*
	 * @see ISourceViewer#setDocument(IDocument, IAnnotationModel, int, int)
	 */
	public void setDocument(IDocument document, int visibleRegionOffset, int visibleRegionLength) {
		setDocument(document, null, visibleRegionOffset, visibleRegionLength);
	}
	
	/*
	 * @see ISourceViewer#setDocument(IDocument, IAnnotationModel)
	 */
	public void setDocument(IDocument document, IAnnotationModel annotationModel) {
		setDocument(document, annotationModel, -1, -1);		
	}
	
	/*
	 * @see ISourceViewer#setDocument(IDocument, IAnnotationModel, int, int)
	 */
	public void setDocument(IDocument document, IAnnotationModel annotationModel, int visibleRegionOffset, int visibleRegionLength) {
		
		if (fVerticalRuler == null && fOverviewRuler == null) {
			
			if (visibleRegionOffset == -1 && visibleRegionLength == -1)
				super.setDocument(document);
			else
				super.setDocument(document, visibleRegionOffset, visibleRegionLength);
		
		} else {
			
			if (fVisualAnnotationModel != null && getDocument() != null)
				fVisualAnnotationModel.disconnect(getDocument());
						
			if (annotationModel != null && document != null) {
				fVisualAnnotationModel= new VisualAnnotationModel(annotationModel);
				fVisualAnnotationModel.connect(document);
			} else {
				fVisualAnnotationModel= null;
			}
			
			if (visibleRegionOffset == -1 && visibleRegionLength == -1)
				super.setDocument(document);
			else
				super.setDocument(document, visibleRegionOffset, visibleRegionLength);
				
			if (fVerticalRuler != null)
				fVerticalRuler.setModel(fVisualAnnotationModel);
			
			if (fOverviewRuler != null)
				fOverviewRuler.setModel(fVisualAnnotationModel);
		}		
	}
	
	/*
	 * @see ISourceViewer#getAnnotationModel()
	 */
	public IAnnotationModel getAnnotationModel() {
		if (fVisualAnnotationModel != null)
			return fVisualAnnotationModel.getModelAnnotationModel();
		return null;
	}
	
	/*
	 * @see TextViewer#handleDispose()
	 */
	protected void handleDispose() {
		
		if (fPresentationReconciler != null) {
			fPresentationReconciler.uninstall();
			fPresentationReconciler= null;
		}
		
		if (fReconciler != null) {
			fReconciler.uninstall();
			fReconciler= null;
		}
		
		if (fContentAssistant != null) {
			fContentAssistant.uninstall();
			fContentAssistantInstalled= false;
			fContentAssistant= null;
		}
		
		fContentFormatter= null;
		
		if (fInformationPresenter != null) {
			fInformationPresenter.uninstall();
			fInformationPresenter= null;
		}
		
		if (fVisualAnnotationModel != null && getDocument() != null) {
			fVisualAnnotationModel.disconnect(getDocument());
			fVisualAnnotationModel= null;
		}
		
		fVerticalRuler= null;
				
		if (fVerticalRulerHoveringController != null) {
			fVerticalRulerHoveringController.dispose();
			fVerticalRulerHoveringController= null;
		}
		
		fOverviewRuler= null;
					
		if (fOverviewRulerHoveringController != null) {
			fOverviewRulerHoveringController.dispose();
			fOverviewRulerHoveringController= null;
		}
		
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=15300
		fComposite= null;
		
		super.handleDispose();
	}
	
	/*
	 * @see ITextOperationTarget#canDoOperation(int)
	 */
	public boolean canDoOperation(int operation) {
		
		if (getTextWidget() == null || !redraws())
			return false;
		
		if (operation == CONTENTASSIST_PROPOSALS)
			return fContentAssistant != null && fContentAssistantInstalled && isEditable();
			
		if (operation == CONTENTASSIST_CONTEXT_INFORMATION)
			return fContentAssistant != null && fContentAssistantInstalled && isEditable();
			
		if (operation == INFORMATION)
			return fInformationPresenter != null;
			
		if (operation == FORMAT) {
			Point p= getSelectedRange();
			int length= (p == null ? -1 : p.y);
			return (fContentFormatter != null && isEditable() && (length == 0 || isBlockSelected()));
		}
		
		return super.canDoOperation(operation);
	}
	
	/**
	 * Creates a new formatting context for a format operation.
	 * <p>
	 * After the use of the context, clients are required to call
	 * its <code>dispose</code> method.
	 * 
	 * @return The new formatting context
	 */
	protected IFormattingContext createFormattingContext() {
		return new FormattingContext();	
	}
	
	/**
	 * Saves the current selection in the document.
	 */
	public void saveSelection(int offset, int length) {

		final IDocument document= getDocument();

		if (fSelections.isEmpty()) {

			fSelectionUpdater= new DefaultPositionUpdater(SELECTION_POSITION_CATEGORY);
			document.addPositionCategory(SELECTION_POSITION_CATEGORY);
			document.addPositionUpdater(fSelectionUpdater);
		}

		final Position selection;
		try {

			selection= new Position(offset, length);
			document.addPosition(SELECTION_POSITION_CATEGORY, selection);
			fSelections.push(selection);

		} catch (BadLocationException exception) {
			// Should not happen
		} catch (BadPositionCategoryException exception) {
			// Should not happen
		}
	}
	
	/**
	 * Restores a previously saved selection in the document.
	 */
	public void restoreSelection() {

		if (!fSelections.isEmpty()) {

			final IDocument document= getDocument();
			final Position selection= (Position)fSelections.pop();

			try {
				document.removePosition(SELECTION_POSITION_CATEGORY, selection);
				setSelectedRange(selection.getOffset(), selection.getLength());

				if (fSelections.isEmpty()) {

					document.removePositionUpdater(fSelectionUpdater);
					fSelectionUpdater= null;
					document.removePositionCategory(SELECTION_POSITION_CATEGORY);
				}
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}
		}
	}

	/*
	 * @see ITextOperationTarget#doOperation(int)
	 */
	public void doOperation(int operation) {
		
		if (getTextWidget() == null || !redraws())
			return;
		
		switch (operation) {
			case CONTENTASSIST_PROPOSALS:
				fContentAssistant.showPossibleCompletions();
				return;
			case CONTENTASSIST_CONTEXT_INFORMATION:
				fContentAssistant.showContextInformation();
				return;
			case INFORMATION:
				fInformationPresenter.showInformation();
				return;
			case FORMAT :
				{
					final Point selection= getSelectedRange();
					saveSelection(selection.x, selection.y);
					
					final IRegion region= new Region(selection.x, selection.y); 
					final IFormattingContext context= createFormattingContext();

					if (selection.y == 0) {
						context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
					} else {
						context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.FALSE);
						context.setProperty(FormattingContextProperties.CONTEXT_REGION, region);
					}
					try {
						setRedraw(false);

						final IDocument document= getDocument();
						if (fContentFormatter instanceof IContentFormatterExtension2) {

							final IContentFormatterExtension2 extension= (IContentFormatterExtension2)fContentFormatter;
							extension.format(document, context);

						} else
							fContentFormatter.format(document, region);

					} finally {
						
						restoreSelection();
						context.dispose();
						
						setRedraw(true);
					}
					return;
				}
			default :
				super.doOperation(operation);
		}
	}
	
	/*
	 * @see ITextOperationTargetExtension#enableOperation(int, boolean)
	 * @since 2.0
	 */
	public void enableOperation(int operation, boolean enable) {
		
		switch (operation) {
			case CONTENTASSIST_PROPOSALS:
			case CONTENTASSIST_CONTEXT_INFORMATION: {
				
				if (fContentAssistant == null)
					return;
			
				if (enable) {
					if (!fContentAssistantInstalled) {
						fContentAssistant.install(this);
						fContentAssistantInstalled= true;
					}
				} else if (fContentAssistantInstalled) {
					fContentAssistant.uninstall();
					fContentAssistantInstalled= false;
				}
			}
		}
	}
		
	/*
	 * @see ISourceViewer#setRangeIndicator(Annotation)
	 */
	public void setRangeIndicator(Annotation rangeIndicator) {
		fRangeIndicator= rangeIndicator;
	}
		
	/*
	 * @see ISourceViewer#setRangeIndication(int, int, boolean)
	 */
	public void setRangeIndication(int start, int length, boolean moveCursor) {
		
		if (moveCursor) {
			setSelectedRange(start, 0);
			revealRange(start, length);
		}
		
		if (fRangeIndicator != null && fVisualAnnotationModel != null)
			fVisualAnnotationModel.modifyAnnotation(fRangeIndicator, new Position(start, length));
	}
	
	/*
	 * @see ISourceViewer#getRangeIndication()
	 */
	public IRegion getRangeIndication() {
		if (fRangeIndicator != null && fVisualAnnotationModel != null) {
			Position position= fVisualAnnotationModel.getPosition(fRangeIndicator);
			if (position != null)
				return new Region(position.getOffset(), position.getLength());
		}
		
		return null;
	}
	
	/*
	 * @see ISourceViewer#removeRangeIndication()
	 */
	public void removeRangeIndication() {
		if (fRangeIndicator != null && fVisualAnnotationModel != null)
			fVisualAnnotationModel.modifyAnnotation(fRangeIndicator, null);
	}
	
	/*
	 * @see ISourceViewer#showAnnotations(boolean)
	 */
	public void showAnnotations(boolean show) {
		boolean old= fIsVerticalRulerVisible;
		fIsVerticalRulerVisible= (show && fVerticalRuler != null);
		if (old != fIsVerticalRulerVisible) {
			if (fComposite != null && !fComposite.isDisposed())
				fComposite.layout();
			if (fIsVerticalRulerVisible) {
				ensureAnnotationHoverManagerInstalled();
			} else if (fVerticalRulerHoveringController != null) {
				fVerticalRulerHoveringController.dispose();
				fVerticalRulerHoveringController= null;
			}
		}
	}
	
	/*
	 * @see org.eclipse.jface.text.source.ISourceViewerExtension#showAnnotationsOverview(boolean)
	 * @since 2.1
	 */
	public void showAnnotationsOverview(boolean show) {
		boolean old= fIsOverviewRulerVisible;
		fIsOverviewRulerVisible= (show && fOverviewRuler != null);
		if (old != fIsOverviewRulerVisible) {
			if (fComposite != null && !fComposite.isDisposed())
				fComposite.layout();
			if (fIsOverviewRulerVisible) {
				ensureOverviewHoverManagerInstalled();
			} else if (fOverviewRulerHoveringController != null) {
				fOverviewRulerHoveringController.dispose();
				fOverviewRulerHoveringController= null;
			}
		}
	}
}
