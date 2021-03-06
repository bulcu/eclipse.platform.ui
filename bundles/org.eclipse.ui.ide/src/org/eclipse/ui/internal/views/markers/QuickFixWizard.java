/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.views.markers;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.markers.internal.MarkerMessages;

/**
 * QuickFixWizard is the wizard for quick fixes.
 *
 * @since 3.4
 *
 */
class QuickFixWizard extends Wizard {

	private IMarker[] selectedMarkers;
	private Map<IMarkerResolution, Collection<IMarker>> resolutionMap;
	private String description;
	private IWorkbenchPartSite partSite;

	/**
	 * Create the wizard with the map of resolutions.
	 *
	 * @param description the description of the problem
	 * @param selectedMarkers the markers that were selected
	 * @param resolutions Map key {@link IMarkerResolution} value {@link IMarker} []
	 * @param site the {@link IWorkbenchPartSite} to open the markers in
	 */
	public QuickFixWizard(String description, IMarker[] selectedMarkers, Map<IMarkerResolution, Collection<IMarker>> resolutions, IWorkbenchPartSite site) {
		this.selectedMarkers= selectedMarkers;
		this.resolutionMap = resolutions;
		this.description = description;
		partSite = site;
		setDefaultPageImageDescriptor(IDEInternalWorkbenchImages
				.getImageDescriptor(IDEInternalWorkbenchImages.IMG_DLGBAN_QUICKFIX_DLG));
		setNeedsProgressMonitor(true);

	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(new QuickFixPage(description, selectedMarkers, resolutionMap, partSite));
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress finishRunnable = mon -> {
			IWizardPage[] pages = getPages();
			SubMonitor subMonitor = SubMonitor.convert(mon, MarkerMessages.MarkerResolutionDialog_Fixing,
					(10 * pages.length) + 1);
			subMonitor.worked(1);
			for (IWizardPage page : pages) {
				// Allow for cancel event processing
				getShell().getDisplay().readAndDispatch();
				QuickFixPage wizardPage = (QuickFixPage) page;
				wizardPage.performFinish(subMonitor.split(10));
			}
		};

		try {
			getContainer().run(false, true, finishRunnable);
		} catch (InvocationTargetException e) {
			StatusManager.getManager().handle(
					StatusUtil.newStatus(IStatus.ERROR,
							e.getLocalizedMessage(), e));
			return false;
		} catch (InterruptedException e) {
			StatusManager.getManager().handle(
					StatusUtil.newStatus(IStatus.ERROR,
							e.getLocalizedMessage(), e));
			return false;
		}

		return true;
	}

}
