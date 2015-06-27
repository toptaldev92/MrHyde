package org.faudroids.mrhyde.git;

import org.eclipse.egit.github.core.TreeEntry;

import java.util.HashMap;
import java.util.Map;

public final class DirNode extends AbstractNode {

	private final Map<String, AbstractNode> entries = new HashMap<>();

	DirNode(DirNode parent, String path, TreeEntry treeEntry) {
		super(parent, path, treeEntry);
	}


	public Map<String, AbstractNode> getEntries() {
		return entries;
	}

}
