package org.faudroids.mrhyde.git;

import android.content.Context;
import android.util.Base64;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

public final class FileManager {

	private final GitManager gitManager;
	private final ApiWrapper apiWrapper;
	private final Repository repository;
	private final File rootDir;

	private Tree tree = null;


	FileManager(Context context, ApiWrapper apiWrapper, Repository repository) {
		this.apiWrapper = apiWrapper;
		this.repository = repository;
		this.rootDir = new File(context.getFilesDir(), repository.getName());
		this.gitManager = new GitManager(rootDir);
	}


	/**
	 * Gets and stores a tree in memory (!).
	 */
	public Observable<Tree> getTree() {
		if (tree != null) return Observable.just(tree);
		else return apiWrapper.getCommits(repository)
				.flatMap(new Func1<List<RepositoryCommit>, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(List<RepositoryCommit> repositoryCommits) {
						String sha = repositoryCommits.get(0).getSha();
						return apiWrapper.getTree(repository, sha, true);
					}
				})
				.flatMap(new Func1<Tree, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(Tree tree) {
						FileManager.this.tree = tree;
						return Observable.just(tree);
					}
				})
				.compose(new InitRepoTransformer<Tree>());
	}


	/**
	 * Gets and stores a file on disk (!).
	 */
	public Observable<String> getFile(TreeEntry treeEntry) {
		final File file = new File(rootDir, treeEntry.getPath());

		if (file.exists()) {
			return Observable.defer(new Func0<Observable<String>>() {
				@Override
				public Observable<String> call() {
					return Observable.just(readFile(file));
				}
			});

		} else {
			return apiWrapper.getBlob(repository, treeEntry.getSha())
					.flatMap(new Func1<Blob, Observable<String>>() {
						@Override
						public Observable<String> call(Blob blob) {
							String content;
							if (blob.getEncoding().equals(Blob.ENCODING_UTF8)) {
								content = blob.getContent();
							} else {
								byte[] bytes = Base64.decode(blob.getContent(), Base64.DEFAULT);
								try {
									content = new String(bytes, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									throw new RuntimeException(e);
								}
							}

							writeFile(file, content);
							return Observable.just(content);
						}
					})
					.flatMap(gitManager.<String>commit(file));
		}
	}


	/**
	 * Stores the content on disk.
	 */
	public void writeFile(TreeEntry treeEntry, String content) {
		writeFile(new File(rootDir, treeEntry.getPath()), content);
	}


	public Observable<String> getDiff() {
		return gitManager.diff();
	}




	private String readFile(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			StringBuilder builder = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append('\n');
			}
			return builder.toString();

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ioe) {
					Timber.e(ioe, "failed to close writer");
				}
			}
		}
	}


	private void writeFile(File file, String content) {
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) Timber.w("failed to create dirs " + parentDir.getPath());
		}

		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ioe) {
					Timber.e(ioe, "failed to close writer");
				}
			}
		}
	}



	private final class InitRepoTransformer<T> implements Observable.Transformer<T, T> {
		@Override
		public Observable<T> call(Observable<T> observable) {
			if (!rootDir.exists() && !rootDir.mkdirs()) {
				Timber.w("failed to create root dir");
			}

			try {
				if (!gitManager.exists()) gitManager.init();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			return observable;
		}
	}
}
