package org.faudroids.mrhyde.ui.tree;


import org.eclipse.egit.github.core.TreeEntry;

public abstract class AbstractNode implements Comparable<AbstractNode> {

	private final AbstractNode parent;
	private final String path;
	private final TreeEntry treeEntry;

	public AbstractNode(AbstractNode parent, String path, TreeEntry treeEntry) {
		this.parent = parent;
		this.path = path;
		this.treeEntry = treeEntry;
	}


	public AbstractNode getParent() {
		return parent;
	}


	public String getPath() {
		return path;
	}


	public TreeEntry getTreeEntry() {
		return treeEntry;
	}


	@Override
	public int compareTo(AbstractNode node) {
		return path.compareTo(node.getPath());
	}

}
