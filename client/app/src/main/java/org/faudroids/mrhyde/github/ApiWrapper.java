package org.faudroids.mrhyde.github;


import org.eclipse.egit.github.core.Repository;
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

	@Inject
	public ApiWrapper(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}


	public Observable<List<Repository>> getRepositories() {
		return new Wrapper<List<Repository>>() {
			@Override
			protected List<Repository> doWrapMethod() throws Exception {
				return repositoryService.getRepositories();
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
