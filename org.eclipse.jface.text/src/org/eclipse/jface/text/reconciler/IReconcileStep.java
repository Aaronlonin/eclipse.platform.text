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

package org.eclipse.jface.text.reconciler;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;

/**
 * A reconcile step is one of n steps of a reconcile
 * strategy that consists of several steps.
 * 
 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy 
 * @since 3.0
 */
public interface IReconcileStep {

	/**
	 * Returns whether this is the last reconcile step or not.
	 * 
	 * @return <code>true</code> if this is the last reconcile step
	 */
	boolean isLastStep();

	/**
	 * Returns whether this is the first reconcile step or not.
	 * 
	 * @return <code>true</code> if this is the last reconcile step
	 */
	boolean isFirstStep();

	/**
	 * Sets the step which is in front of this step in the pipe.
	 * <p>
	 * Note: This method must be called at most once per reconcile step.
	 * </p>
	 * 
	 * @param step the previous step
	 * @exception org.eclipse.jface.text.Assert#AssertionFailedException if called more than once
	 */
	void setPreviousStep(IReconcileStep step);

	/**
	 * Activates incremental reconciling of the specified dirty region.
	 * As a dirty region might span multiple content types, the segment of the
	 * dirty region which should be investigated is also provided to this 
	 * reconciling strategy. The given regions refer to the document passed into
	 * the most recent call of <code>setDocument</code>.
	 *
	 * @param dirtyRegion the document region which has been changed
	 * @param subRegion the sub region in the dirty region which should be reconciled
	 * @return an array with reconcile results 
	 */
	IReconcileResult[] reconcile(DirtyRegion dirtyRegion, IRegion subRegion);

	/**
	 * Activates non-incremental reconciling. The reconciling strategy is just told
	 * that there are changes and that it should reconcile the given partition of the
	 * document most recently passed into <code>setDocument</code>.
	 *
	 * @param partition the document partition to be reconciled
	 * @return an array with reconcile results 
	 */
	IReconcileResult[] reconcile(IRegion partition);

	/**
	 * Sets the progress monitor to this reconcile step.
	 * 
	 * @param monitor the progress monitor to be used
	 */
	void setProgressMonitor(IProgressMonitor monitor);

	/**
	 * Returns the progress monitor used to report progress.
	 *
	 * @return a progress monitor or null if no progress monitor is provided
	 */
	public IProgressMonitor getProgressMonitor();

	/**
	 * Tells this reconcile step on which model it will
	 * work. This method will be called before any other method 
	 * and can be called multiple times. The regions passed to the
	 * other methods always refer to the most recent document 
	 * passed into this method.
	 *
	 * @param inputModel the model on which this strategy will work
	 */
	void setInputModel(IReconcilableModel inputModel);
}
