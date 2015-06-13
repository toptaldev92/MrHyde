package org.faudroids.mrhyde.git;


import android.content.Context;
import android.content.SharedPreferences;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.faudroids.mrhyde.github.GitHubApiWrapper;
import org.faudroids.mrhyde.github.LoginManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit.RetrofitError;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

@Singleton
public final class RepositoryManager {

	private static final String PREFS_NAME = RepositoryManager.class.getName();

	private final Context context;
	private final LoginManager loginManager;
	private final GitHubApiWrapper gitHubApiWrapper;
	private final FileUtils fileUtils;
	private final Map<String, FileManager> fileManagerMap = new HashMap<>();
	private Map<String, Repository> allRepositoryMap, favouriteRepositoriesMap;


	@Inject
	RepositoryManager(Context context, LoginManager loginManager, GitHubApiWrapper gitHubApiWrapper, FileUtils fileUtils) {
		this.context = context;
		this.loginManager = loginManager;
		this.gitHubApiWrapper = gitHubApiWrapper;
		this.fileUtils = fileUtils;
	}


	public FileManager getFileManager(Repository repository) {
		FileManager fileManager = fileManagerMap.get(getFullRepoName(repository));
		if (fileManager == null) {
			fileManager = new FileManager(context, loginManager, gitHubApiWrapper, repository, fileUtils);
			fileManagerMap.put(getFullRepoName(repository), fileManager);
		}
		return fileManager;
	}


	public Observable<Collection<Repository>> getAllRepositories() {
		if (allRepositoryMap != null) {
			return Observable.just(allRepositoryMap.values());
		} else {
			return Observable.zip(
					gitHubApiWrapper.getRepositories(),
					gitHubApiWrapper.getOrganizations()
							.flatMap(new Func1<List<User>, Observable<User>>() {
								@Override
								public Observable<User> call(List<User> users) {
									return Observable.from(users);
								}
							})
							.flatMap(new Func1<User, Observable<List<Repository>>>() {
								@Override
								public Observable<List<Repository>> call(User org) {
									return gitHubApiWrapper.getOrgRepositories(org.getLogin());
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
					.flatMap(new Func1<List<Repository>, Observable<Collection<Repository>>>() {
						@Override
						public Observable<Collection<Repository>> call(List<Repository> repositories) {
							allRepositoryMap = new HashMap<>();
							for (Repository repository : repositories) {
								allRepositoryMap.put(getFullRepoName(repository), repository);
							}
							return Observable.just(allRepositoryMap.values());
						}
					});
		}
	}


	public boolean hasFavouriteRepositories() {
		if (favouriteRepositoriesMap != null) return !favouriteRepositoriesMap.isEmpty();
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return !prefs.getAll().isEmpty();
	}


	public Observable<Collection<Repository>> getFavouriteRepositories() {
		// get cached values
		if (favouriteRepositoriesMap != null) return Observable.just(favouriteRepositoriesMap.values());

		// get favourite repos from all cached
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		Set<String> repoNames = prefs.getAll().keySet();

		if (allRepositoryMap != null) {
			favouriteRepositoriesMap = new HashMap<>();
			for (String repoName : repoNames) {
				Repository repo = allRepositoryMap.get(repoName);
				if (repo == null) {
					Timber.w("failed to find favourite repo " + repoName);
					unmarkRepositoryAsFavourite(repoName);
					continue;
				}
				favouriteRepositoriesMap.put(repoName, allRepositoryMap.get(repoName));
			}
			return Observable.just(favouriteRepositoriesMap.values());
		}

		// download favourite repos
		return Observable.from(repoNames)
				.flatMap(new Func1<String, Observable<Repository>>() {
					@Override
					public Observable<Repository> call(String repoName) {
						String[] repoParts = repoName.split("/");
						return gitHubApiWrapper.getRepository(repoParts[0], repoParts[1]);
					}
				})
				.onErrorResumeNext(new Func1<Throwable, Observable<Repository>>() {
					@Override
					public Observable<Repository> call(Throwable throwable) {
						// ignore 404's, as repo might no longer exist
						if (throwable instanceof RetrofitError) {
							RetrofitError error = (RetrofitError) throwable;
							if (error.getResponse() != null && error.getResponse().getStatus() == 404) {
								return null;
							}
						}
						return Observable.error(throwable);
					}
				})
				.toList()
				.flatMap(new Func1<List<Repository>, Observable<Collection<Repository>>>() {
					@Override
					public Observable<Collection<Repository>> call(List<Repository> repositories) {
						favouriteRepositoriesMap = new HashMap<>();
						for (Repository repo : repositories) favouriteRepositoriesMap.put(getFullRepoName(repo), repo);
						return Observable.<Collection<Repository>>just(repositories);
					}
				});
	}


	public void markRepositoryAsFavourite(Repository repository) {
		Timber.d("marking repo " + getFullRepoName(repository) + " as favourite");
		SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(getFullRepoName(repository), "");
		editor.commit();

		if (favouriteRepositoriesMap != null) {
			favouriteRepositoriesMap.put(getFullRepoName(repository), repository);
		}
	}


	public boolean isRepositoryFavourite(Repository repository) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return prefs.contains(getFullRepoName(repository));
	}


	public void unmarkRepositoryAsFavourite(Repository repository) {
		unmarkRepositoryAsFavourite(getFullRepoName(repository));
	}


	private void unmarkRepositoryAsFavourite(String repoName) {
		Timber.d("unmarking repo " + repoName + " as favourite");
		SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
		editor.remove(repoName);
		editor.commit();

		if (favouriteRepositoriesMap != null) {
			favouriteRepositoriesMap.remove(repoName);
		}
	}


	private String getFullRepoName(Repository repository) {
		return repository.getOwner().getLogin() + "/" + repository.getName();
	}

}
