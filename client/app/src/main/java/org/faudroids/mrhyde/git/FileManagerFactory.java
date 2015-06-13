package org.faudroids.mrhyde.git;


import android.content.Context;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.github.GitHubApiWrapper;
import org.faudroids.mrhyde.github.LoginManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class FileManagerFactory {

	private final Context context;
	private final LoginManager loginManager;
	private final GitHubApiWrapper gitHubApiWrapper;
	private final FileUtils fileUtils;
	private final Map<String, FileManager> managerCache = new HashMap<>();


	@Inject
	FileManagerFactory(
			Context context,
			LoginManager loginManager,
			GitHubApiWrapper gitHubApiWrapper,
			FileUtils fileUtils) {

		this.context = context;
		this.loginManager = loginManager;
		this.gitHubApiWrapper = gitHubApiWrapper;
		this.fileUtils = fileUtils;
	}


	/**
	 * Creates and caches one {@link FileManager} instance.
	 */
	public FileManager createFileManager(Repository repository) {
		FileManager fileManager = managerCache.get(getFullRepoName(repository));
		if (fileManager == null) {
			fileManager = new FileManager(context, loginManager, gitHubApiWrapper, repository, fileUtils);
			managerCache.put(getFullRepoName(repository), fileManager);
		}
		return fileManager;
	}


	private String getFullRepoName(Repository repository) {
		return repository.getOwner().getLogin() + "/" + repository.getName();
	}

}
