package org.faudroids.mrhyde.jekyll;

import org.faudroids.mrhyde.git.FileNode;
import org.roboguice.shaded.goole.common.base.Objects;

/**
 * General Jeklly content.
 */
public abstract class AbstractJekyllContent {

	protected final String title;
	protected final FileNode fileNode;

	public AbstractJekyllContent(String title, FileNode fileNode) {
		this.title = title;
		this.fileNode = fileNode;
	}

	public String getTitle() {
		return title;
	}

	public FileNode getFileNode() {
		return fileNode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractJekyllContent draft = (AbstractJekyllContent) o;
		return Objects.equal(title, draft.title) &&
				Objects.equal(fileNode, draft.fileNode);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title, fileNode);
	}

}
