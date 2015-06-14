package org.faudroids.mrhyde.jekyll;

import org.faudroids.mrhyde.git.FileNode;
import org.roboguice.shaded.goole.common.base.Objects;

/**
 * One Jekyll draft comparable with other drafts via its title.
 */
public class Draft implements Comparable<Draft> {

	private final String title;
	private final FileNode fileNode;

	public Draft(String title, FileNode fileNode) {
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
		Draft draft = (Draft) o;
		return Objects.equal(title, draft.title) &&
				Objects.equal(fileNode, draft.fileNode);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title, fileNode);
	}

	@Override
	public int compareTo(Draft another) {
		return title.compareTo(another.title);
	}
}
