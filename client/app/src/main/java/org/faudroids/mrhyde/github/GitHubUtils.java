package org.faudroids.mrhyde.github;

import org.eclipse.egit.github.core.Repository;

import javax.inject.Inject;

/**
 * Various helper classes for GitHub related tasks.
 */
public class GitHubUtils {

	@Inject
	GitHubUtils() { }


	/**
	 * Returns {user}/{repository name}.
	 */
	public String getFullRepoName(Repository repository) {
		return repository.getOwner().getLogin() + "/" + repository.getName();
	}

}
