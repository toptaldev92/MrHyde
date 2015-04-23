package org.faudroids.mrhyde.git;


import android.content.Context;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

@Singleton
public final class RepositoryManager {

	private final Context context;
	private final ApiWrapper apiWrapper;
	private final Map<String, FileManager> fileManagerMap = new HashMap<>();
	private List<Repository> repositories;


	@Inject
	RepositoryManager(Context context, ApiWrapper apiWrapper) {
		this.context = context;
		this.apiWrapper = apiWrapper;
	}


	public FileManager getFileManager(Repository repository) {
		FileManager fileManager = fileManagerMap.get(getFullRepoName(repository));
		if (fileManager == null) {
			fileManager = new FileManager(context, apiWrapper, repository);
			fileManagerMap.put(getFullRepoName(repository), fileManager);
		}
		return fileManager;
	}


	public Observable<List<Repository>> getRepositories() {
		if (repositories != null) {
			return Observable.just(repositories);
		} else {
			return Observable.zip(
					apiWrapper.getRepositories(),
					apiWrapper.getOrganizations()
							.flatMap(new Func1<List<User>, Observable<User>>() {
								@Override
								public Observable<User> call(List<User> users) {
									return Observable.from(users);
								}
							})
							.flatMap(new Func1<User, Observable<List<Repository>>>() {
								@Override
								public Observable<List<Repository>> call(User org) {
									return apiWrapper.getOrgRepositories(org.getLogin());
								}
							})
							.toList(),
					new Func2<List<Repository>, List<List<Repository>>, List<Repository>>() {
						@Override
						public List<Repository> call(List<Repository> userRepos, List<List<Repository>> orgRepos) {
							List<Repository> allRepos = new ArrayList<>(userRepos);
							for (List<Repository> repos : orgRepos) allRepos.addAll(repos);
							return allRepos;
						}
					})
					.flatMap(new Func1<List<Repository>, Observable<List<Repository>>>() {
						@Override
						public Observable<List<Repository>> call(List<Repository> repositories) {
							RepositoryManager.this.repositories = repositories;
							return Observable.just(repositories);
						}
					});
		}
	}


	private String getFullRepoName(Repository repository) {
		return repository.getOwner().getLogin() + "/" + repository.getName();
	}

}
