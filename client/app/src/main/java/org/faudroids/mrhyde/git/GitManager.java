package org.faudroids.mrhyde.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

public final class GitManager {

	private final File rootDir;

	GitManager(File rootDir) {
		this.rootDir = rootDir;
	}


	public boolean exists() {
		File gitDir = new File(rootDir, ".git");
		return gitDir.exists();
	}


	public Observable<Void> init() {
		return Observable.defer(new Func0<Observable<Void>>() {
			@Override
			public Observable<Void> call() {
				Timber.d("init repository " + rootDir.getPath());
				Git git = null;
				try {
					git = Git.init().setDirectory(rootDir).call();
					git.commit().setMessage("initial empty commit").call();
					return Observable.just(null);

				} catch (GitAPIException e) {
					return Observable.error(e);

				} finally {
					if (git != null) git.close();
				}
			}
		});
	}


	public<T> Func1<T, Observable<T>> commit(final File file) {
		return new AbstractFunc<T>() {
			@Override
			protected void doCall(Git git) throws IOException, GitAPIException {
				String fileName = getRepoFileName(file);
				Timber.d("adding and committing file " + fileName);
				git.add().addFilepattern(fileName).call();
				git.commit().setMessage("downloaded " + fileName).call();
			}
		};
	}


	public Observable<String> diff() {
		return diff(null);
	}


	public Observable<String> diff(final Set<String> filesToIgnore) {
		return Observable.defer(new Func0<Observable<String>>() {
			@Override
			public Observable<String> call() {
				Git git = null;
				try {
					git = Git.open(rootDir);
					AbstractTreeIterator commitTreeIterator = prepareCommitTreeIterator(git.getRepository());
					FileTreeIterator workTreeIterator = new FileTreeIterator(git.getRepository());

					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					DiffFormatter formatter = new DiffFormatter(outputStream);
					formatter.setRepository(git.getRepository());

					List<DiffEntry> diffs = formatter.scan(commitTreeIterator, workTreeIterator);
					// remove ignored files
					if (filesToIgnore != null) {
						Iterator<DiffEntry> entryIterator = diffs.iterator();
						while (entryIterator.hasNext()) {
							if (filesToIgnore.contains(entryIterator.next().getNewPath())) {
								entryIterator.remove();
							}
						}
					}

					formatter.format(diffs);
					return Observable.just(outputStream.toString());

				} catch (IOException | GitAPIException e) {
					throw new RuntimeException(e);
				} finally {
					if (git != null) git.close();
				}
			}
		});
	}


	/**
	 * Returns all files that have been changed (including empty dirs), regardless if they are new or not.
	 * This does not include deleted files.
	 */
	public Observable<Set<String>> getChangedFiles() {
		return getStatus().flatMap(new Func1<Status, Observable<Set<String>>>() {
			@Override
			public Observable<Set<String>> call(Status status) {
				Set<String> allFiles = status.getUncommittedChanges();
				allFiles.addAll(status.getUntracked());
				allFiles.addAll(status.getUntrackedFolders());
				allFiles.removeAll(status.getMissing());
				return Observable.just(allFiles);
			}
		});
	}


	/**
	 * Returns only new files.
	 */
	public Observable<Set<String>> getDeletedFiles() {
		return getStatus().flatMap(new Func1<Status, Observable<Set<String>>>() {
			@Override
			public Observable<Set<String>> call(Status status) {
				return Observable.just(status.getMissing());
			}
		});
	}


	private Observable<Status> getStatus() {
		return Observable.defer(new Func0<Observable<Status>>() {
			@Override
			public Observable<Status> call() {
				Git git = null;
				try {
					git = Git.open(rootDir);
					Status status = git.status().call();
					return Observable.just(status);

				} catch (IOException | GitAPIException e) {
					return Observable.error(e);
				} finally {
					if (git != null) git.close();
				}
			}
		});
	}



	/**
	 * Returns the full filename according to the base path.
	 */
	private String getRepoFileName(File file) {
		return file.getPath().substring(rootDir.getPath().length() + 1);
	}


	private static AbstractTreeIterator prepareCommitTreeIterator(Repository repository) throws IOException, GitAPIException {
		Ref head = repository.getRef(repository.getBranch());
		RevWalk walk = new RevWalk(repository);
		RevCommit commit = walk.parseCommit(head.getObjectId());
		RevTree tree = walk.parseTree(commit.getTree().getId());

		CanonicalTreeParser parser = new CanonicalTreeParser();
		ObjectReader reader = repository.newObjectReader();
		try {
			parser.reset(reader, tree.getId());
		} finally {
			reader.release();
		}
		walk.dispose();
		return parser;
	}


	private abstract class AbstractFunc<T> implements Func1<T, Observable<T>> {

		@Override
		public Observable<T> call(T value) {
			Git git = null;
			try {
				git = Git.open(rootDir);
				doCall(git);
			} catch (IOException | GitAPIException e) {
				throw new RuntimeException(e);
			} finally {
				if (git != null) git.close();
			}
			return Observable.just(value);
		}

		protected abstract void doCall(Git git) throws IOException, GitAPIException;

	}

}
