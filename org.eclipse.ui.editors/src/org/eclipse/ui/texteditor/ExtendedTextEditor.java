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
package org.eclipse.ui.texteditor;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.ChangeRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IChangeRulerColumn;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.LineNumberChangeRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.quickdiff.QuickDiff;

import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;

/**
 * An intermediate editor comprising functionality not present in the leaner <code>AbstractTextEditor</code>,
 * but used in many heavy weight (and especially source editing) editors, such as line numbers, 
 * change ruler, overview ruler, print margins, current line highlighting, etc.
 * 
 * <p>Note that this is work in progress and API is still subject to change for <code>3.0</code>.</p>
 * 
 * @since 3.0
 */
public abstract class ExtendedTextEditor extends StatusTextEditor {
	/**
	 * Preference key for showing the line number ruler.
	 */
	private final static String LINE_NUMBER_RULER= ExtendedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER;
	/**
	 * Preference key for the foreground color of the line numbers.
	 */
	private final static String LINE_NUMBER_COLOR= ExtendedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR;
	/**
	 * Preference key for showing the overview ruler.
	 */
	private final static String OVERVIEW_RULER= ExtendedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER;
	/**
	 * Preference key for highlighting current line.
	 */
	private final static String CURRENT_LINE= ExtendedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;
	/**
	 * Preference key for highlight color of current line.
	 */
	private final static String CURRENT_LINE_COLOR= ExtendedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR;
	/**
	 * Preference key for showing print marging ruler.
	 */
	private final static String PRINT_MARGIN= ExtendedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN;
	/**
	 * Preference key for print margin ruler color.
	 */
	private final static String PRINT_MARGIN_COLOR= ExtendedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR;
	/**
	 * Preference key for print margin ruler column.
	 **/
	private final static String PRINT_MARGIN_COLUMN= ExtendedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN;
	
	/**
	 * Adapter class for <code>IGotoMarker</code>.
	 */
	private class GotoMarkerAdapter implements IGotoMarker {
		public void gotoMarker(IMarker marker) {
			ExtendedTextEditor.this.gotoMarker(marker);
		}
	}
	
	/**
	 * The annotation preferences.
	 */
	private MarkerAnnotationPreferences fAnnotationPreferences;
	/** 
	 * The overview ruler of this editor.
	 * 
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use 
	 * {@link #getOverviewRuler()} instead.</p>
	 */
	protected IOverviewRuler fOverviewRuler;
	/**
	 * Helper for accessing annotation from the perspective of this editor.
	 * 
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use 
	 * {@link #getAnnotationAccess()} instead.</p>
	 */
	protected IAnnotationAccess fAnnotationAccess;
	/**
	 * Helper for managing the decoration support of this editor's viewer.
	 * 
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use 
	 * {@link #getSourceViewerDecorationSupport(ISourceViewer)} instead.</p>
	 */
	protected SourceViewerDecorationSupport fSourceViewerDecorationSupport;
	/**
	 * The line number column.
	 * 
	 * <p>This field should not be referenced by subclasses. It is <code>protected</code> for API
	 * compatibility reasons and will be made <code>private</code> soon. Use 
	 * {@link AbstractTextEditor#getVerticalRuler()} to access the vertical bar instead.</p>
	 */
	protected LineNumberRulerColumn fLineNumberRulerColumn;
	/**
	 * The change ruler column.
	 */
	private IChangeRulerColumn fChangeRulerColumn;
	/**
	 * Whether quick diff information is displayed, either on a change ruler or the line number ruler.
	 */
	private boolean fIsChangeInformationShown;
	/**
	 * The annotation ruler column used in the vertical ruler.
	 * @since 3.0
	 */
	private AnnotationRulerColumn fAnnotationRulerColumn;
	/** 
	 * The editor's implicit document provider.
	 * @since 3.0
	 */
	private IDocumentProvider fImplicitDocumentProvider;
	/** 
	 * The editor's goto marker adapter.
	 * @since 3.0 
	 */
	private Object fGotoMarkerAdapter= new GotoMarkerAdapter();
	
	
	/**
	 * Creates a new text editor.
	 */
	public ExtendedTextEditor() {
		super();
		fAnnotationPreferences= new MarkerAnnotationPreferences();
		setRangeIndicator(new DefaultRangeIndicator());
		initializeKeyBindingScopes();
		initializeEditor();
	}
	
	/**
	 * Initializes this editor.
	 */
	protected void initializeEditor() {
		setPreferenceStore(EditorsPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Initializes the key binding scopes of this editor.
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "org.eclipse.ui.textEditorScope" });  //$NON-NLS-1$
	}
	
	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {
		if (fSourceViewerDecorationSupport != null) {
			fSourceViewerDecorationSupport.dispose();
			fSourceViewerDecorationSupport= null;
		}
		
		fAnnotationAccess= null;
		fAnnotationPreferences= null;
		fAnnotationRulerColumn= null;
		
		super.dispose();
	}

	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow(IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.SHIFT_RIGHT);
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.SHIFT_LEFT);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		
		fAnnotationAccess= createAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());
		
		ISourceViewer viewer= new SourceViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		
		return viewer;
	}

	protected ISharedTextColors getSharedColors() {
		ISharedTextColors sharedColors= EditorsPlugin.getDefault().getSharedTextColors();
		return sharedColors;
	}

	protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
		IOverviewRuler ruler= new OverviewRuler(getAnnotationAccess(), VERTICAL_RULER_WIDTH, sharedColors);
		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference preference= (AnnotationPreference) e.next();
			if (preference.contributesToHeader())
				ruler.addHeaderAnnotationType(preference.getAnnotationType());
		}
		return ruler;
	}

	/**
	 * Creates the annotation access for this editor.
	 * 
	 * @return the created annotation access
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new DefaultMarkerAnnotationAccess(fAnnotationPreferences);
	}

	/**
	 * Configures the decoration support for this editor's the source viewer. Subclasses may override this
	 * method, but should call their superclass' implementation at some point.
	 */
	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {

		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext())
			support.setAnnotationPreference((AnnotationPreference) e.next());
		
		support.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
		support.setMarginPainterPreferenceKeys(PRINT_MARGIN, PRINT_MARGIN_COLOR, PRINT_MARGIN_COLUMN);
		support.setSymbolicFontName(getFontPropertyPreferenceKey());
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		if (fSourceViewerDecorationSupport != null)
			fSourceViewerDecorationSupport.install(getPreferenceStore());
		
		if (isPrefQuickDiffAlwaysOn())
			showChangeInformation(true);
	}
	
	/**
	 * Tells whether the overview ruler is visible.
	 */
	protected boolean isOverviewRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? store.getBoolean(OVERVIEW_RULER) : false;
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#showChangeInformation(boolean)
	 */
	public void showChangeInformation(boolean show) {
		if (show == fIsChangeInformationShown)
			return;
		
		if (fIsChangeInformationShown) {
			uninstallChangeRulerModel();
			showChangeRuler(false); // hide change ruler if its displayed - if the line number ruler is showing, only the colors get removed by deinstalling the model
		} else {
			ensureChangeInfoCanBeDisplayed();
			installChangeRulerModel();
		}
		
		fIsChangeInformationShown= show;
	}

	/**
	 * Installs the differ annotation model with the current quick diff display. 
	 */
	private void installChangeRulerModel() {
		IChangeRulerColumn column= getChangeColumn();
		if (column != null)
			column.setModel(getOrCreateDiffer());
		IOverviewRuler ruler= getOverviewRuler();
		if (ruler != null) {
			ruler.addAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffChange"); //$NON-NLS-1$
			ruler.addAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffAddition"); //$NON-NLS-1$
			ruler.addAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffDeletion"); //$NON-NLS-1$
			ruler.update();
		}
	}

	/**
	 * Uninstalls the differ annotation model from the current quick diff display.
	 */
	private void uninstallChangeRulerModel() {
		IChangeRulerColumn column= getChangeColumn();
		if (column != null)
			column.setModel(null);
		IOverviewRuler ruler= getOverviewRuler();
		if (ruler != null) {
			ruler.removeAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffChange"); //$NON-NLS-1$
			ruler.removeAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffAddition"); //$NON-NLS-1$
			ruler.removeAnnotationType("org.eclipse.ui.workbench.texteditor.quickdiffDeletion"); //$NON-NLS-1$
			ruler.update();
		}
		IAnnotationModel model= getOrCreateDiffer();
		if (model instanceof DocumentLineDiffer)
			((DocumentLineDiffer)model).suspend();
	}

	/**
	 * Ensures that either the line number display is a <code>LineNumberChangeRuler</code> or
	 * a separate change ruler gets displayed.
	 */
	private void ensureChangeInfoCanBeDisplayed() {
		if (isLineNumberRulerVisible()) {
			if (!(fLineNumberRulerColumn instanceof IChangeRulerColumn)) {
				hideLineNumberRuler();
				// HACK: set state already so a change ruler is created. Not needed once always a change line number bar gets installed
				fIsChangeInformationShown= true;
				showLineNumberRuler();
			}
		} else 
			showChangeRuler(true);
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditorExtension3#isChangeInformationShowing()
	 */
	public boolean isChangeInformationShowing() {
		return fIsChangeInformationShown;
	}
	
	/**
	 * Extracts the line differ from the displayed document's annotation model. If none can be found,
	 * a new differ is created and attached to the annotation model.
	 * 
	 * @return the linediffer, or <code>null</code> if none could be found or created.
	 */
	private IAnnotationModel getOrCreateDiffer() {
		// get annotation model extension
		ISourceViewer viewer= getSourceViewer();
		if (viewer == null)
			return null;
			
		IAnnotationModel m= viewer.getAnnotationModel();
		IAnnotationModelExtension model;
		if (m instanceof IAnnotationModelExtension)
			model= (IAnnotationModelExtension) m;
		else
			return null;
		
		// get diff model if it exists already
		IAnnotationModel differ= model.getAnnotationModel(IChangeRulerColumn.QUICK_DIFF_MODEL_ID);
		
		// create diff model if it doesn't
		if (differ == null) {
			String defaultId= getPreferenceStore().getString(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_DEFAULT_PROVIDER);
			differ= new QuickDiff().createQuickDiffAnnotationModel(this, defaultId);
			if (differ != null)
				model.addAnnotationModel(IChangeRulerColumn.QUICK_DIFF_MODEL_ID, differ);
		} else if (differ instanceof DocumentLineDiffer && !fIsChangeInformationShown)
			((DocumentLineDiffer)differ).resume();
		
		return differ;
	}

	/**
	 * Returns the <code>IChangeRulerColumn</code> of this editor, or <code>null</code> if there is none. Either
	 * the line number bar or a separate change ruler column can be returned.
	 * 
	 * @return an instance of <code>IChangeRulerColumn</code> or <code>null</code>.
	 */
	private IChangeRulerColumn getChangeColumn() {
		if (fChangeRulerColumn != null)
			return fChangeRulerColumn;
		else if (fLineNumberRulerColumn instanceof IChangeRulerColumn)
			return (IChangeRulerColumn) fLineNumberRulerColumn;
		else
			return null;
	}

	/**
	 * Sets the display state of the separate change ruler column (not the quick diff display on
	 * the line number ruler column) to <code>show</code>.
	 * 
	 * @param show <code>true</code> if the change ruler column should be shown, <code>false</code> if it should be hidden
	 */
	private void showChangeRuler(boolean show) {
		IVerticalRuler v= getVerticalRuler();
		if (v instanceof CompositeRuler) {
			CompositeRuler c= (CompositeRuler) v;
			if (show && fChangeRulerColumn == null)
				c.addDecorator(1, createChangeRulerColumn());
			else if (!show && fChangeRulerColumn != null) {
				c.removeDecorator(fChangeRulerColumn);
				fChangeRulerColumn= null;
			}
		}
	}

	/**
	 * Shows the line number ruler column.
	 */
	private void showLineNumberRuler() {
		showChangeRuler(false);
		if (fLineNumberRulerColumn == null) {
			IVerticalRuler v= getVerticalRuler();
			if (v instanceof CompositeRuler) {
				CompositeRuler c= (CompositeRuler) v;
				c.addDecorator(1, createLineNumberRulerColumn());
			}
		}
	}
	
	/**
	 * Hides the line number ruler column.
	 */
	private void hideLineNumberRuler() {
		if (fLineNumberRulerColumn != null) {
			IVerticalRuler v= getVerticalRuler();
			if (v instanceof CompositeRuler) {
				CompositeRuler c= (CompositeRuler) v;
				c.removeDecorator(fLineNumberRulerColumn);
			}
			fLineNumberRulerColumn = null;
		}
		if (fIsChangeInformationShown)
			showChangeRuler(true);
	}
	
	/**
	 * Returns whether the line number ruler column should be 
	 * visible according to the preference store settings. Subclasses may override this
	 * method to provide a custom preference setting.
	 * 
	 * @return <code>true</code> if the line numbers should be visible
	 */
	protected boolean isLineNumberRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store != null ? store.getBoolean(LINE_NUMBER_RULER) : false;
	}

	/**
	 * Returns whether quick diff info should be visible upon opening an editor 
	 * according to the preference store settings.
	 * 
	 * @return <code>true</code> if the line numbers should be visible
	 */
	protected boolean isPrefQuickDiffAlwaysOn() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON);
	}
	
	/**
	 * Initializes the given line number ruler column from the preference store.
	 * 
	 * @param rulerColumn the ruler column to be initialized
	 */
	protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
		ISharedTextColors sharedColors= getSharedColors();
		IPreferenceStore store= getPreferenceStore();
		if (store != null) {
		
			RGB rgb=  null;
			// foreground color
			if (store.contains(LINE_NUMBER_COLOR)) {
				if (store.isDefault(LINE_NUMBER_COLOR))
					rgb= PreferenceConverter.getDefaultColor(store, LINE_NUMBER_COLOR);
				else
					rgb= PreferenceConverter.getColor(store, LINE_NUMBER_COLOR);
			}
			if (rgb == null)
				rgb= new RGB(0, 0, 0);
			rulerColumn.setForeground(sharedColors.getColor(rgb));
			
			
			rgb= null;
			// background color: same as editor, or system default
			if (!store.getBoolean(PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
				if (store.contains(PREFERENCE_COLOR_BACKGROUND)) {
					if (store.isDefault(PREFERENCE_COLOR_BACKGROUND))
						rgb= PreferenceConverter.getDefaultColor(store, PREFERENCE_COLOR_BACKGROUND);
					else
						rgb= PreferenceConverter.getColor(store, PREFERENCE_COLOR_BACKGROUND);
				}
			}
			rulerColumn.setBackground(sharedColors.getColor(rgb));			
			
			rulerColumn.redraw();
		}
	}
	
	/**
	 * Initializes the given change ruler column from the preference store.
	 * 
	 * @param changeColumn the ruler column to be initialized
	 */
	private void initializeChangeRulerColumn(IChangeRulerColumn changeColumn) {
		ISharedTextColors sharedColors= getSharedColors();
		IPreferenceStore store= getPreferenceStore();
	
		if (store != null) {
			ISourceViewer v= getSourceViewer();
			if (v != null && v.getAnnotationModel() != null) {
				changeColumn.setModel(v.getAnnotationModel());
			}
			
			Iterator iter= fAnnotationPreferences.getAnnotationPreferences().iterator();
			while (iter.hasNext()) {
				AnnotationPreference pref= (AnnotationPreference) iter.next();
				
				if ("org.eclipse.quickdiff.changeindication".equals(pref.getMarkerType())) { //$NON-NLS-1$
					RGB rgb= getColorPreference(store, pref);
					changeColumn.setChangedColor(sharedColors.getColor(rgb));
				} else if ("org.eclipse.quickdiff.additionindication".equals(pref.getMarkerType())) { //$NON-NLS-1$
					RGB rgb= getColorPreference(store, pref);
					changeColumn.setAddedColor(sharedColors.getColor(rgb));
				} else if ("org.eclipse.quickdiff.deletionindication".equals(pref.getMarkerType())) { //$NON-NLS-1$
					RGB rgb= getColorPreference(store, pref);
					changeColumn.setDeletedColor(sharedColors.getColor(rgb));
				}
			}
			
			RGB rgb= null;
			// background color: same as editor, or system default
			if (!store.getBoolean(PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
				if (store.contains(PREFERENCE_COLOR_BACKGROUND)) {
					if (store.isDefault(PREFERENCE_COLOR_BACKGROUND))
						rgb= PreferenceConverter.getDefaultColor(store, PREFERENCE_COLOR_BACKGROUND);
					else
						rgb= PreferenceConverter.getColor(store, PREFERENCE_COLOR_BACKGROUND);
				}
			}
			changeColumn.setBackground(sharedColors.getColor(rgb));			

			if (changeColumn instanceof LineNumberChangeRulerColumn) {
				LineNumberChangeRulerColumn lncrc= (LineNumberChangeRulerColumn) changeColumn;
				lncrc.setDisplayMode(store.getBoolean(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_CHARACTER_MODE));
			}
		}
	
		changeColumn.redraw();
	}

	/**
	 * Extracts the color preference for <code>pref</code> from <code>store</code>. If <code>store</code>
	 * indicates that the default value is to be used, or the value stored in the preferences store is <code>null</code>,
	 * the value is taken from the <code>AnnotationPreference</code>'s default color value.
	 * 
	 * <p>The return value is never <code>null</code></p>
	 * 
	 * @param store
	 * @param pref
	 * @return
	 */
	private RGB getColorPreference(IPreferenceStore store, AnnotationPreference pref) {
		RGB rgb= null;
		if (store.contains(pref.getColorPreferenceKey())) {
			if (store.isDefault(pref.getColorPreferenceKey()))
				rgb= pref.getColorPreferenceValue();
			else
				rgb= PreferenceConverter.getColor(store, pref.getColorPreferenceKey());
		}
		if (rgb == null)
			rgb= pref.getColorPreferenceValue();
		return rgb;
	}

	/**
	 * Creates a new line number ruler column that is appropriately initialized.
	 */
	protected IVerticalRulerColumn createLineNumberRulerColumn() {
		if (isPrefQuickDiffAlwaysOn() || isChangeInformationShowing()) {
			LineNumberChangeRulerColumn column= new LineNumberChangeRulerColumn(getSharedColors());
			column.setHover(createChangeHover());
			initializeChangeRulerColumn(column);
			fLineNumberRulerColumn= column;
		} else {
			fLineNumberRulerColumn= new LineNumberRulerColumn();
		}
		initializeLineNumberRulerColumn(fLineNumberRulerColumn);
		return fLineNumberRulerColumn;
	}
	
	/**
	 * Creates and returns a <code>LineChangeHover</code> to be used on this editor's change
	 * ruler column. This default implementation returns a plain <code>LineChangeHover</code>.
	 * Subclasses may override.
	 * 
	 * @return the change hover to be used by this editors quick diff display
	 */
	protected LineChangeHover createChangeHover() {
		return new LineChangeHover();
	}

	/**
	 * Creates a new change ruler column for quick diff display independent of the 
	 * line number ruler column
	 * 
	 * @return a new change ruler column
	 */
	protected IChangeRulerColumn createChangeRulerColumn() {
		IChangeRulerColumn column= new ChangeRulerColumn();
		column.setHover(createChangeHover());
		fChangeRulerColumn= column;
		initializeChangeRulerColumn(fChangeRulerColumn);
		return fChangeRulerColumn;
	}
	
	/**
	 * Returns {@link #createCompositeRuler()}. Subclasses should not override this method, but
	 * rather <code>createCompositeRuler</code> if they want to contribute their own vertical ruler
	 * implementation. If not an instance of {@link CompositeRuler} is returned, the built-in ruler
	 * columns (line numbers, annotations) will not work.
	 * 
	 * <p>May become <code>final</code> in the future.</p>
	 * 
	 * @see AbstractTextEditor#createVerticalRuler()
	 */
	protected IVerticalRuler createVerticalRuler() {
		CompositeRuler ruler= createCompositeRuler();
		if (ruler != null) {
			for (Iterator iter=  ruler.getDecoratorIterator(); iter.hasNext();) {
				IVerticalRulerColumn column= (IVerticalRulerColumn)iter.next();
				if (column instanceof AnnotationRulerColumn) {
					fAnnotationRulerColumn= (AnnotationRulerColumn)column;
					for (Iterator iter2= fAnnotationPreferences.getAnnotationPreferences().iterator(); iter2.hasNext();) {
						AnnotationPreference preference= (AnnotationPreference)iter2.next();
						String key= preference.getVerticalRulerPreferenceKey();
						boolean showAnnotation= true;
						if (key != null)
							showAnnotation= getPreferenceStore().getBoolean(key);
						if (showAnnotation)
							fAnnotationRulerColumn.addAnnotationType(preference.getAnnotationType());
					}
					fAnnotationRulerColumn.addAnnotationType(DefaultMarkerAnnotationAccess.UNKNOWN);
					break;
				}
			}
		}
		return ruler;
	}
	
	/**
	 * Creates a composite ruler to be used as the vertical ruler by this editor.
	 * Subclasses may re-implement this method.
	 *
	 * @return the vertical ruler
	 */
	protected CompositeRuler createCompositeRuler() {
		CompositeRuler ruler= new CompositeRuler();
		ruler.addDecorator(0, new AnnotationRulerColumn(VERTICAL_RULER_WIDTH, getAnnotationAccess()));
		
		if (isLineNumberRulerVisible())
			ruler.addDecorator(1, createLineNumberRulerColumn());
		else if (isPrefQuickDiffAlwaysOn())
			ruler.addDecorator(1, createChangeRulerColumn());
			
		return ruler;
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {			

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			String property= event.getProperty();	
			
			if (fSourceViewerDecorationSupport != null && fOverviewRuler != null && OVERVIEW_RULER.equals(property))  {
				if (isOverviewRulerVisible())
					showOverviewRuler();
				else
					hideOverviewRuler();
				return;
			}
			
			if (LINE_NUMBER_RULER.equals(property)) {
				if (isLineNumberRulerVisible())
					showLineNumberRuler();
				else
					hideLineNumberRuler();
				return;
			}

			if (fLineNumberRulerColumn != null
				&&	(LINE_NUMBER_COLOR.equals(property) 
				||	PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property)
				||	PREFERENCE_COLOR_BACKGROUND.equals(property))) {
					
				initializeLineNumberRulerColumn(fLineNumberRulerColumn);
			}
			
			if (fChangeRulerColumn != null
				&&	(LINE_NUMBER_COLOR.equals(property) 
				||	PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property)
				||	PREFERENCE_COLOR_BACKGROUND.equals(property))) {
					
				initializeChangeRulerColumn(fChangeRulerColumn);
			}
			
			if (fLineNumberRulerColumn instanceof LineNumberChangeRulerColumn
					&& ExtendedTextEditorPreferenceConstants.QUICK_DIFF_CHARACTER_MODE.equals(property)) {
				initializeChangeRulerColumn(getChangeColumn());
			}

			AnnotationPreference pref= getAnnotationPreference(property);
			if (pref != null) {
				IChangeRulerColumn column= getChangeColumn();
				if (column != null) {
					if ("org.eclipse.quickdiff.changeindication".equals(pref.getMarkerType()) //$NON-NLS-1$
							|| "org.eclipse.quickdiff.additionindication".equals(pref.getMarkerType()) //$NON-NLS-1$
							|| "org.eclipse.quickdiff.deletionindication".equals(pref.getMarkerType())) { //$NON-NLS-1$
						initializeChangeRulerColumn(column);
					}
				}
			}
			
			AnnotationPreference annotationPreference= getVerticalRulerAnnotationPreference(property);
			if (annotationPreference != null && event.getNewValue() instanceof Boolean) {
				Object type= annotationPreference.getAnnotationType();
				if (((Boolean)event.getNewValue()).booleanValue())
					fAnnotationRulerColumn.addAnnotationType(type);
				else
					fAnnotationRulerColumn.removeAnnotationType(type);
				getVerticalRuler().update();
			}
			
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/**
	 * Returns the <code>AnnotationPreference</code> corresponding to <code>colorKey</code>.
	 * 
	 * @param colorKey the color key.
	 * @return the corresponding <code>AnnotationPreference</code>
	 */
	private AnnotationPreference getAnnotationPreference(String colorKey) {
		for (Iterator iter= fAnnotationPreferences.getAnnotationPreferences().iterator(); iter.hasNext();) {
			AnnotationPreference pref= (AnnotationPreference) iter.next();
			if (colorKey.equals(pref.getColorPreferenceKey()))
				return pref;
		}
		return null;
	}
	
	/**
	 * Returns the annotation preference for which the given
	 * preference matches a vertical ruler preference key.
	 * 
	 * @param preferenceKey the preference key string
	 * @return the annotation preference or <code>null</code> if none
	 * @since 3.0
	 */
	private AnnotationPreference getVerticalRulerAnnotationPreference(String preferenceKey) {
		if (preferenceKey == null)
			return null;
		
		Iterator e= fAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference info= (AnnotationPreference) e.next();
			if (info != null && preferenceKey.equals(info.getVerticalRulerPreferenceKey())) 
				return info;
		}
		return null;
	}
	
	/**
	 * Shows the overview ruler.
	 */
	protected void showOverviewRuler() {
		if (fOverviewRuler != null) {
			if (getSourceViewer() instanceof ISourceViewerExtension) {
				((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(true);
				fSourceViewerDecorationSupport.updateOverviewDecorations();
			}
		}
	}

	/**
	 * Hides the overview ruler.
	 */
	protected void hideOverviewRuler() {
		if (getSourceViewer() instanceof ISourceViewerExtension) {
			fSourceViewerDecorationSupport.hideAnnotationOverview();
			((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(false);
		}
	}
	
	/**
	 * Returns the annotation access. 
	 * 
	 * @return the annotation access
	 */
	protected IAnnotationAccess getAnnotationAccess() {
		if (fAnnotationAccess == null)
			fAnnotationAccess= createAnnotationAccess();
		return fAnnotationAccess;
	}

	/**
	 * Returns the overview ruler.
	 * 
	 * @return the overview ruler
	 */
	protected IOverviewRuler getOverviewRuler() {
		if (fOverviewRuler == null)
			fOverviewRuler= createOverviewRuler(getSharedColors());
		return fOverviewRuler;
	}

	/**
	 * Returns the source viewer decoration support.
	 * 
	 * @return the source viewer decoration support
	 */
	protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
		if (fSourceViewerDecorationSupport == null) {
			fSourceViewerDecorationSupport= new SourceViewerDecorationSupport(viewer, getOverviewRuler(), getAnnotationAccess(), getSharedColors());
			configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
		}
		return fSourceViewerDecorationSupport;
	}

	/**
	 * Returns the annotation preferences.
	 * 
	 * @return the annotation preferences
	 */
	protected MarkerAnnotationPreferences getAnnotationPreferences() {
		return fAnnotationPreferences;
	}
	
	
	/**
	 * If the editor can be saved all marker ranges have been changed according to
	 * the text manipulations. However, those changes are not yet propagated to the
	 * marker manager. Thus, when opening a marker, the marker's position in the editor
	 * must be determined as it might differ from the position stated in the marker.
	 * 
	 * @param marker the marker to go to
	 * @see EditorPart#gotoMarker(org.eclipse.core.resources.IMarker)
	 */
	public void gotoMarker(IMarker marker) {
		
		if (getSourceViewer() == null)
			return;
		
		int start= MarkerUtilities.getCharStart(marker);
		int end= MarkerUtilities.getCharEnd(marker);
		
		if (start < 0 || end < 0) {
			
			// there is only a line number
			int line= MarkerUtilities.getLineNumber(marker);
			if (line > -1) {
				
				// marker line numbers are 1-based
				-- line;
				
				try {
					
					IDocument document= getDocumentProvider().getDocument(getEditorInput());
					selectAndReveal(document.getLineOffset(line), document.getLineLength(line));
					
				} catch (BadLocationException x) {
					// marker refers to invalid text position -> do nothing
				}
			}
			
		} else {
			
			// look up the current range of the marker when the document has been edited
			IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
			if (model instanceof AbstractMarkerAnnotationModel) {
				
				AbstractMarkerAnnotationModel markerModel= (AbstractMarkerAnnotationModel) model;
				Position pos= markerModel.getMarkerPosition(marker);
				if (pos != null && !pos.isDeleted()) {
					// use position instead of marker values
					start= pos.getOffset();
					end= pos.getOffset() + pos.getLength();
				}
				
				if (pos != null && pos.isDeleted()) {
					// do nothing if position has been deleted
					return;
				}
			}
			
			IDocument document= getDocumentProvider().getDocument(getEditorInput());
			int length= document.getLength();
			if (end - 1 < length && start < length)
				selectAndReveal(start, end - start);
		}
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#isErrorStatus(org.eclipse.core.runtime.IStatus)
	 * @since 3.0
	 */
	protected boolean isErrorStatus(IStatus status) {
		// see bug 42230
		return status != null && !status.isOK() && status.getCode() != IResourceStatus.READ_ONLY_LOCAL;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();
		
		ResourceAction action= new AddMarkerAction(TextEditorMessages.getResourceBundle(), "Editor.AddBookmark.", this, IMarker.BOOKMARK, true); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.BOOKMARK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_BOOKMARK);
		setAction(ITextEditorActionConstants.BOOKMARK, action);

		action= new AddTaskAction(TextEditorMessages.getResourceBundle(), "Editor.AddTask.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.ADD_TASK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
		setAction(ITextEditorActionConstants.ADD_TASK, action);		
	}
	
	/*
	 * @see IAdaptable#getAdapter(java.lang.Class)
	 * @since 3.0
	 */
	public Object getAdapter(Class adapter) {
		if (IGotoMarker.class.equals(adapter))
			return fGotoMarkerAdapter;
		return super.getAdapter(adapter);
	}
	
	/*
	 * If there is no explicit document provider set, the implicit one is
	 * re-initialized based on the given editor input.
	 *
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#setDocumentProvider(org.eclipse.ui.IEditorInput)
	 * @since 3.0
	 */
	protected void setDocumentProvider(IEditorInput input) {
		fImplicitDocumentProvider= DocumentProviderRegistry.getDefault().getDocumentProvider(input);		
	}

	/*
	 * @see org.eclipse.ui.texteditor.ITextEditor#getDocumentProvider()
	 * @since 3.0
	 */
	public IDocumentProvider getDocumentProvider() {
		IDocumentProvider provider= super.getDocumentProvider();
		if (provider == null)
			return fImplicitDocumentProvider;
		return provider;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#disposeDocumentProvider()
	 * @since 3.0
	 */
	protected void disposeDocumentProvider() {
		super.disposeDocumentProvider();
		fImplicitDocumentProvider= null;
	}
}
