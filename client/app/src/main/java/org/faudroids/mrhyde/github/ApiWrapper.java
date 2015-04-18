package org.faudroids.mrhyde.github;


import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;

/**
 * Simple wrapper class for converting the GitHuh client methods into an async version
 * by using RxJava.
 *
 * Methods are added as needed.
 */
public final class ApiWrapper {

	private final RepositoryService repositoryService;
	private final CommitService commitService;
	private final DataService dataService;

	@Inject
	public ApiWrapper(
			RepositoryService repositoryService,
			CommitService commitService,
			DataService dataService) {

		this.repositoryService = repositoryService;
		this.commitService = commitService;
		this.dataService = dataService;
	}


	public Observable<List<Repository>> getRepositories() {
		return new Wrapper<List<Repository>>() {
			@Override
			protected List<Repository> doWrapMethod() throws Exception {
				return repositoryService.getRepositories();
			}
		}.wrapMethod();
	}


	public Observable<List<RepositoryCommit>> getCommits(final IRepositoryIdProvider repository) {
		return new Wrapper<List<RepositoryCommit>>() {
			@Override
			protected List<RepositoryCommit> doWrapMethod() throws Exception {
				return commitService.getCommits(repository);
			}
		}.wrapMethod();
	}


	public Observable<Tree> getTree(final IRepositoryIdProvider repository, final String sha, final boolean recursive) {
		return new Wrapper<Tree>() {
			@Override
			protected Tree doWrapMethod() throws Exception {
				return dataService.getTree(repository, sha, recursive);
			}
		}.wrapMethod();
	}


	private static abstract class Wrapper<T> {

		public Observable<T> wrapMethod() {
			return Observable.defer(new Func0<Observable<T>>() {
				@Override
				public Observable<T> call() {
					try {
						return Observable.just(doWrapMethod());
					} catch (Exception e) {
						throw OnErrorThrowable.from(e);
					}

				}
			});
		}

		protected abstract T doWrapMethod() throws Exception;

	}

}
