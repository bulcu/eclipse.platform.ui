/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *     Steven Spungin <steven@spungin.tv> - Bug 437958
 ******************************************************************************/

package org.eclipse.e4.ui.tests.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.internal.workbench.E4XMIResource;
import org.eclipse.e4.ui.internal.workbench.ResourceHandler;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class ResourceHandlerTest extends HeadlessStartupTest {
	private ServiceTracker locationTracker;

	public Location getInstanceLocation() {
		if (locationTracker == null) {
			BundleContext context = FrameworkUtil.getBundle(
					ResourceHandlerTest.class).getBundleContext();
			Filter filter = null;
			try {
				filter = context.createFilter(Location.INSTANCE_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this. It should never happen as we have tested the
				// above format.
			}
			locationTracker = new ServiceTracker(context, filter, null);
			locationTracker.open();
		}
		return (Location) locationTracker.getService();
	}

	private ResourceHandler createHandler(URI uri) {
		IEclipseContext localContext = applicationContext.createChild();
		localContext.set(E4Workbench.INSTANCE_LOCATION, getInstanceLocation());
		localContext.set(IWorkbench.PERSIST_STATE, Boolean.TRUE);
		localContext.set(IWorkbench.CLEAR_PERSISTED_STATE, Boolean.TRUE);

		localContext.set(E4Workbench.INITIAL_WORKBENCH_MODEL_URI, uri);

		return ContextInjectionFactory
				.make(ResourceHandler.class, localContext);

	}

	@Test
	public void testModelProcessor() {
		URI uri = URI.createPlatformPluginURI(
				"org.eclipse.e4.ui.tests/xmi/modelprocessor/base.e4xmi", true);

		ResourceHandler handler = createHandler(uri);
		Resource resource = handler.loadMostRecentModel();
		MApplication application = (MApplication) resource.getContents().get(0);
		assertNotNull(application);
		assertEquals(2, application.getChildren().size());
		MWindow mWindow2 = application.getChildren().get(1);
		assertEquals("fragment.contributedWindow", mWindow2.getElementId());

		// Test for XML-ID stuff
		assertEquals("_w4fQ8HVHEd-aXt9fFntEtw", ((E4XMIResource) resource).getID((EObject) mWindow2)); // Window Id

		MWindowElement mWindowElement = mWindow2.getChildren().get(0);
		assertEquals("_rdlLgJQUEd-6X_lmWgGEDA", ((E4XMIResource) resource).getID((EObject) mWindowElement)); // Perspective

		// Test contributorURI
		assertEquals("platform:/plugin/org.eclipse.e4.ui.tests", mWindow2.getContributorURI()); // Window
		assertEquals("platform:/plugin/org.eclipse.e4.ui.tests", mWindowElement.getContributorURI()); // Perspective

		// Fix test suite when live-tooling is part of the build
		List<MHandler> handlers = application.getHandlers();
		if (handlers.size() > 2) {
			String check = "bundleclass://org.eclipse.e4.tools.emf.liveeditor/org.eclipse.e4.tools.emf.liveeditor.OpenLiveDialogHandler";
			if (check.equals(handlers.get(0).getContributionURI())) {
				handlers.remove(0);
			} else if (check.equals(handlers.get(1).getContributionURI())) {
				handlers.remove(1);
			}
		}

		assertTrue(handlers.size() > 0);
		List<MCommand> commands = application.getCommands();
		MCommand expected = commands.get(0);

		// TODO Assert below fails because the commands contain 726 elements but the
		// handlers only 8 and the order of commands and handlers differs too.
		// assertSame(expected, handlers.get(0).getCommand());
		long count = handlers.stream().filter(x -> x.getCommand() == expected).count();
		assertEquals(1, count);

		assertEquals(2, expected.getParameters().size());
		assertEquals(1, mWindow2.getVariables().size());

		MWindow mWindow1 = application.getChildren().get(0);
		assertNotNull(mWindow1.getMainMenu());

		List<MWindowElement> children = mWindow1.getChildren();
		assertEquals(8, children.size());
		assertEquals("fragment.contributedPosFirst", children.get(0).getElementId());
		assertEquals("fragment.contributedBeforePart1", children.get(1).getElementId());
		assertEquals("fragment.contributedAfterPart1", children.get(3).getElementId());
		assertEquals("fragment.contributedPos1", children.get(4).getElementId());
		assertEquals("fragment.contributedBeforePart2", children.get(5).getElementId());
		assertEquals("fragment.contributedAfterPart2", children.get(7).getElementId());
	}

	@Test
	public void testXPathModelProcessor() {

		URI uri = URI.createPlatformPluginURI("org.eclipse.e4.ui.tests/xmi/modelprocessor/base.e4xmi", true);
		ResourceHandler handler = createHandler(uri);
		Resource resource = handler.loadMostRecentModel();
		MApplication application = (MApplication) resource.getContents().get(0);
		assertNotNull(application);

		/**
		 * We will now test the various ways an element can be contributed to
		 * multiple parents. ModelFragments.e4xmi has been configured to add 2
		 * menus to the Main Menu. These menus will receive our test
		 * contributions.
		 */
		MMenu mainMenu = application.getChildren().get(0).getMainMenu();
		assertNotNull(mainMenu);
		MMenu menu1 = (MMenu) findByElementId(mainMenu.getChildren(), "fragment.contributedMenu1");
		assertNotNull(menu1);
		MMenu menu2 = (MMenu) findByElementId(mainMenu.getChildren(), "fragment.contributedMenu2");
		assertNotNull(menu2);
		// Method 1 - comma separated list of parentElementIds
		assertNotNull(findByElementId(menu1.getChildren(), "fragment.contributedMenuItem.csv"));
		assertNotNull(findByElementId(menu2.getChildren(), "fragment.contributedMenuItem.csv"));
		// Method 2 - xpath
		assertNotNull(findByElementId(menu1.getChildren(), "fragment.contributedMenuItem.xpath"));
		assertNotNull(findByElementId(menu2.getChildren(), "fragment.contributedMenuItem.xpath"));
	}

	/**
	 * @param children
	 * @param id
	 * @return the MMenuElement or null if not found
	 */
	private Object findByElementId(List<MMenuElement> children, String id) {
		for (MMenuElement item : children) {
			if (id.equals(item.getElementId())) {
				return item;
			}
		}
		return null;
	}

}
