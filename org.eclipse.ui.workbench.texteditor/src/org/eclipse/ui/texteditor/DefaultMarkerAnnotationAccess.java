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
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPresentation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;

import org.eclipse.ui.internal.texteditor.TextEditorPlugin;


/**
 * @since 2.1
 */
public class DefaultMarkerAnnotationAccess implements IAnnotationAccess, IAnnotationAccessExtension {
	
	
	
	/** Constant for the unknown marker type */
	public final static String UNKNOWN= TextEditorPlugin.PLUGIN_ID + ".unknown";  //$NON-NLS-1$
	
	/** The marker annotation preferences */
	protected MarkerAnnotationPreferences fMarkerAnnotationPreferences;
	
	/**
	 * Returns a new default marker annotation access with the given preferences.
	 * 
	 * @param markerAnnotationPreferences
	 */
	public DefaultMarkerAnnotationAccess(MarkerAnnotationPreferences markerAnnotationPreferences) {
		fMarkerAnnotationPreferences= markerAnnotationPreferences;
	}

	/**
	 * Returns the annotation preference for the given marker.
	 * 
	 * @param marker
	 * @return the annotation preference or <code>null</code> if none
	 */	
	private AnnotationPreference getAnnotationPreference(IMarker marker) {
		Iterator e= fMarkerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			Integer severity;
			boolean isSubtype;
			AnnotationPreference info= (AnnotationPreference) e.next();
			try {
				severity= (Integer)marker.getAttribute(IMarker.SEVERITY);
				isSubtype= marker.isSubtypeOf(info.getMarkerType());
			} catch (CoreException x) {
				return null;
			}
			if (isSubtype && (severity == null || severity.intValue() == info.getSeverity()))
				return info;
			}
		return null;
	}

	/**
	 * Returns the annotation preference for the given marker.
	 * 
	 * @param marker
	 * @return the annotation preference or <code>null</code> if none
	 */	
	private AnnotationPreference getAnnotationPreference(String markerType, int severity) {
		Iterator e= fMarkerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference info= (AnnotationPreference) e.next();
			if (info.getMarkerType().equals(markerType) && severity == info.getSeverity())
				return info;
			}
		return null;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccess#getType(org.eclipse.jface.text.source.Annotation)
	 */
	public Object getType(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation markerAnnotation= (MarkerAnnotation) annotation;
			IMarker marker= markerAnnotation.getMarker();
			if (marker != null && marker.exists()) {
				AnnotationPreference preference= getAnnotationPreference(marker);
				if (preference != null)
					return preference.getAnnotationType();
			}
		} else if (annotation instanceof IAnnotationExtension) {
			IAnnotationExtension annotationExtension= (IAnnotationExtension)annotation;
			AnnotationPreference preference= getAnnotationPreference(annotationExtension.getMarkerType(), annotationExtension.getSeverity());
			if (preference != null)
				return preference.getAnnotationType();
		}
		return UNKNOWN;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccess#isMultiLine(org.eclipse.jface.text.source.Annotation)
	 */
	public boolean isMultiLine(Annotation annotation) {
		return true;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccess#isTemporary(org.eclipse.jface.text.source.Annotation)
	 */
	public boolean isTemporary(Annotation annotation) {

		if (annotation instanceof IAnnotationExtension)
			return ((IAnnotationExtension)annotation).isTemporary();

		return false;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccessExtension#getLabel(org.eclipse.jface.text.source.Annotation)
	 */
	public String getTypeLabel(Annotation annotation) {
		AnnotationPreference preference= null;
		if (annotation instanceof MarkerAnnotation) {
			MarkerAnnotation markerAnnotation= (MarkerAnnotation) annotation;
			IMarker marker= markerAnnotation.getMarker();
			if (marker != null && marker.exists()) {
				preference= getAnnotationPreference(marker);
			}
		} else if (annotation instanceof IAnnotationExtension) {
			IAnnotationExtension annotationExtension= (IAnnotationExtension)annotation;
			preference= getAnnotationPreference(annotationExtension.getMarkerType(), annotationExtension.getSeverity());
		}
		if (preference != null)
			return preference.getPreferenceLabel();
		
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccessExtension#getAnnotationPresentation(org.eclipse.jface.text.source.Annotation)
	 */
	public AnnotationPresentation getAnnotationPresentation(Annotation annotation) {
		Object data= annotation.getData(AnnotationPresentation.class);
		if (data instanceof AnnotationPresentation)
			return (AnnotationPresentation) data;
		
		if (annotation instanceof MarkerAnnotation){
			MarkerAnnotation m= (MarkerAnnotation) annotation;
			AnnotationPresentation presentation= new MarkerAnnotationPresentation(m);
			annotation.setData(AnnotationPresentation.class, presentation);
			return presentation;
		}
		
		return null;
	}
}
