package org.faudroids.mrhyde.git;


import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import timber.log.Timber;

public class NodeUtils {

	@Inject
	NodeUtils() { }


	/**
	 * @see #saveNode(String, Bundle, AbstractNode)
	 */
	public void saveNode(String key, Intent intent, AbstractNode node) {
		Bundle extras = intent.getExtras();
		if (extras == null) extras = new Bundle();
		intent.putExtras(extras);
		saveNode(key, extras, node);
	}


	/**
	 * Stores a node in the supplied bundle to be retrieved later.
	 */
	public void saveNode(String key, Bundle outState, AbstractNode node) {
		if (node == null) return;

		AbstractNode iter = node;
		String selectedPath = iter.getPath();
		iter = iter.getParent();

		while (iter != null) {
			selectedPath = iter.getPath() + "/" + selectedPath;
			Timber.d(selectedPath);
			iter = iter.getParent();
		}

		outState.putString(key, selectedPath);
	}


	/**
	 * @see #restoreNode(String, Bundle, DirNode)
	 */
	public AbstractNode restoreNode(String key, Intent intent, DirNode rootNode) {
		return restoreNode(key, intent.getExtras(), rootNode);
	}


	/**
	 * Retrieves a previously stored node from bundle, relative to passed in root node.
	 */
	public AbstractNode restoreNode(String key, Bundle inState, DirNode rootNode) {
		String selectedPath = inState.getString(key);
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
