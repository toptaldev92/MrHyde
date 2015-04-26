package org.faudroids.mrhyde.git;

import android.content.Context;
import android.util.Base64;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.TypedResource;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

public final class FileManager {

	private final GitManager gitManager;
	private final ApiWrapper apiWrapper;
	private final Repository repository;
	private final File rootDir;

	private Tree cachedTree = null;
	private String cachedBaseCommitSha = null;


	FileManager(Context context, ApiWrapper apiWrapper, Repository repository) {
		this.apiWrapper = apiWrapper;
		this.repository = repository;
		this.rootDir = new File(context.getFilesDir(), repository.getOwner().getLogin() + "/" + repository.getName());
		this.gitManager = new GitManager(rootDir);
	}


	/**
	 * Gets and stores a tree in memory (!).
	 */
	public Observable<Tree> getTree() {
		if (cachedTree != null) return Observable.just(cachedTree)
				.compose(new InitRepoTransformer<Tree>())
				.flatMap(new LoadLocalFilesFunc());
		else return apiWrapper.getCommits(repository)
				.flatMap(new Func1<List<RepositoryCommit>, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(List<RepositoryCommit> repositoryCommits) {
						Timber.d("loaded last commit");
						cachedBaseCommitSha = repositoryCommits.get(0).getSha();
						return apiWrapper.getTree(repository, cachedBaseCommitSha, true);
					}
				})
				.flatMap(new Func1<Tree, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(Tree tree) {
						Timber.d("loaded tree");
						FileManager.this.cachedTree = tree;
						return Observable.just(tree);
					}
				})
				.compose(new InitRepoTransformer<Tree>())
				.flatMap(new LoadLocalFilesFunc());
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
			Timber.d("getting file with sha " + treeEntry.getSha());
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

							try {
								writeFile(file, content);
							} catch (IOException e) {
								return Observable.error(e);
							}
							return Observable.just(content);
						}
					})
					.flatMap(gitManager.<String>commit(file));
		}
	}


	/**
	 * Creates a new tree entry which can be used later on to save content. It does NOT
	 * touch the file system.
	 */
	public TreeEntry createNewTreeEntry(TreeEntry parentEntry, String fileName) {
		String path = fileName;
		if (parentEntry != null) path = parentEntry.getPath() + "/" + path;
		return new DummyTreeEntry(path, TreeEntry.MODE_BLOB);
	}


	/**
	 * Stores the content on disk.
	 */
	public void writeFile(TreeEntry treeEntry, String content) throws IOException {
		Timber.d("writing file " + treeEntry.getPath());
		File file = new File(rootDir, treeEntry.getPath());
		if (!file.exists()) file.createNewFile();
		writeFile(file, content);
	}


	public Observable<Void> commit() {
		return getChangedFiles()
				// get changed files
				.flatMap(new Func1<Set<String>, Observable<String>>() {
					@Override
					public Observable<String> call(Set<String> filePaths) {
						for (String file : filePaths) Timber.d("committing file " + file);
						return Observable.from(filePaths);
					}
				})
				// store blobs on GitHub
				.flatMap(new Func1<String, Observable<SavedBlob>>() {
					@Override
					public Observable<SavedBlob> call(final String filePath) {
						final Blob blob = new Blob();
						blob.setContent(readFile(new File(rootDir, filePath)))
								.setEncoding(Blob.ENCODING_UTF8);
						return apiWrapper.createBlob(repository, blob)
								.flatMap(new Func1<String, Observable<SavedBlob>>() {
									@Override
									public Observable<SavedBlob> call(String blobSha) {
										return Observable.just(new SavedBlob(filePath, blobSha, blob));
									}
								});
					}
				})
				// wait for all blobs
				.toList()
				// create new git tree on GitHub
				.flatMap(new Func1<List<SavedBlob>, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(List<SavedBlob> savedBlobs) {
						Collection<TreeEntry> treeEntries = new ArrayList<>();
						for (SavedBlob savedBlob : savedBlobs) {
							TreeEntry treeEntry = new TreeEntry();
							treeEntry.setPath(savedBlob.path);
							treeEntry.setMode(TreeEntry.MODE_BLOB);
							treeEntry.setType(TreeEntry.TYPE_BLOB);
							treeEntry.setSha(savedBlob.sha);
							treeEntry.setSize(savedBlob.blob.getContent().length());
							treeEntries.add(treeEntry);
						}
						return apiWrapper.createTree(repository, treeEntries, cachedTree.getSha());
					}
				})
				// create new commit on GitHub
				.flatMap(new Func1<Tree, Observable<Commit>>() {
					@Override
					public Observable<Commit> call(Tree newTree) {
						Commit commit = new Commit();
						commit.setMessage("MrHyde update");
						commit.setTree(newTree);

						CommitUser author = new CommitUser();
						author.setName("MrHyde");
						author.setEmail("faudroids@gmail.com");
						author.setDate(Calendar.getInstance().getTime());
						commit.setAuthor(author);
						commit.setCommitter(author);

						List<Commit> commitList = new ArrayList<>();
						commitList.add(new Commit().setSha(cachedBaseCommitSha));
						commit.setParents(commitList);
						return apiWrapper.createCommit(repository, commit);
					}
				})
				// get and update reference from GitHub
				.flatMap(new Func1<Commit, Observable<Reference>>() {
					@Override
					public Observable<Reference> call(Commit commit) {
						final TypedResource commitResource = new TypedResource();
						commitResource.setSha(commit.getSha());
						commitResource.setType(TypedResource.TYPE_COMMIT);
						commitResource.setUrl(commit.getUrl());

						return apiWrapper.getReference(repository, "heads/master")
								.flatMap(new Func1<Reference, Observable<Reference>>() {
									@Override
									public Observable<Reference> call(Reference reference) {
										reference.setObject(commitResource);
										return apiWrapper.editReference(repository, reference);
									}
								});
					}
				})
				// cleanup
				.flatMap(new Func1<Reference, Observable<Void>>() {
					@Override
					public Observable<Void> call(Reference reference) {
						cachedTree = null;
						cachedBaseCommitSha = null;
						try {
							delete(rootDir);
						} catch (IOException ioe) {
							throw new RuntimeException(ioe);
						}
						return Observable.just(null);
					}
				});
	}


	public Observable<String> getDiff() {
		return gitManager.diff();
	}


	public Observable<Set<String>> getChangedFiles() {
		return gitManager.getChangedFiles();
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


	private void writeFile(File file, String content) throws IOException {
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) Timber.w("failed to create dirs " + parentDir.getPath());
		}

		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			writer.write(content);
			writer.close();
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


	private void delete(File file) throws IOException {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}


	private final class InitRepoTransformer<T> implements Observable.Transformer<T, T> {
		@Override
		public Observable<T> call(final Observable<T> observable) {
			if (!rootDir.exists() && !rootDir.mkdirs()) {
				Timber.w("failed to create root dir");
			}
			if (!gitManager.exists()) {
				return gitManager.init()
						.flatMap(new Func1<Void, Observable<T>>() {
							@Override
							public Observable<T> call(Void aVoid) {
								return observable;
							}
						});
			} else {
				return observable;
			}
		}
	}


	private final class LoadLocalFilesFunc implements Func1<Tree, Observable<Tree>> {
		@Override
		public Observable<Tree> call(final Tree tree) {
			return gitManager.getNewFiles().flatMap(new Func1<Set<String>, Observable<Tree>>() {
				@Override
				public Observable<Tree> call(Set<String> files) {
					Timber.d("loading local files");
					List<TreeEntry> entries = tree.getTree();
					for (String file : files) {
						entries.add(new DummyTreeEntry(file, TreeEntry.MODE_BLOB));
					}
					Tree newTree = new Tree();
					newTree.setTree(entries);
					newTree.setUrl(tree.getUrl());
					newTree.setSha(tree.getSha());
					return Observable.just(newTree);
				}
			});
		}
	}


	private static final class DummyTreeEntry extends TreeEntry {

		public DummyTreeEntry(String path, String mode) {
			setPath(path);
			setMode(mode);
		}

		@Override
		public String getSha() { throw new UnsupportedOperationException("dummy"); }

		@Override
		public long getSize() { throw new UnsupportedOperationException("dummy"); }

		@Override
		public String getType() { throw new UnsupportedOperationException("dummy"); }

		@Override
		public String getUrl() { throw new UnsupportedOperationException("dummy"); }

	}


	private static final class SavedBlob {

		private final String path;
		private final String sha;
		private final Blob blob;

		public SavedBlob(String path, String sha, Blob blob) {
			this.path = path;
			this.sha = sha;
			this.blob = blob;
		}

	}


}
