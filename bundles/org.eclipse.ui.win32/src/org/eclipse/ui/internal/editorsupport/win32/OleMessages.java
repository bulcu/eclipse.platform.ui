/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.editorsupport.win32;

import com.ibm.icu.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class which helps managing messages
 */
public class OleMessages {
    private static final String RESOURCE_BUNDLE = "org.eclipse.ui.internal.editorsupport.win32.messages";//$NON-NLS-1$

    private static ResourceBundle bundle = ResourceBundle
            .getBundle(RESOURCE_BUNDLE);

    private OleMessages() {
        // prevent instantiation of class
    }

    /**
     * Returns the formatted message for the given key in the resource bundle.
     *
     * @param key
     *            the resource name
     * @param args
     *            the message arguments
     * @return the string
     */
    public static String format(String key, Object[] args) {
        return MessageFormat.format(getString(key), args);
    }

    /**
     * Returns the resource object with the given key in the resource bundle. If
     * there isn't any value under the given key, the key is returned.
     *
     * @param key
     *            the resource name
     * @return the string
     */
    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the resource object with the given key in the resource bundle. If
     * there isn't any value under the given key, the default value is returned.
     *
     * @param key
     *            the resource name
     * @param def
     *            the default value
     * @return the string
     */
    public static String getString(String key, String def) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return def;
        }
    }
}
