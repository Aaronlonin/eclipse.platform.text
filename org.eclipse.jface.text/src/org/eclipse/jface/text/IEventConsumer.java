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
package org.eclipse.jface.text;



import org.eclipse.swt.events.VerifyEvent;

/**
 * Implementers can register with a text viewer and 
 * receive <code>VerifyEvent</code>s before the text viewer 
 * they are registered with. If the event consumer marks events
 * as processed by turning their <code>doit</code> field to 
 * <code>false</code> the text viewer subsequently ignores them.
 * Clients may implement this interface.<p>
 * 
 * <code>ITextViewerExtension2</code> allows clients to manage the
 * <code>VerifyListener</code>s of a <code>TextViewer</code>. This makes
 * <code>IEventConsumer</code> obsolete.
 * 
 * @see ITextViewer
 * @see org.eclipse.swt.events.VerifyEvent
 */
public interface IEventConsumer {
	
	/**
	 * Processes the given event and marks it as done if it should 
	 * be ignored by subsequent receivers.
	 *
	 * @param event the verify event which will be investigated
	 */ 
	public void processEvent(VerifyEvent event);
}
