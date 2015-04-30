package org.faudroids.mrhyde.git;

import org.eclipse.egit.github.core.TreeEntry;


public final class FileNode extends AbstractNode {

	FileNode(AbstractNode parent, String path, TreeEntry treeEntry) {
		super(parent, path, treeEntry);
	}

}
