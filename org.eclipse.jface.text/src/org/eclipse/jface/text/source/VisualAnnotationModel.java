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




import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;



/**
 * Annotation model for visual annotations. Assume a viewer's input element is annotated with
 * some semantic annotation such as a breakpoint and that it is simultanously shown in multiple
 * viewers. A source viewer, e.g., supports visual range indication for which it utilizes
 * annotations. The range indicating annotation is specific to the visual presentation
 * of the input element in this viewer and thus should only be visible in this viewer. The
 * breakpoints however are independent from the input element's presentation and thus should
 * be shown in all viewers in which the element is shown. As a viewer supports one vertical
 * ruler which is based on one annotation model, there must be a visual annotation model for
 * each viewer which all wrap the same element specific model annotation model.
 */
class VisualAnnotationModel extends AnnotationModel implements IAnnotationModelListener {
	
	/** The wrapped model annotation model */
	private IAnnotationModel fModel;

	/**
	 * Constructs a visual annotation model which wraps the given
	 * model based annotation model
	 *
	 * @param modelAnnotationModel the model based annotation model
	 */
	public VisualAnnotationModel(IAnnotationModel modelAnnotationModel) {
		fModel= modelAnnotationModel;
	}
	
	/**
	 * Returns the visual annotation model's wrapped model based annotation model.
	 *
	 * @return the model based annotation model
	 */
	public IAnnotationModel getModelAnnotationModel() {
		return fModel;
	}
	
	/*
	 * @see IAnnotationModel#addAnnotationModelListener(IAnnotationModelListener)
	 */
	public void addAnnotationModelListener(IAnnotationModelListener listener) {

		if (fModel != null && fAnnotationModelListeners.isEmpty())
			fModel.addAnnotationModelListener(this);

		super.addAnnotationModelListener(listener);
	}
	
	/*
	 * @see IAnnotationModel#connect(IDocument)
	 */
	public void connect(IDocument document) {
		super.connect(document);
		if (fModel != null)
			fModel.connect(document);
	}
	
	/*
	 * @see IAnnotationModel#disconnect(IDocument)
	 */
	public void disconnect(IDocument document) {
		super.disconnect(document);
		if (fModel != null)
			fModel.disconnect(document);
	}
	
	/*
	 * @see IAnnotationModel#getAnnotationIterator()
	 */
	public Iterator getAnnotationIterator() {
		
		if (fModel == null)
			return super.getAnnotationIterator();

		ArrayList a= new ArrayList(20);

		Iterator e= fModel.getAnnotationIterator();
		while (e.hasNext())
			a.add(e.next());

		e= super.getAnnotationIterator();
		while (e.hasNext())
			a.add(e.next());

		return a.iterator();
	}
	
	/*
	 * @see IAnnotationModel#getPosition(Annotation)
	 */
	public Position getPosition(Annotation annotation) {
		
		Position p= (Position) fAnnotations.get(annotation);
		if (p != null)
			return p;
			
		if (fModel != null)
			return fModel.getPosition(annotation);
			
		return null;
	}
		
	/*
	 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
	 */
	public void modelChanged(IAnnotationModel model) {
		if (model == fModel) {
			Iterator iter= new ArrayList(fAnnotationModelListeners).iterator();
			while (iter.hasNext()) {
				IAnnotationModelListener l= (IAnnotationModelListener)iter.next();
				l.modelChanged(this);
			}
		}
	}
	
	/**
	 * Modifies associated position of the given annotation to the given position.
	 * If the annotation is not yet managed by this annotation model, the annotation
	 * is added. All annotation model change listeners will be informed about the change.
	 *
	 * @param annotation the annotation whose associated position should be modified
	 * @param position the position to whose values the associated position should be changed
	 */
	public void modifyAnnotation(Annotation annotation, Position position) {
		modifyAnnotation(annotation, position, true);
	}
	
	/**
	 * Modifies the associated position of the given annotation to the given position.
	 * If the annotation is not yet managed by this annotation model, the annotation
	 * is added. If requested, all annotation model change listeners will be informed 
	 * about the change.
	 *
	 * @param annotation the annotation whose associated position should be modified
	 * @param position the position to whose values the associated position should be changed
	 * @param fireModelChanged indicates whether to notify all model listeners	 
	 */
	private void modifyAnnotation(Annotation annotation, Position position, boolean fireModelChanged) {
		if (position == null) {
			removeAnnotation(annotation, fireModelChanged);
		} else {
			Position p= (Position) fAnnotations.get(annotation);
			if (p != null) {
				p.setOffset(position.getOffset());
				p.setLength(position.getLength());
				if (fireModelChanged)
					fireModelChanged();
			} else {
				try {
					addAnnotation(annotation, position, fireModelChanged);
				} catch (BadLocationException e) {
					// ignore invalid position
				}
			}
		}
	}
	
	/*
	 * @see IAnnotationModel#removeAnnotationModelListener(IAnnotationModelListener)
	 */
	public void removeAnnotationModelListener(IAnnotationModelListener listener) {
		super.removeAnnotationModelListener(listener);

		if (fModel != null && fAnnotationModelListeners.isEmpty())
			fModel.removeAnnotationModelListener(this);
	}
}
