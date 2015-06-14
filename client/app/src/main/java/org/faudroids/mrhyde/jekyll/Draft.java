package org.faudroids.mrhyde.jekyll;

import org.roboguice.shaded.goole.common.base.Objects;

/**
 * One Jekyll draft comparable with other drafts via its title.
 */
public class Draft implements Comparable<Draft> {

	private final String title;

	public Draft(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Draft draft = (Draft) o;
		return Objects.equal(title, draft.title);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title);
	}

	@Override
	public int compareTo(Draft another) {
		return title.compareTo(another.title);
	}
}
