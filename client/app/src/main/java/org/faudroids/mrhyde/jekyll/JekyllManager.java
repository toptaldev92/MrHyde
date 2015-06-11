package org.faudroids.mrhyde.jekyll;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles Jekyll specific tasks for one repository.
 */
public class JekyllManager {

	@Inject
	JekyllManager() { }


	public List<String> getAllPosts() {
		return new ArrayList<>();
	}


	public List<String> getAllDrafts() {
		return new ArrayList<>();
	}

}
