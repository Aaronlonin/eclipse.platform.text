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

import org.eclipse.swt.graphics.RGB;


/**
 * An annotation preference provides all the information required for handing the preferences for the presentation of annotations of a specified type.
 * The provided information covers:
 * <ul>
 * <li> the preference key for the presentation  color
 * <li> the default presentation color
 * <li> the preference key for the visibility of annotations inside text
 * <li> the default visibility of annotations inside text
 * <li> the preference key for the visibility of annotations inside the overview ruler
 * <li> the default visibility of annotations inside the overview ruler
 * <li> the presentation layer
 * <li> how the annotation type should be presented on a preference page
 * <li> whether the annotation type should be presented in the header of the overview ruler
 * <li> the marker type if the annotation type is derived from an <code>IMarker</code>
 * <li> the severity of the marker if the annotation type is derived from an <code>IMarker</code>
 * <li> the preference key for the visibility in the next/previous navigation toolbar drop down action
 * <li> the default value for the visibility in the next/previous navigation toolbar drop down action
 * <li> the preference key for go to next navigation enablement
 * <li> the default value for go to next navigation enablement
 * <li> the preference key for got to previous navigation enablement
 * <li> the default value for got to previous navigation enablement
 * </ul>
 * 
 * @since 2.1
 */
public class AnnotationPreference {
	
	/** The annotation type */
	private Object fAnnotationType;
	/** The preference label */
	private String fPreferenceLabel;
	/** The marker type */
	private String fMarkerType;
	/** The marker severity */
	private int fSeverity;
	/** The preference key for the presentation color */
	private String fColorKey;
	/** The default presentation color */
	private RGB fColorValue;
	/** The preference key for the visibility inside text */
	private String fTextKey;
	/** The default visibility inside text */
	private boolean fTextValue;
	/**
	 * The preference key for highlighting inside text.
	 * @since 3.0
	 */
	private String fHighlightKey;
	/**
	 * The default value for highlighting inside text.
	 * @since 3.0
	 */
	private boolean fHighlightValue;
	/** The preference key for the visibility in the overview ruler */
	private String fOverviewRulerKey;
	/** The default visibility in the overview ruler */
	private boolean fOverviewRulerValue;
	/**
	 * The preference key for the visibility in the vertical ruler.
	 * @since 3.0
	 */
	private String fVerticalRulerKey;
	/**
	 * The default visibility in the vertical ruler.
	 * @since 3.0
	 */
	private boolean fVerticalRulerValue;
	/** The presentation layer */
	private int fPresentationLayer;
	/** Indicates whether the annotation type contributed to the overview ruler's header */
	private boolean fContributesToHeader;
	/**
	 * The preference key for the visibility in the next/previous drop down toolbar action.
	 * @since 3.0
	 */
	private String fShowInNextPrevDropdownToolbarActionKey;
	/**
	 * The default value for the visibility in the next/previous drop down toolbar action.
	 * @since 3.0
	 */
	private boolean fShowInNextPrevDropdownToolbarAction;
	/**
	 * The preference key for go to next navigation enablement.
	 * @since 3.0
	 */
	private String fIsGoToNextNavigationTargetKey;
	/**
	 * The default value for go to next navigation enablement.
	 * @since 3.0
	 */
	private boolean fIsGoToNextNavigationTarget;
	/**
	 * The preference key for go to previous navigation enablement.
	 * @since 3.0
	 */
	private String fIsGoToPreviousNavigationTargetKey;
	/**
	 * The default value for go to previous navigation enablement.
	 * @since 3.0
	 */
	private boolean fIsGoToPreviousNavigationTarget;
	

	/**
	 * Creates a new uninitialized annotation preference.
	 */
	public AnnotationPreference() {
	}
		
	/**
	 * Creates a new annotation preference for the given annotation type.
	 * 
	 * @param annotationType the annotation type
	 * @param colorKey the preference key for the presentation color
	 * @param textKey the preference key for the visibility inside text
	 * @param overviewRulerKey the preference key for the visibility in the overview ruler
	 * @param presentationLayer the presentation layer
	 */
	public AnnotationPreference(Object annotationType, String colorKey, String textKey, String overviewRulerKey, int presentationLayer) {
		fAnnotationType= annotationType;
		fColorKey= colorKey;
		fTextKey= textKey;
		fOverviewRulerKey= overviewRulerKey;
		fPresentationLayer= presentationLayer;
	}
	
	/**
	 * Returns whether the given string is a preference key.
	 * 
	 * @param key the string to test
	 * @return <code>true</code> if the string is a preference key
	 */
	public boolean isPreferenceKey(String key) {
		if (key == null)
			return false;
		return key.equals(fColorKey) || key.equals(fOverviewRulerKey) || key.equals(fTextKey) || key.equals(fHighlightKey) || key.equals(fVerticalRulerKey);
	}
	
	/**
	 * Returns the annotation type.
	 * 
	 * @return the annotation type
	 */
	public Object getAnnotationType() {
		return fAnnotationType;
	}
	
	/**
	 * Returns the marker type.
	 * 
	 * @return the marker type
	 */
	public String getMarkerType() {
		return fMarkerType;
	}

	/**
	 * Returns the preference key for the presentation color.
	 * 
	 * @return the preference key for the presentation color
	 */
	public String getColorPreferenceKey() {
		return fColorKey;
	}
	
	/**
	 * Returns the default presentation color.
	 * 
	 * @return the default presentation color.
	 */
	public RGB getColorPreferenceValue() {
		return fColorValue;
	}
	
	/**
	 * Returns the presentation string for this annotation type.
	 * 
	 * @return the presentation string for this annotation type
	 */
	public String getPreferenceLabel() {
		return fPreferenceLabel;
	}

	/**
	 * Returns the preference key for the visibility in the overview ruler.
	 * 
	 * @return the preference key for the visibility in the overview ruler
	 */
	public String getOverviewRulerPreferenceKey() {
		return fOverviewRulerKey;
	}
	
	/**
	 * Returns the default visibility in the overview ruler.
	 * 
	 * @return the default visibility in the overview ruler
	 */
	public boolean getOverviewRulerPreferenceValue() {
		return fOverviewRulerValue;
	}
	
	/**
	 * Returns the preference key for the visibility in the vertical ruler.
	 * 
	 * @return the preference key for the visibility in the vertical ruler
	 * @since 3.0
	 */
	public String getVerticalRulerPreferenceKey() {
		return fVerticalRulerKey;
	}
	
	/**
	 * Returns the default visibility in the vertical ruler.
	 * 
	 * @return the default visibility in the vertical ruler
	 * @since 3.0
	 */
	public boolean getVerticalRulerPreferenceValue() {
		return fVerticalRulerValue;
	}
	
	/**
	 * Returns the presentation layer.
	 * 
	 * @return the presentation layer
	 */
	public int getPresentationLayer() {
		return fPresentationLayer;
	}

	/**
	 * Returns the marker severity.
	 * 
	 * @return the marker severity
	 */
	public int getSeverity() {
		return fSeverity;
	}

	/**
	 * Returns the preference key for the visibility inside text.
	 * 
	 * @return the preference key for the visibility inside text
	 */
	public String getTextPreferenceKey() {
		return fTextKey;
	}

	/**
	 * Returns the default visibility inside text.
	 * 
	 * @return the default visibility inside text
	 */
	public boolean getTextPreferenceValue() {
		return fTextValue;
	}
	
	/**
	 * Returns the preference key for highlighting inside text.
	 * 
	 * @return the preference key for highlighting inside text
	 * @since 3.0
	 */
	public String getHighlightPreferenceKey() {
		return fHighlightKey;
	}

	/**
	 * Returns the default value for highlighting inside text.
	 * 
	 * @return the default value for highlighting inside text
	 * @since 3.0
	 */
	public boolean getHighlightPreferenceValue() {
		return fHighlightValue;
	}
		
	/**
	 * Returns whether the annotation type contributes to the header of the overview ruler.
	 * 
	 * @return <code>true</code> if the annotation type contributes to the header of the overview ruler
	 */
	public boolean contributesToHeader() {
		return fContributesToHeader;
	}
	
	/**
	 * Sets the annotation type.
	 * 
	 * @param annotationType the annotation type
	 */
	public void setAnnotationType(Object annotationType) {
		fAnnotationType= annotationType;
	}
	
	/**
	 * Sets the marker type.
	 * 
	 * @param markerType the marker type
	 */
	public void setMarkerType(String markerType) {
		fMarkerType= markerType;
	}
	
	/**
	 * Sets the preference key for the presentation color.
	 * 
	 * @param colorKey the preference key
	 */
	public void setColorPreferenceKey(String colorKey) {
		fColorKey= colorKey;
	}

	/**
	 * Sets the default presentation color.
	 * 
	 * @param colorValue the default color
	 */
	public void setColorPreferenceValue(RGB colorValue) {
		fColorValue= colorValue;
	}
	
	/**
	 * Sets the presentation label of this annotation type.
	 * 
	 * @param label the presentation label
	 */
	public void setPreferenceLabel(String label) {
		fPreferenceLabel= label;
	}

	/**
	 * Sets the preference key for the visibility in the overview ruler.
	 * 
	 * @param overviewRulerKey the preference key
	 */
	public void setOverviewRulerPreferenceKey(String overviewRulerKey) {
		fOverviewRulerKey= overviewRulerKey;
	}
	
	/**
	 * Sets the default visibility in the overview ruler.
	 * 
	 * @param overviewRulerValue <code>true</code> if visible by default, <code>false</code> otherwise
	 */
	public void setOverviewRulerPreferenceValue(boolean overviewRulerValue) {
		fOverviewRulerValue= overviewRulerValue;
	}
	
	/**
	 * Sets the preference key for the visibility in the vertical ruler.
	 * 
	 * @param verticalRulerKey the preference key
	 * @since 3.0
	 */
	public void setVerticalRulerPreferenceKey(String verticalRulerKey) {
		fVerticalRulerKey= verticalRulerKey;
	}
	
	/**
	 * Sets the default visibility in the vertical ruler.
	 * 
	 * @param verticalRulerValue <code>true</code> if visible by default, <code>false</code> otherwise
	 * @since 3.0
	 */
	public void setVerticalRulerPreferenceValue(boolean verticalRulerValue) {
		fVerticalRulerValue= verticalRulerValue;
	}
	
	/**
	 * Sets the presentation layer.
	 * 
	 * @param presentationLayer the presentation layer
	 */
	public void setPresentationLayer(int presentationLayer) {
		fPresentationLayer= presentationLayer;
	}
	
	/**
	 * Sets the marker serverity.
	 * 
	 * @param severity the marker severity
	 */
	public void setSeverity(int severity) {
		fSeverity= severity;
	}

	/**
	 * Sets the preference key for the visibility of squiggles inside text.
	 * 
	 * @param textKey the preference key
	 */
	public void setTextPreferenceKey(String textKey) {
		fTextKey= textKey;
	}

	/**
	 * Sets the default visibility inside text.
	 * 
	 * @param textValue <code>true</code> if visible by default, <code>false</code> otherwise
	 */
	public void setTextPreferenceValue(boolean textValue) {
		fTextValue= textValue;
	}
	
	/**
	 * Sets the preference key for highlighting inside text.
	 * 
	 * @param highlightKey the preference key
	 * @since 3.0
	 */
	public void setHighlightPreferenceKey(String highlightKey) {
		fHighlightKey= highlightKey;
	}
	
	/**
	 * Sets the default value for highlighting inside text.
	 * 
	 * @param highlightValue <code>true</code> if highlighted in text by default, <code>false</code> otherwise
	 * @since 3.0
	 */
	public void setHighlightPreferenceValue(boolean highlightValue) {
		fHighlightValue= highlightValue;
	}
	
	/**
	 * Sets whether the annotation type contributes to the overview ruler's header.
	 * 
	 * @param contributesToHeader <code>true</code> if in header, <code>false</code> otherwise
	 */
	public void setContributesToHeader(boolean contributesToHeader) {
		fContributesToHeader= contributesToHeader;
	}
	/**
	 * Returns the default value for go to next navigation enablement.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isGoToNextNavigationTarget() {
		return fIsGoToNextNavigationTarget;
	}

	/**
	 * Sets the default value for go to next navigation enablement.
	 * 
	 * @param isGoToNextNavigationTarget <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToNextNavigationTarget(boolean isGoToNextNavigationTarget) {
		fIsGoToNextNavigationTarget= isGoToNextNavigationTarget;
	}

	/**
	 * Returns the preference key for go to next navigation enablement.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getIsGoToNextNavigationTargetKey() {
		return fIsGoToNextNavigationTargetKey;
	}

	/**
	 * Sets the preference key for go to next navigation enablement.
	 * 
	 * @param isGoToNextNavigationTargetKey <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToNextNavigationTargetKey(String isGoToNextNavigationTargetKey) {
		fIsGoToNextNavigationTargetKey= isGoToNextNavigationTargetKey;
	}

	/**
	 * Returns the default value for go to previous navigation enablement.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isGoToPreviousNavigationTarget() {
		return fIsGoToPreviousNavigationTarget;
	}

	/**
	 * Sets the default value for go to previous navigation enablement.
	 * 
	 * @param isGoToPreviousNavigationTarget <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToPreviousNavigationTarget(boolean isGoToPreviousNavigationTarget) {
		fIsGoToPreviousNavigationTarget= isGoToPreviousNavigationTarget;
	}

	/**
	 * Returns the preference key for go to previous navigation enablement.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getIsGoToPreviousNavigationTargetKey() {
		return fIsGoToPreviousNavigationTargetKey;
	}

	/**
 	 * Sets the preference key for go to previous navigation enablement.
 	 * 
 	 * @param isGoToPreviousNavigationTargetKey the preference key
	 * @since 3.0
	 */
	public void setIsGoToPreviousNavigationTargetKey(String isGoToPreviousNavigationTargetKey) {
		fIsGoToPreviousNavigationTargetKey= isGoToPreviousNavigationTargetKey;
	}

	/**	
	 * Returns the preference key for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getShowInNextPrevDropdownToolbarActionKey() {
		return fShowInNextPrevDropdownToolbarActionKey;
	}

	/**
	 * Sets the preference key for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @param showInNextPrevDropdownToolbarActionKey the preference key
	 * @since 3.0
	 */
	public void setShowInNextPrevDropdownToolbarActionKey(String showInNextPrevDropdownToolbarActionKey) {
		fShowInNextPrevDropdownToolbarActionKey= showInNextPrevDropdownToolbarActionKey;
	}
	
	/**
	 * Returns the default value for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isShowInNextPrevDropdownToolbarAction() {
		return fShowInNextPrevDropdownToolbarAction;
	}

	/**
	 * Sets the default value for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @param showInNextPrevDropdownToolbarAction <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setShowInNextPrevDropdownToolbarAction(boolean showInNextPrevDropdownToolbarAction) {
		fShowInNextPrevDropdownToolbarAction= showInNextPrevDropdownToolbarAction;
	}
}
