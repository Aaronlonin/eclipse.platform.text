/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.texteditor.rulers;


/**
 * Provides support to modify and query the visibility of
 * ruler columns and test whether a ruler column is supported.
 * <p>
 * This interface must not be implemented by clients.
 * </p>
 * 
 * @since 3.3
 */
public interface IColumnSupport {

	/**
	 * Returns <code>true</code> if the column described by <code>descriptor</code> is
	 * currently showing, <code>false</code> if not.
	 * 
	 * @param descriptor the column descriptor
	 * @return <code>true</code> if the specified column is currently visible
	 */
	boolean isColumnVisible(RulerColumnDescriptor descriptor);

	/**
	 * Attempts to set the visibility of the column described by <code>descriptor</code>. Nothing
	 * happens if the visibility is already as requested, or if the column is not supported by the
	 * editor.
	 * 
	 * @param descriptor the column descriptor
	 * @param visible <code>true</code> to show the column, <code>false</code> to hide it
	 */
	void setColumnVisible(RulerColumnDescriptor descriptor, boolean visible);

	/**
	 * Returns <code>true</code> if the column described by <code>descriptor</code> is
	 * supported by the receiver's editor, <code>false</code> if <code>id</code> is not the
	 * identifier of a known column contribution, if the column does not target the editor, or if
	 * the editor does not support contributed columns.
	 * 
	 * @param descriptor the column descriptor
	 * @return <code>true</code> if the specified column is supported
	 */
	boolean isColumnSupported(RulerColumnDescriptor descriptor);

	/**
	 * Removes and disposes all currently visible ruler columns.
	 */
	void dispose();
}