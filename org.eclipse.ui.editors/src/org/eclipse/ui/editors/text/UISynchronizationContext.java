/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.ui.editors.text;

import org.eclipse.core.filebuffers.ISynchronizationContext;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Synchronization context for file buffers whose documents are shown in the UI.
 * The synchronization runnable is executed in the UI thread.
 * 
 * @since 3.0
 */
public class UISynchronizationContext implements ISynchronizationContext {

	/*
	 * @see org.eclipse.core.filebuffers.ISynchronizationContext#run(java.lang.Runnable)
	 */
	public void run(Runnable runnable) {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		if (windows != null && windows.length > 0) {
			Display display= windows[0].getShell().getDisplay();
			display.asyncExec(runnable);
		} else {
			runnable.run();
		}
	}
}
