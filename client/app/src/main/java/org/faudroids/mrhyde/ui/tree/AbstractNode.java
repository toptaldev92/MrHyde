package org.faudroids.mrhyde.ui.tree;


public abstract class AbstractNode implements Comparable<AbstractNode> {

	private final AbstractNode parent;
	private final String path;

	public AbstractNode(AbstractNode parent, String path) {
		this.parent = parent;
		this.path = path;
	}


	public AbstractNode getParent() {
		return parent;
	}


	public String getPath() {
		return path;
	}


	@Override
	public int compareTo(AbstractNode node) {
		return path.compareTo(node.getPath());
	}

}
