/*******************************************************************************
 * Copyright (c) 2008, 2015 Matthew Hall and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 195222, 264307, 265561, 301774
 *     Ovidio Mallo - bug 306633
 ******************************************************************************/

package org.eclipse.core.internal.databinding.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.IDiff;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.databinding.property.list.SimpleListProperty;

/**
 * @since 3.3
 *
 */
public class BeanListProperty extends SimpleListProperty {
	private final PropertyDescriptor propertyDescriptor;
	private final Class elementType;

	/**
	 * @param propertyDescriptor
	 * @param elementType
	 */
	public BeanListProperty(PropertyDescriptor propertyDescriptor,
			Class elementType) {
		this.propertyDescriptor = propertyDescriptor;
		this.elementType = elementType == null ? BeanPropertyHelper
				.getCollectionPropertyElementType(propertyDescriptor)
				: elementType;
	}

	@Override
	public Object getElementType() {
		return elementType;
	}

	@Override
	protected List doGetList(Object source) {
		return asList(BeanPropertyHelper.readProperty(source,
				propertyDescriptor));
	}

	private List asList(Object propertyValue) {
		if (propertyValue == null)
			return Collections.EMPTY_LIST;
		if (propertyDescriptor.getPropertyType().isArray())
			return Arrays.asList((Object[]) propertyValue);
		return (List) propertyValue;
	}

	@Override
	protected void doSetList(Object source, List list, ListDiff diff) {
		doSetList(source, list);
	}

	@Override
	protected void doSetList(Object source, List list) {
		BeanPropertyHelper.writeProperty(source, propertyDescriptor,
				convertListToBeanPropertyType(list));
	}

	private Object convertListToBeanPropertyType(List list) {
		Object propertyValue = list;
		if (propertyDescriptor.getPropertyType().isArray()) {
			Class componentType = propertyDescriptor.getPropertyType()
					.getComponentType();
			Object[] array = (Object[]) Array.newInstance(componentType, list
					.size());
			list.toArray(array);
			propertyValue = array;
		}
		return propertyValue;
	}

	@Override
	public INativePropertyListener adaptListener(
			final ISimplePropertyListener listener) {
		return new BeanPropertyListener(this, propertyDescriptor, listener) {
			@Override
			protected IDiff computeDiff(Object oldValue, Object newValue) {
				return Diffs
						.computeListDiff(asList(oldValue), asList(newValue));
			}
		};
	}

	@Override
	public String toString() {
		String s = BeanPropertyHelper.propertyName(propertyDescriptor) + "[]"; //$NON-NLS-1$
		if (elementType != null)
			s += "<" + BeanPropertyHelper.shortClassName(elementType) + ">"; //$NON-NLS-1$//$NON-NLS-2$
		return s;
	}
}
