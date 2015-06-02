package org.faudroids.mrhyde.git;


import android.os.Bundle;

import javax.inject.Inject;

import timber.log.Timber;

public class NodeUtils {

	private static final String STATE_NODE = NodeUtils.class.getName() + ".NODE";

	@Inject
	NodeUtils() { }


	/**
	 * Stores a node in the supplied bundle to be retrieved later.
	 */
	public void saveInstanceState(Bundle outState, AbstractNode node) {
		if (node == null) return;

		AbstractNode iter = node;
		String selectedPath = iter.getPath();
		iter = iter.getParent();

		while (iter != null) {
			selectedPath = iter.getPath() + "/" + selectedPath;
			Timber.d(selectedPath);
			iter = iter.getParent();
		}

		outState.putString(STATE_NODE, selectedPath);
	}


	/**
	 * Retrieves a previously stored node from bundle, relative to passed in root node.
	 */
	public AbstractNode restoreInstanceState(Bundle inState, DirNode rootNode) {
		String selectedPath = inState.getString(STATE_NODE);
		if (selectedPath == null) return null;
		return getNodeByPath(rootNode, selectedPath);
	}


	/**
	 * Returns a node based on a given path relative to a root.
	 */
	public AbstractNode getNodeByPath(DirNode rootNode, String path) {
		if (!path.startsWith("/")) path = "/" + path;
		Timber.d("restoring " + path);
		String[] paths = path.split("/");
		AbstractNode iter = rootNode;
		for (int i = 1; i < paths.length; ++i) {
			iter = ((DirNode) iter).getEntries().get(paths[i]);
		}
		return iter;
	}

}
