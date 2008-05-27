/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * Oakland Software (Francis Upton - francisu@ieee.org) 
 *    bug 197113 Project Explorer drag and drop selection not working properly
 *******************************************************************************/
package org.eclipse.ui.navigator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.internal.navigator.ContributorTrackingSet;
import org.eclipse.ui.internal.navigator.NavigatorContentService;
import org.eclipse.ui.internal.navigator.NavigatorDecoratingLabelProvider;
import org.eclipse.ui.internal.navigator.NavigatorPipelineService;

/**
 * 
 * Provides the Tree Viewer for the Common Navigator. Content and labels are
 * provided by an instance of {@link INavigatorContentService}&nbsp; which uses
 * the ID supplied in the constructor
 * {@link CommonViewer#CommonViewer(String, Composite, int)} or through
 * {@link NavigatorContentServiceFactory#createContentService(String, org.eclipse.jface.viewers.StructuredViewer)}.
 * 
 * <p>
 * Clients may extend this class.
 * </p>
 * 
 * <p>
 * Note that as of 3.2.1 and 3.3, the common viewer caches its selection.
 * Clients must not set the selection of the viewer's tree control directly.
 * </p>
 * 
 * @since 3.2
 */
public class CommonViewer extends TreeViewer {

	private final NavigatorContentService contentService;

	private ISelection cachedSelection;
	
	/**
	 * <p>
	 * Constructs the Tree Viewer for the Common Navigator and the corresponding
	 * NavigatorContentService. The NavigatorContentService will provide the
	 * Content Provider and Label Provider -- these need not be supplied by
	 * clients.
	 * <p>
	 * For the valid bits to supply in the style mask (aStyle), see
	 * documentation provided by {@link TreeViewer}.
	 * </p>
	 * 
	 * @param aViewerId
	 *            An id tied to the extensions that is used to focus specific
	 *            content to a particular instance of the Common Navigator
	 * @param aParent
	 *            A Composite parent to contain the actual SWT widget
	 * @param aStyle
	 *            A style mask that will be used to create the TreeViewer
	 *            Composite.
	 */
	public CommonViewer(String aViewerId, Composite aParent, int aStyle) {
		super(aParent, aStyle);
		contentService = new NavigatorContentService(aViewerId, this);
		init();
	}

	/**
	 * <p>
	 * Initializes the content provider, label provider, and drag and drop
	 * support. Should not be called by clients -- this method is invoked when
	 * the constructor is invoked.
	 * </p>
	 */
	protected void init() {
		setUseHashlookup(true);
		setContentProvider(contentService.createCommonContentProvider());
		setLabelProvider(new NavigatorDecoratingLabelProvider(contentService.createCommonLabelProvider()));
		initDragAndDrop();

	}

	protected void removeWithoutRefresh(Object[] elements) {
		super.remove(elements);
	}

	/**
	 * <p>
	 * Adds DND support to the Navigator. Uses hooks into the extensible
	 * framework for DND.
	 * </p>
	 * <p>
	 * By default, the following Transfer types are supported:
	 * <ul>
	 * <li>LocalSelectionTransfer.getInstance(),
	 * <li>PluginTransfer.getInstance()
	 * </ul>
	 * </p>
	 * 
	 * @see CommonDragAdapter
	 * @see CommonDropAdapter
	 */
	protected void initDragAndDrop() {

		/* Handle Drag and Drop */
		int operations = DND.DROP_COPY | DND.DROP_MOVE;

		CommonDragAdapter dragAdapter = new CommonDragAdapter(contentService,
				this);
		addDragSupport(operations, dragAdapter.getSupportedDragTransfers(),
				dragAdapter);

		CommonDropAdapter dropAdapter = new CommonDropAdapter(contentService,
				this);
		addDropSupport(operations, dropAdapter.getSupportedDropTransfers(),
				dropAdapter);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#createTreeItem(org.eclipse.swt.widgets.Widget,
	 *      java.lang.Object, int)
	 */
	protected void createTreeItem(Widget parent, final Object element, int index) {
		try {
			super.createTreeItem(parent, element, index);
		} catch (Exception ex) {
			ex.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();
		}

	}

	/*
	 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
	 */
	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {

		Object[] changed = event.getElements();
		if (changed != null) {
			List others = Arrays.asList(changed);
			for (Iterator iter = others.iterator(); iter.hasNext();) {
				if(iter.next() == null)
					iter.remove(); 
			} 
			if (others.isEmpty()) {
				return;
			}
			event = new LabelProviderChangedEvent((IBaseLabelProvider) event
					.getSource(), others.toArray());
		}
		super.handleLabelProviderChanged(event);
	}

	protected void handleDispose(DisposeEvent event) {
		dispose();
		super.handleDispose(event);
	}
 
	/**
	 * <p>
	 * Disposes of the NavigatorContentService, which will dispose the Content
	 * and Label providers.
	 * </p>
	 */
	public void dispose() {
		if (contentService != null) {
			contentService.dispose();
		}
		clearSelectionCache();
	}

	/**
	 * Sets this viewer's sorter and triggers refiltering and resorting of this
	 * viewer's element. Passing <code>null</code> turns sorting off.
	 * 
	 * @param sorter
	 *            a viewer sorter, or <code>null</code> if none
	 */
	public void setSorter(ViewerSorter sorter) {
		if (sorter != null && sorter instanceof CommonViewerSorter) {
			((CommonViewerSorter) sorter).setContentService(contentService);
		}

		super.setSorter(sorter);
	}

	/**
	 * <p>
	 * The {@link INavigatorContentService}provides the hook into the framework
	 * to provide content from the various extensions.
	 * </p>
	 * 
	 * @return The {@link INavigatorContentService}that was created when the
	 *         viewer was created.
	 */
	public INavigatorContentService getNavigatorContentService() {
		return contentService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#add(java.lang.Object,
	 *      java.lang.Object[])
	 */
	public void add(Object parentElement, Object[] childElements) {
		// TODO Intercept ADD for the pipeline service.

		NavigatorPipelineService pipeDream = (NavigatorPipelineService) contentService
				.getPipelineService();

		PipelinedShapeModification modification = new PipelinedShapeModification(
				parentElement, new ContributorTrackingSet(contentService,
						childElements));

		pipeDream.interceptAdd(modification);

		Object parent = (parentElement == getInput()) ? getInput()
				: modification.getParent();

		super.add(parent, modification.getChildren().toArray());

	}

	/**
	 * <p>
	 * Removals are handled by refreshing the parents of each of the given
	 * elements. The parents are determined via calls ot the contentProvider.
	 * </p>
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object[])
	 */
	public void remove(Object[] elements) {

		// TODO Intercept REMOVE for the pipeline service.

		NavigatorPipelineService pipeDream = (NavigatorPipelineService) contentService
				.getPipelineService();

		PipelinedShapeModification modification = new PipelinedShapeModification(
				null, new ContributorTrackingSet(contentService, elements));

		pipeDream.interceptRemove(modification);

		super.remove(modification.getChildren().toArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#refresh(java.lang.Object,
	 *      boolean)
	 */
	public void refresh(Object element, boolean updateLabels) {

		
		if(element != getInput()) {
		
			INavigatorPipelineService pipeDream = contentService
					.getPipelineService();
	
			PipelinedViewerUpdate update = new PipelinedViewerUpdate();
			update.getRefreshTargets().add(element);
			update.setUpdateLabels(updateLabels);
			/* if the update is modified */
			if (pipeDream.interceptRefresh(update)) {
				/* intercept and apply the update */
				boolean toUpdateLabels = update.isUpdateLabels();
				for (Iterator iter = update.getRefreshTargets().iterator(); iter
						.hasNext();) {
					super.refresh(iter.next(), toUpdateLabels);
				}
			} else {
				super.refresh(element, updateLabels);
			}
		} else {
			super.refresh(element, updateLabels);
		}
	}
	
	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) { 

		if(selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			
			INavigatorPipelineService pipeDream = contentService
					.getPipelineService();

			PipelinedViewerUpdate update = new PipelinedViewerUpdate();
			update.getRefreshTargets().addAll(sSelection.toList());
			update.setUpdateLabels(false);
			/* if the update is modified */
			if (pipeDream.interceptRefresh(update)) {
				/* intercept and apply the update */ 
				super.setSelection(new StructuredSelection(update.getRefreshTargets().toArray()) , reveal);
			} else {
				super.setSelection(selection, reveal);
			}
		}
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ContentViewer#hookControl(Control)
     */
    protected void hookControl(Control control) {
    	super.hookControl(control);
        // FIXME - This caching thing should not be here; it's brittle.
        // The underlying problem of over-calling of getSelection() should
        // be addressed instead (see bugs 144294 and 140032)
        // The DragStart event will come before the SelectionEvent on
        // some platforms (GTK).  Since DragStart can turn around and
        // call getSelection(), we need to clear the cache.
        control.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent e) {
            	clearSelectionCache();
            }
        });
    }

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#setSelectionToWidget(java.util.List, boolean)
	 */
	protected void setSelectionToWidget(List v, boolean reveal) {
		clearSelectionCache();
		super.setSelectionToWidget(v, reveal);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#handleDoubleSelect(org.eclipse.swt.events.SelectionEvent)
	 */
	protected void handleDoubleSelect(SelectionEvent event) {
		clearSelectionCache();
		super.handleDoubleSelect(event);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#handleOpen(org.eclipse.swt.events.SelectionEvent)
	 */
	protected void handleOpen(SelectionEvent event) {
		clearSelectionCache();
		super.handleOpen(event);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#handlePostSelect(org.eclipse.swt.events.SelectionEvent)
	 */
	protected void handlePostSelect(SelectionEvent e) {
		clearSelectionCache();
		super.handlePostSelect(e);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#handleSelect(org.eclipse.swt.events.SelectionEvent)
	 */
	protected void handleSelect(SelectionEvent event) {
		clearSelectionCache();
		super.handleSelect(event);
	}
	
	/**
	 * Clears the selection cache.
	 */
	private void clearSelectionCache() {
		cachedSelection = null;
	}
	
	/**
	 * Returns the current selection.
	 * <p>
	 * Note that as of 3.2.1 and 3.3, the common viewer caches its selection.
	 * Clients must not set the selection of the viewer's tree control directly.
	 * </p>
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#getSelection()
	 */
	public ISelection getSelection() {
		if (cachedSelection == null) {
			cachedSelection = super.getSelection();
		}
		return cachedSelection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#refresh(java.lang.Object)
	 */
	public void refresh(Object element) {
		refresh(element, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.StructuredViewer#update(java.lang.Object,
	 *      java.lang.String[])
	 */
	public void update(Object element, String[] properties) {


		if(element != getInput()) {
			INavigatorPipelineService pipeDream = contentService
					.getPipelineService();
	
			PipelinedViewerUpdate update = new PipelinedViewerUpdate();
			update.getRefreshTargets().add(element);
			update.setUpdateLabels(true);
			/* if the update is modified */
			if (pipeDream.interceptUpdate(update)) {
				/* intercept and apply the update */ 
				for (Iterator iter = update.getRefreshTargets().iterator(); iter
						.hasNext();) {
					super.update(iter.next(), properties);
				}
			} else {
				super.update(element, properties);
			}
		} else {
			super.update(element, properties);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return contentService.toString() + " Viewer"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#internalRefresh(java.lang.Object,
	 *      boolean)
	 */
	protected void internalRefresh(Object element, boolean updateLabels) {
		if (element == null && getRoot() == null) {
			return;
		}
		super.internalRefresh(element, updateLabels);
	}

}
