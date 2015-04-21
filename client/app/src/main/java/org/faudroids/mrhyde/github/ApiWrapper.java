package org.faudroids.mrhyde.github;


import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.util.Collection;
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


	public Observable<Commit> createCommit(final IRepositoryIdProvider repository, final Commit commit) {
		return new Wrapper<Commit>() {
			@Override
			protected Commit doWrapMethod() throws Exception {
				return dataService.createCommit(repository, commit);
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


	public Observable<Tree> createTree(final IRepositoryIdProvider repository, final Collection<TreeEntry> entries, final String baseTreeSha) {
		return new Wrapper<Tree>() {
			@Override
			protected Tree doWrapMethod() throws Exception {
				return dataService.createTree(repository, entries, baseTreeSha);
			}
		}.wrapMethod();
	}


	public Observable<Blob> getBlob(final IRepositoryIdProvider repository, final String sha) {
		return new Wrapper<Blob>() {
			@Override
			protected Blob doWrapMethod() throws Exception {
				return dataService.getBlob(repository, sha);
			}
		}.wrapMethod();
	}


	public Observable<String> createBlob(final IRepositoryIdProvider repository, final Blob blob) {
		return new Wrapper<String>() {
			@Override
			protected String doWrapMethod() throws Exception {
				return dataService.createBlob(repository, blob);
			}
		}.wrapMethod();
	}


	public Observable<Reference> getReference(final IRepositoryIdProvider repository, final String ref) {
		return new Wrapper<Reference>() {
			@Override
			protected Reference doWrapMethod() throws Exception {
				return dataService.getReference(repository, ref);
			}
		}.wrapMethod();
	}


	public Observable<Reference> editReference(final IRepositoryIdProvider repository, final Reference reference) {
		return new Wrapper<Reference>() {
			@Override
			protected Reference doWrapMethod() throws Exception {
				return dataService.editReference(repository, reference);
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
