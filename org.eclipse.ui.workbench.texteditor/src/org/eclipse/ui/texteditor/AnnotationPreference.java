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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.resource.ImageDescriptor;


import org.eclipse.ui.internal.texteditor.TextEditorPlugin;


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
 * <li> the image descriptor
 * <li> the symbolic image name
 * </ul>
 * 
 * @since 2.1
 */
public class AnnotationPreference {
	
	/** IDs for presentation preference attributes */
	
	/** The image to be used for drawing in the vertical ruler. */ 
	protected final static Object IMAGE_DESCRIPTOR= new Object();
	/** The preference label */
	protected final static Object PREFERENCE_LABEL= new Object();
	/** The presentation layer */
	protected final static Object PRESENTATION_LAYER= new Object();
	/** The symbolic name of the image to be drawn in the vertical ruler. */
	protected final static Object SYMBOLIC_IMAGE_NAME= new Object();
	/** Indicates whether the annotation type contributed to the overview ruler's header */
	protected final static Object HEADER_VALUE= new Object();
	/** The annotation image provider. */
	protected final static Object IMAGE_PROVIDER= new Object();
	
	/** IDs for preference store access and initialization */
	
	/** The preference key for the visibility inside text */
	protected final static Object TEXT_PREFERENCE_KEY= new Object();
	/** The visibility inside text */
	protected final static Object TEXT_PREFERENCE_VALUE= new Object();
	/** The preference key for the presentation color */
	protected final static Object COLOR_PREFERENCE_KEY= new Object();
	/** The presentation color */
	protected final static Object COLOR_PREFERENCE_VALUE= new Object();
	/** The preference key for highlighting inside text. */
	protected final static Object HIGHLIGHT_PREFERENCE_KEY= new Object();
	/** The value for highlighting inside text. */
	protected final static Object HIGHLIGHT_PREFERENCE_VALUE= new Object();
	/** The preference key for go to next navigation enablement. */
	protected final static Object IS_GO_TO_NEXT_TARGET_KEY= new Object();
	/** The value for go to next navigation enablement. */
	protected final static Object IS_GO_TO_NEXT_TARGET_VALUE= new Object();
	/** The preference key for go to previous navigation enablement. */
	protected final static Object IS_GO_TO_PREVIOUS_TARGET_KEY= new Object();
	/** The value for go to previous navigation enablement. */
	protected final static Object IS_GO_TO_PREVIOUS_TARGET_VALUE= new Object();
	/** The preference key for the visibility in the vertical ruler. */
	protected final static Object VERTICAL_RULER_PREFERENCE_KEY= new Object();
	/** The visibility in the vertical ruler. */
	protected final static Object VERTICAL_RULER_PREFERENCE_VALUE= new Object();
	/** The preference key for the visibility in the overview ruler */
	protected final static Object OVERVIEW_RULER_PREFERENCE_KEY= new Object();
	/** The visibility in the overview ruler */
	protected final static Object OVERVIEW_RULER_PREFERENCE_VALUE= new Object();
	/** The preference key for the visibility in the next/previous drop down toolbar action. */
	protected final static Object SHOW_IN_NAVIGATION_DROPDOWN_KEY= new Object();
	/** The value for the visibility in the next/previous drop down toolbar action. */
	protected final static Object SHOW_IN_NAVIGATION_DROPDOWN_VALUE= new Object();
	
	/**
	 * Array of all supported attributes.
	 */
	protected final static Object[] ATTRIBUTES= new Object[] {
			IMAGE_DESCRIPTOR, 
			PREFERENCE_LABEL,
			PRESENTATION_LAYER,
			SYMBOLIC_IMAGE_NAME,
			HEADER_VALUE,
			IMAGE_PROVIDER,
			TEXT_PREFERENCE_KEY,
			TEXT_PREFERENCE_VALUE,
			COLOR_PREFERENCE_KEY,
			COLOR_PREFERENCE_VALUE,
			HIGHLIGHT_PREFERENCE_KEY,
			HIGHLIGHT_PREFERENCE_VALUE,
			IS_GO_TO_NEXT_TARGET_KEY,
			IS_GO_TO_NEXT_TARGET_VALUE,
			IS_GO_TO_PREVIOUS_TARGET_KEY,
			IS_GO_TO_PREVIOUS_TARGET_VALUE,
			VERTICAL_RULER_PREFERENCE_KEY,
			VERTICAL_RULER_PREFERENCE_VALUE,
			OVERVIEW_RULER_PREFERENCE_KEY,
			OVERVIEW_RULER_PREFERENCE_VALUE,
			SHOW_IN_NAVIGATION_DROPDOWN_KEY,
			SHOW_IN_NAVIGATION_DROPDOWN_VALUE
	};
	
	/** The annotation type */
	private Object fAnnotationType;
	/** The marker type */
	private String fMarkerType;
	/** The marker severity */
	private int fSeverity;
	/**
	 * The annotation image provider.
	 * @since 3.0
	 */
	public IAnnotationImageProvider fAnnotationImageProvider;
	/**
	 * The configuration element from which to create the annotation image provider.
	 * @since 3.0
	 */
	public IConfigurationElement fConfigurationElement;
	/**
	 * The name of the attribute used to load the annotation image provider
	 * from the configuration element.
	 * @since 3.0
	 */
	public String fAnnotationImageProviderAttribute;
	/**
	 * The map of attributes.
	 * @since 3.0
	 */
	private Map fAttributes= new HashMap();
	
	

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
		setValue(COLOR_PREFERENCE_KEY, colorKey);
		setValue(TEXT_PREFERENCE_KEY, textKey);
		setValue(OVERVIEW_RULER_PREFERENCE_KEY, overviewRulerKey);
		setValue(PRESENTATION_LAYER, presentationLayer);
	}
	
	/**
	 * Sets the given value for the given attribute.
	 * 
	 * @param attribute the attribute
	 * @param value the attribute value
	 * @since 3.0
	 */
	protected void setValue(Object attribute, Object value) {
		fAttributes.put(attribute, value);
	}
	
	/**
	 * Sets the given value for the given attribute.
	 * 
	 * @param attribute the attribute
	 * @param value the attribute value
	 * @since 3.0
	 */
	protected void setValue(Object attribute, int value) {
		fAttributes.put(attribute, new Integer(value));
	}
	
	/**
	 * Sets the given value for the given attribute.
	 * 
	 * @param attribute the attribute
	 * @param value the attribute value
	 * @since 3.0
	 */
	protected void setValue(Object attribute, boolean value) {
		fAttributes.put(attribute, value ? Boolean.TRUE : Boolean.FALSE);
	}
	
	/**
	 * Returns the value of the given attribute as string.
	 * 
	 * @param attribute the attribute
	 * @return the attribute value
	 * @since 3.0
	 */
	protected String getStringValue(Object attribute) {
		Object value= fAttributes.get(attribute);
		if (value instanceof String)
			return (String) value;
		return null;
	}
	
	/**
	 * Returns the value of the given attribute as boolean.
	 * 
	 * @param attribute the attribute
	 * @return the attribute value
	 * @since 3.0
	 */
	protected boolean getBooleanValue(Object attribute) {
		Object value= fAttributes.get(attribute);
		if (value instanceof Boolean)
			return ((Boolean) value).booleanValue();
		return false;
	}

	/**
	 * Returns the value of the given attribute as integer.
	 * 
	 * @param attribute the attribute
	 * @return the attribute value
	 * @since 3.0
	 */
	protected int getIntegerValue(Object attribute) {
		Object value= fAttributes.get(attribute);
		if (value instanceof Integer)
			return ((Integer) value).intValue();
		return 0;
	}
	
	/**
	 * Returns the value of the given attribute.
	 * 
	 * @param attribute the attribute
	 * @return the attribute value
	 * @since 3.0
	 */
	public Object getValue(Object attribute) {
		return fAttributes.get(attribute);
	}
	
	/**
	 * Returns whether the given attribute is defined.
	 * 
	 * @param attribute the attribute
	 * @return <code>true</code> if the attribute has a value <code>false</code> otherwise
	 * @since 3.0
	 */
	public boolean hasValue(Object attribute) {
		return fAttributes.get(attribute) != null;
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
		
		return key.equals(getStringValue(COLOR_PREFERENCE_KEY)) || 
				key.equals(getStringValue(OVERVIEW_RULER_PREFERENCE_KEY)) || 
				key.equals(getStringValue(TEXT_PREFERENCE_KEY)) || 
				key.equals(getStringValue(HIGHLIGHT_PREFERENCE_KEY)) || 
				key.equals(getStringValue(VERTICAL_RULER_PREFERENCE_KEY));
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
	 * @deprecated since 3.0
	 */
	public String getMarkerType() {
		return fMarkerType;
	}
	
	/**
	 * Returns the marker severity.
	 * 
	 * @return the marker severity
	 * @deprecated since 3.0
	 */
	public int getSeverity() {
		return fSeverity;
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
	 * Sets the marker serverity.
	 * 
	 * @param severity the marker severity
	 */
	public void setSeverity(int severity) {
		fSeverity= severity;
	}

	/**
	 * Returns the preference key for the presentation color.
	 * 
	 * @return the preference key for the presentation color
	 */
	public String getColorPreferenceKey() {
		return getStringValue(COLOR_PREFERENCE_KEY);
	}
	
	/**
	 * Returns the default presentation color.
	 * 
	 * @return the default presentation color.
	 */
	public RGB getColorPreferenceValue() {
		return (RGB) getValue(COLOR_PREFERENCE_VALUE);
	}
	
	/**
	 * Returns the presentation string for this annotation type.
	 * 
	 * @return the presentation string for this annotation type
	 */
	public String getPreferenceLabel() {
		return getStringValue(PREFERENCE_LABEL);
	}

	/**
	 * Returns the preference key for the visibility in the overview ruler.
	 * 
	 * @return the preference key for the visibility in the overview ruler
	 */
	public String getOverviewRulerPreferenceKey() {
		return getStringValue(OVERVIEW_RULER_PREFERENCE_KEY);
	}
	
	/**
	 * Returns the default visibility in the overview ruler.
	 * 
	 * @return the default visibility in the overview ruler
	 */
	public boolean getOverviewRulerPreferenceValue() {
		return getBooleanValue(OVERVIEW_RULER_PREFERENCE_VALUE);
	}
	
	/**
	 * Returns the preference key for the visibility in the vertical ruler.
	 * 
	 * @return the preference key for the visibility in the vertical ruler
	 * @since 3.0
	 */
	public String getVerticalRulerPreferenceKey() {
		return getStringValue(VERTICAL_RULER_PREFERENCE_KEY);
	}
	
	/**
	 * Returns the default visibility in the vertical ruler.
	 * 
	 * @return the default visibility in the vertical ruler
	 * @since 3.0
	 */
	public boolean getVerticalRulerPreferenceValue() {
		return getBooleanValue(VERTICAL_RULER_PREFERENCE_VALUE);
	}
	
	/**
	 * Returns the presentation layer.
	 * 
	 * @return the presentation layer
	 */
	public int getPresentationLayer() {
		return getIntegerValue(PRESENTATION_LAYER);
	}

	/**
	 * Returns the preference key for the visibility inside text.
	 * 
	 * @return the preference key for the visibility inside text
	 */
	public String getTextPreferenceKey() {
		return getStringValue(TEXT_PREFERENCE_KEY);
	}

	/**
	 * Returns the default visibility inside text.
	 * 
	 * @return the default visibility inside text
	 */
	public boolean getTextPreferenceValue() {
		return getBooleanValue(TEXT_PREFERENCE_VALUE);
	}
	
	/**
	 * Returns the preference key for highlighting inside text.
	 * 
	 * @return the preference key for highlighting inside text
	 * @since 3.0
	 */
	public String getHighlightPreferenceKey() {
		return getStringValue(HIGHLIGHT_PREFERENCE_KEY);
	}

	/**
	 * Returns the default value for highlighting inside text.
	 * 
	 * @return the default value for highlighting inside text
	 * @since 3.0
	 */
	public boolean getHighlightPreferenceValue() {
		return getBooleanValue(HIGHLIGHT_PREFERENCE_VALUE);
	}
		
	/**
	 * Returns whether the annotation type contributes to the header of the overview ruler.
	 * 
	 * @return <code>true</code> if the annotation type contributes to the header of the overview ruler
	 */
	public boolean contributesToHeader() {
		return getBooleanValue(HEADER_VALUE);
	}
	
	/**
	 * Sets the preference key for the presentation color.
	 * 
	 * @param colorKey the preference key
	 */
	public void setColorPreferenceKey(String colorKey) {
		setValue(COLOR_PREFERENCE_KEY, colorKey);
	}

	/**
	 * Sets the default presentation color.
	 * 
	 * @param colorValue the default color
	 */
	public void setColorPreferenceValue(RGB colorValue) {
		setValue(COLOR_PREFERENCE_VALUE, colorValue);
	}
	
	/**
	 * Sets the presentation label of this annotation type.
	 * 
	 * @param label the presentation label
	 */
	public void setPreferenceLabel(String label) {
		setValue(PREFERENCE_LABEL, label);
	}

	/**
	 * Sets the preference key for the visibility in the overview ruler.
	 * 
	 * @param overviewRulerKey the preference key
	 */
	public void setOverviewRulerPreferenceKey(String overviewRulerKey) {
		setValue(OVERVIEW_RULER_PREFERENCE_KEY, overviewRulerKey);
	}
	
	/**
	 * Sets the default visibility in the overview ruler.
	 * 
	 * @param overviewRulerValue <code>true</code> if visible by default, <code>false</code> otherwise
	 */
	public void setOverviewRulerPreferenceValue(boolean overviewRulerValue) {
		setValue(OVERVIEW_RULER_PREFERENCE_VALUE, overviewRulerValue);
	}
	
	/**
	 * Sets the preference key for the visibility in the vertical ruler.
	 * 
	 * @param verticalRulerKey the preference key
	 * @since 3.0
	 */
	public void setVerticalRulerPreferenceKey(String verticalRulerKey) {
		setValue(VERTICAL_RULER_PREFERENCE_KEY, verticalRulerKey);
	}
	
	/**
	 * Sets the default visibility in the vertical ruler.
	 * 
	 * @param verticalRulerValue <code>true</code> if visible by default, <code>false</code> otherwise
	 * @since 3.0
	 */
	public void setVerticalRulerPreferenceValue(boolean verticalRulerValue) {
		setValue(VERTICAL_RULER_PREFERENCE_VALUE, verticalRulerValue);
	}
	
	/**
	 * Sets the presentation layer.
	 * 
	 * @param presentationLayer the presentation layer
	 */
	public void setPresentationLayer(int presentationLayer) {
		setValue(PRESENTATION_LAYER, presentationLayer);
	}

	/**
	 * Sets the preference key for the visibility of squiggles inside text.
	 * 
	 * @param textKey the preference key
	 */
	public void setTextPreferenceKey(String textKey) {
		setValue(TEXT_PREFERENCE_KEY, textKey);
	}

	/**
	 * Sets the default visibility inside text.
	 * 
	 * @param textValue <code>true</code> if visible by default, <code>false</code> otherwise
	 */
	public void setTextPreferenceValue(boolean textValue) {
		setValue(TEXT_PREFERENCE_VALUE, textValue);
	}
	
	/**
	 * Sets the preference key for highlighting inside text.
	 * 
	 * @param highlightKey the preference key
	 * @since 3.0
	 */
	public void setHighlightPreferenceKey(String highlightKey) {
		setValue(HIGHLIGHT_PREFERENCE_KEY, highlightKey);
	}
	
	/**
	 * Sets the default value for highlighting inside text.
	 * 
	 * @param highlightValue <code>true</code> if highlighted in text by default, <code>false</code> otherwise
	 * @since 3.0
	 */
	public void setHighlightPreferenceValue(boolean highlightValue) {
		setValue(HIGHLIGHT_PREFERENCE_VALUE, highlightValue);
	}
	
	/**
	 * Sets whether the annotation type contributes to the overview ruler's header.
	 * 
	 * @param contributesToHeader <code>true</code> if in header, <code>false</code> otherwise
	 */
	public void setContributesToHeader(boolean contributesToHeader) {
		setValue(HEADER_VALUE, contributesToHeader);
	}
	
	/**
	 * Returns the default value for go to next navigation enablement.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isGoToNextNavigationTarget() {
		return getBooleanValue(IS_GO_TO_NEXT_TARGET_VALUE);
	}

	/**
	 * Sets the default value for go to next navigation enablement.
	 * 
	 * @param isGoToNextNavigationTarget <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToNextNavigationTarget(boolean isGoToNextNavigationTarget) {
		setValue(IS_GO_TO_NEXT_TARGET_VALUE, isGoToNextNavigationTarget);
	}

	/**
	 * Returns the preference key for go to next navigation enablement.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getIsGoToNextNavigationTargetKey() {
		return getStringValue(IS_GO_TO_NEXT_TARGET_KEY);
	}

	/**
	 * Sets the preference key for go to next navigation enablement.
	 * 
	 * @param isGoToNextNavigationTargetKey <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToNextNavigationTargetKey(String isGoToNextNavigationTargetKey) {
		setValue(IS_GO_TO_NEXT_TARGET_KEY, isGoToNextNavigationTargetKey);
	}

	/**
	 * Returns the default value for go to previous navigation enablement.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isGoToPreviousNavigationTarget() {
		return getBooleanValue(IS_GO_TO_PREVIOUS_TARGET_VALUE);
	}

	/**
	 * Sets the default value for go to previous navigation enablement.
	 * 
	 * @param isGoToPreviousNavigationTarget <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setIsGoToPreviousNavigationTarget(boolean isGoToPreviousNavigationTarget) {
		setValue(IS_GO_TO_PREVIOUS_TARGET_VALUE, isGoToPreviousNavigationTarget);
	}

	/**
	 * Returns the preference key for go to previous navigation enablement.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getIsGoToPreviousNavigationTargetKey() {
		return getStringValue(IS_GO_TO_PREVIOUS_TARGET_KEY);
	}

	/**
 	 * Sets the preference key for go to previous navigation enablement.
 	 * 
 	 * @param isGoToPreviousNavigationTargetKey the preference key
	 * @since 3.0
	 */
	public void setIsGoToPreviousNavigationTargetKey(String isGoToPreviousNavigationTargetKey) {
		setValue(IS_GO_TO_PREVIOUS_TARGET_KEY, isGoToPreviousNavigationTargetKey);
	}

	/**	
	 * Returns the preference key for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @return the preference key or <code>null</code> if the key is undefined
	 * @since 3.0
	 */
	public String getShowInNextPrevDropdownToolbarActionKey() {
		return getStringValue(SHOW_IN_NAVIGATION_DROPDOWN_KEY);
	}

	/**
	 * Sets the preference key for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @param showInNextPrevDropdownToolbarActionKey the preference key
	 * @since 3.0
	 */
	public void setShowInNextPrevDropdownToolbarActionKey(String showInNextPrevDropdownToolbarActionKey) {
		setValue(SHOW_IN_NAVIGATION_DROPDOWN_KEY, showInNextPrevDropdownToolbarActionKey);
	}
	
	/**
	 * Returns the default value for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @return <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public boolean isShowInNextPrevDropdownToolbarAction() {
		return getBooleanValue(SHOW_IN_NAVIGATION_DROPDOWN_VALUE);
	}

	/**
	 * Sets the default value for the visibility in the next/previous drop down toolbar action.
	 * 
	 * @param showInNextPrevDropdownToolbarAction <code>true</code> if enabled by default
	 * @since 3.0
	 */
	public void setShowInNextPrevDropdownToolbarAction(boolean showInNextPrevDropdownToolbarAction) {
		setValue(SHOW_IN_NAVIGATION_DROPDOWN_VALUE, showInNextPrevDropdownToolbarAction);
	}
	
	/**
	 * Returns the image descriptor for the image to be drawn in the vertical ruler. The provided
	 * image is only used, if <code>getAnnotationImageProvider</code> returns <code>null</code>.
	 * 
	 * @return the image descriptor or <code>null</code>
	 * @since 3.0
	 **/
	public ImageDescriptor getImageDescriptor() {
		return (ImageDescriptor) getValue(IMAGE_DESCRIPTOR);
	}
	
	/**
	 * Sets the image descriptor for the image to be drawn in the vertical ruler.
	 * 
	 * @param descriptor the image descriptor
	 * @since 3.0
	 */
	public void setImageDescriptor(ImageDescriptor descriptor) {
		setValue(IMAGE_DESCRIPTOR, descriptor);
	}
	
	/**
	 * Returns the symbolic name of the image to be drawn in the vertical ruler.
	 * The image is only used if <code>getImageDescriptor</code> returns <code>null</code>.
	 * 
	 * @return the symbolic name of the image or <code>null</code>
	 * @since 3.0
	 */
	public String getSymbolicImageName() {
		return getStringValue(SYMBOLIC_IMAGE_NAME);
	}
	
	/**
	 * Sets the symbolic name of the image to be drawn in the vertical ruler.
	 * 
	 * @param symbolicImageName the symbolic image name
	 * @since 3.0
	 */
	public void setSymbolicImageName(String symbolicImageName) {
		setValue(SYMBOLIC_IMAGE_NAME, symbolicImageName);
	}
	
	/**
	 * Returns the annotation image provider. If no default annotation image
	 * provider has been set, this method checks whether the annotation image
	 * provider data has been set. If so, an annotation image provider is
	 * created if the configuration element's plug-in is loaded. When an
	 * annotation image provider has been created successfully, it is set as
	 * the default annotation image provider.
	 * 
	 * @return the annotation image provider
	 * @since 3.0
	 */
	public IAnnotationImageProvider getAnnotationImageProvider() {
		if (fAnnotationImageProvider == null) {
			if (fConfigurationElement != null && fAnnotationImageProviderAttribute != null) {
				IPluginDescriptor descriptor = fConfigurationElement.getDeclaringExtension().getDeclaringPluginDescriptor();
				if (descriptor.isPluginActivated()) {
					try {
						fAnnotationImageProvider= (IAnnotationImageProvider) fConfigurationElement.createExecutableExtension(fAnnotationImageProviderAttribute);
					} catch (CoreException x) {
						TextEditorPlugin.getDefault().getLog().log(x.getStatus());
					}
				}
			}
		}
		return fAnnotationImageProvider;
	}
	
	/**
	 * Sets the annotation image provider who provides images for annotations
	 * of the specified annotation type.
	 * 
	 * @param provider the annotation image provider
	 * @since 3.0
	 */
	public void setAnnotationImageProvider(IAnnotationImageProvider provider) {
		fAnnotationImageProvider= provider;
		setValue(IMAGE_PROVIDER, provider != null);
	}
	
	/**
	 * Sets the data needed to create the annotation image provider.
	 * 
	 * @param configurationElement the configuration element
	 * @param annotationImageProviderAttribute the atrribute of the
	 *            configuration element
	 * @since 3.0
	 */
	public void setAnnotationImageProviderData(IConfigurationElement configurationElement, String annotationImageProviderAttribute) {
		fConfigurationElement= configurationElement;
		fAnnotationImageProviderAttribute= annotationImageProviderAttribute;
		setValue(IMAGE_PROVIDER, annotationImageProviderAttribute != null);
	}
	
	/**
	 * Merges the values of the given preference into this preference. Existing
	 * values will not be overwritten. Subclasses may extend.
	 * 
	 * @param preference the preference to merge into this preference
	 * @since 3.0
	 */
	public void merge(AnnotationPreference preference) {
		if (!getAnnotationType().equals(preference.getAnnotationType()))
			return;
		
		for (int i= 0; i < ATTRIBUTES.length; i++) {
			if (!hasValue(ATTRIBUTES[i]))
				setValue(ATTRIBUTES[i], preference.getValue(ATTRIBUTES[i]));
		}
		
		if (fAnnotationImageProvider == null)
			fAnnotationImageProvider= preference.fAnnotationImageProvider;
		if (fConfigurationElement == null)
			fConfigurationElement= preference.fConfigurationElement;
		if (fAnnotationImageProviderAttribute == null)
			fAnnotationImageProviderAttribute= preference.fAnnotationImageProviderAttribute;
	}
}
