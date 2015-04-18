package org.faudroids.mrhyde.git;


import android.content.Context;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import roboguice.inject.ContextSingleton;

@ContextSingleton
public final class RepositoryManager {

	private final Context context;
	private final ApiWrapper apiWrapper;
	private final Map<String, FileManager> fileManagerMap = new HashMap<>();


	@Inject
	RepositoryManager(Context context, ApiWrapper apiWrapper) {
		this.context = context;
		this.apiWrapper = apiWrapper;
	}


	public FileManager getFileManager(Repository repository) {
		FileManager fileManager = fileManagerMap.get(repository.getName());
		if (fileManager == null) {
			fileManager = new FileManager(context, apiWrapper, repository);
			fileManagerMap.put(repository.getName(), fileManager);
		}
		return fileManager;
	}

}
