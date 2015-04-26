package org.faudroids.mrhyde.ui.tree;

import org.eclipse.egit.github.core.TreeEntry;


public final class FileNode extends AbstractNode {

	public FileNode(AbstractNode parent, String path, TreeEntry treeEntry) {
		super(parent, path, treeEntry);
	}

}
