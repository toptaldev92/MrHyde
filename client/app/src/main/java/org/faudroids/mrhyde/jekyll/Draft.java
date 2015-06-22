package org.faudroids.mrhyde.jekyll;

import org.faudroids.mrhyde.git.FileNode;

/**
 * One Jekyll draft comparable with other drafts via its title.
 */
public class Draft extends AbstractJekyllContent implements Comparable<Draft> {

	public Draft(String title, FileNode fileNode) {
		super(title, fileNode);
	}


	@Override
	public int compareTo(Draft another) {
		return title.compareTo(another.title);
	}
}
