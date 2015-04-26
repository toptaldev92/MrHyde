package org.faudroids.mrhyde.ui.tree;

import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.ui.tree.AbstractNode;


public final class FileNode extends AbstractNode {

	private final TreeEntry treeEntry;

	public FileNode(AbstractNode parent, String path, TreeEntry treeEntry) {
		super(parent, path);
		this.treeEntry = treeEntry;
	}


	public TreeEntry getTreeEntry() {
		return treeEntry;
	}

}
