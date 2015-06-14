package org.faudroids.mrhyde.jekyll;

import org.faudroids.mrhyde.git.FileNode;
import org.roboguice.shaded.goole.common.base.Objects;

import java.util.Date;

/**
 * One Jekyll post. Can be compared to other posts via the creation date.
 */
public class Post implements Comparable<Post> {

	private final String title;
	private final Date date;
	private final FileNode fileNode;

	public Post(String title, Date date, FileNode fileNode) {
		this.title = title;
		this.date = date;
		this.fileNode = fileNode;
	}

	public String getTitle() {
		return title;
	}

	public Date getDate() {
		return date;
	}

	public FileNode getFileNode() {
		return fileNode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Post post = (Post) o;
		return Objects.equal(title, post.title) &&
				Objects.equal(date, post.date) &&
				Objects.equal(fileNode, post.fileNode);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title, date, fileNode);
	}

	@Override
	public int compareTo(Post another) {
		return date.compareTo(another.date);
	}

}
