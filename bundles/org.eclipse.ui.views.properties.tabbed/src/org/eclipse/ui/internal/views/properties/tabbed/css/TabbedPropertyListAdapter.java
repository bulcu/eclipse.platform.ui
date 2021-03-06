/*******************************************************************************
 * Copyright (c) 2017 SAP SE and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.ui.internal.views.properties.tabbed.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyList;

public class TabbedPropertyListAdapter extends CompositeElement {

	public TabbedPropertyListAdapter(TabbedPropertyList composite, CSSEngine engine) {
		super(composite, engine);
	}

	@Override
	public void reset() {
		getTabbedPropertyTitle().initColours();
		super.reset();
	}

	private TabbedPropertyList getTabbedPropertyTitle() {
		return (TabbedPropertyList) getNativeWidget();
	}
}
