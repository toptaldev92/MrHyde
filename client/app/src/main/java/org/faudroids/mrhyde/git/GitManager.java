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


	public void init() throws IOException, GitAPIException {
		Timber.d("init repository " + rootDir.getPath());
		Git git = null;
		try {
			git = Git.init().setDirectory(rootDir).call();
		} finally {
			if (git != null) git.close();
		}
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


	public Observable<Set<String>> getChangedFiles() {
		return Observable.defer(new Func0<Observable<Set<String>>>() {
			@Override
			public Observable<Set<String>> call() {
				Git git = null;
				try {
					git = Git.open(rootDir);
					Status status = git.status().call();
					return Observable.just(status.getUncommittedChanges());

				} catch (IOException | GitAPIException e) {
					throw new RuntimeException(e);
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
