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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Accessor for the <code>JFaceTextMessages.properties</code> file in
 * package <code>org.eclipse.jface.text</code>.
 * @since 2.0
 */
class JFaceTextMessages {

	/** The resource bundle name. */
	private static final String RESOURCE_BUNDLE= "org.eclipse.jface.text.JFaceTextMessages";//$NON-NLS-1$
	
	/** The resource bundle. */
	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);
	
	/**
	 * Prohibits the creation of accessor objects.
	 */
	private JFaceTextMessages() {
	}
	
	/**
	 * Returns the string found in the resource bundle under the given key or a place holder string.
	 * 
	 * @param key the look up key
	 * @return the value found under the given key
	 */
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	/**
	 * Gets a string from the resource bundle and formats it with the argument
	 * 
	 * @param key	the string used to get the bundle value, must not be null
	 * @since 3.0
	 */
	public static String getFormattedString(String key, Object[] args) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		return MessageFormat.format(format, args);
	}
}
