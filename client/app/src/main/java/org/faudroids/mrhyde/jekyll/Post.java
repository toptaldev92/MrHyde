package org.faudroids.mrhyde.jekyll;

import org.roboguice.shaded.goole.common.base.Objects;

import java.util.Date;

/**
 * One Jekyll post. Can be compared to other posts via the creation date.
 */
public class Post implements Comparable<Post> {

	private final String title;
	private final Date date;

	public Post(String title, Date date) {
		this.title = title;
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public Date getDate() {
		return date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Post post = (Post) o;
		return Objects.equal(title, post.title) &&
				Objects.equal(date, post.date);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title, date);
	}

	@Override
	public int compareTo(Post another) {
		return date.compareTo(another.date);
	}

}
