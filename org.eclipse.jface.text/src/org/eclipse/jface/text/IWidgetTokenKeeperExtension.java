/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jface.text;

/**
 * Extension interface to <code>IWidgetTokenKeeper</code>. Replaces the original
 * <code>requestWidgetToken</code> functionality with a new priority based approach.
 * Adds the concept of focus handling.
 * 
 * @since 3.0
 */
public interface IWidgetTokenKeeperExtension {
	
	/**
	 * The given widget token owner requests the widget token  from 
	 * this token keeper. Returns  <code>true</code> if the token is released
	 * by this token keeper. Note, the keeper must not call 
	 * <code>releaseWidgetToken(IWidgetTokenKeeper)</code> explicitly.
	 * 
	 * <p>The general contract is that the receiver should release the token
	 * if <code>priority</code> exceeds the receiver's priority.</p>
	 * 
	 * @param owner the token owner
	 * @param priority the priority of the request
	 * @return <code>true</code> if token has been released <code>false</code> otherwise
	 */
	boolean requestWidgetToken(IWidgetTokenOwner owner, int priority);
	
	/**
	 * Requests the receiver to give focus to its popup shell, hover, or similar. There is
	 * no assumption made whether the receiver actually succeeded in taking the focus.
	 * 
	 * @param owner the token owner
	 */
	void setFocus(IWidgetTokenOwner owner);
}
