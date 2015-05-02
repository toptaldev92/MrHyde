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
import org.faudroids.mrhyde.github.LoginManager;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

public final class FileManager {

	private final LoginManager loginManager;
	private final GitManager gitManager;
	private final ApiWrapper apiWrapper;
	private final Repository repository;
	private final File rootDir;

	private Tree cachedTree = null;
	private String cachedBaseCommitSha = null;


	FileManager(Context context, LoginManager loginManager, ApiWrapper apiWrapper, Repository repository) {
		this.loginManager = loginManager;
		this.apiWrapper = apiWrapper;
		this.repository = repository;
		this.rootDir = new File(context.getFilesDir(), repository.getOwner().getLogin() + "/" + repository.getName());
		this.gitManager = new GitManager(rootDir);
	}


	/**
	 * Gets and stores a tree in memory (!).
	 */
	public Observable<DirNode> getTree() {
		if (cachedTree != null) return Observable.just(cachedTree)
				.compose(new InitRepoTransformer<Tree>())
				.flatMap(new GitHubParseFunc())
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
				.flatMap(new GitHubParseFunc())
				.flatMap(new LoadLocalFilesFunc());
	}


	/**
	 * Gets and stores a file on disk (!).
	 */
	public Observable<String> getFile(FileNode fileNode) {
		final TreeEntry treeEntry = fileNode.getTreeEntry();
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
	 * Creates a new file node and updates the file system.
	 */
	public FileNode createNewFile(DirNode parentNode, String fileName) {
		// create node
		String path = fileName;
		if (parentNode.getTreeEntry() != null) path = parentNode.getTreeEntry().getPath() + "/" + path;
		FileNode fileNode = new FileNode(parentNode, fileName, new DummyTreeEntry(path, TreeEntry.MODE_BLOB));
		parentNode.getEntries().put(fileName, fileNode);

		// create file
		Timber.d("creating new file " + path);
		try {
			File file = new File(rootDir, path);
			if (!file.exists()) if (!file.createNewFile()) Timber.w("failed to create new file");
		} catch (IOException ioe) {
			Timber.e(ioe, "failed to create file");
		}

		return fileNode;
	}


	/**
	 * Stores the content on disk.
	 */
	public void writeFile(FileNode fileNode, String content) throws IOException {
		Timber.d("writing file " + fileNode.getTreeEntry().getPath());
		File file = new File(rootDir, fileNode.getTreeEntry().getPath());
		if (!file.exists()) if (!file.createNewFile()) Timber.w("failed to create new file");
		writeFile(file, content);
	}


	/**
	 * Creates a new {@link DirNode} and updates the file system.
	 */
	public DirNode createNewDir(DirNode parentNode, String dirName) {
		// create node
		String path = dirName;
		if (parentNode.getTreeEntry() != null) path = parentNode.getTreeEntry().getPath() + "/" + path;
		DirNode dirNode = new DirNode(parentNode, dirName, new DummyTreeEntry(path, TreeEntry.MODE_DIRECTORY));
		parentNode.getEntries().put(dirName, dirNode);

		// create dir
		Timber.d("creating new dir " + path);
		File file = new File(rootDir, path);
		if (!file.exists()) if (!file.mkdir()) Timber.w("did not create dir");

		return dirNode;
	}


	/**
	 * Deletes a node from the file system. Do NOT pass in the root node!
	 */
	public Observable<Void> deleteFile(final FileNode node) {
		// first download file to ensure proper diff
		return getFile(node)
				.flatMap(new Func1<String, Observable<Void>>() {
					@Override
					public Observable<Void> call(String fileContent) {
						// remove from tree
						DirNode parentNode = (DirNode) node.getParent();
						parentNode.getEntries().remove(node.getPath());

						// delete files
						Timber.d("deleting file " + node.getTreeEntry().getPath());
						try {
							File file = new File(rootDir, node.getTreeEntry().getPath());
							delete(file);
						} catch (IOException ioe) {
							Timber.e(ioe, "failed to create file");
						}

						return Observable.just(null);
					}
				});
	}


	public Observable<Void> commit(final String commitMessage) {
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
						File file = new File(rootDir, filePath);
						if (file.isDirectory()) {
							Timber.d("ignoring dir " + filePath + " for blob creation");
							return Observable.empty();

						} else {
							blob.setContent(readFile(file)).setEncoding(Blob.ENCODING_UTF8);
							return apiWrapper.createBlob(repository, blob)
									.flatMap(new Func1<String, Observable<SavedBlob>>() {
										@Override
										public Observable<SavedBlob> call(String blobSha) {
											return Observable.just(new SavedBlob(filePath, blobSha, blob));
										}
									});
						}
					}
				})
				// wait for all blobs
				.toList()
				// construct the final tree
				.zipWith(gitManager.getDeletedFiles(), new Func2<List<SavedBlob>, Set<String>, Collection<TreeEntry>>() {
					@Override
					public Collection<TreeEntry> call(List<SavedBlob> savedBlobs, Set<String> deletedFiles) {
						// send the full tree, everything not included will be marked as deleted
						Map<String, SavedBlob> blobsMap = new HashMap<>();
						for (SavedBlob blob : savedBlobs) {
							blobsMap.put(blob.path, blob);
						}

						// update existing files
						Collection<TreeEntry> treeEntries = new ArrayList<>();
						for (TreeEntry entry : cachedTree.getTree()) {
							SavedBlob blob = blobsMap.remove(entry.getPath());

							// do not add deleted files
							if (deletedFiles.contains(entry.getPath())) {
								Timber.d("ignoring " + entry.getPath() + " during commit");
								continue;
							}

							// ignore existing trees (creates new ones on GitHub)
							if (entry.getMode().equals(TreeEntry.MODE_DIRECTORY)) {
								Timber.d("ignoring " + entry.getPath() + " during commit");
								continue;
							}

							TreeEntry newEntry;
							if (blob != null) {
								blobsMap.remove(entry.getPath());
								newEntry = createTreeEntryFromBlob(blob);
							} else {
								newEntry = entry;
							}
							Timber.d("adding " + newEntry.getPath() + " to commit");
							treeEntries.add(newEntry);
						}

						// add new files
						for (SavedBlob blob : blobsMap.values()) {
							Timber.d("adding " + blob.path + " to commit");
							treeEntries.add(createTreeEntryFromBlob(blob));
						}

						return treeEntries;
					}
				})
				// create tree on GitHub
				.flatMap(new Func1<Collection<TreeEntry>, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(Collection<TreeEntry> treeEntries) {
						return apiWrapper.createTree(repository, treeEntries);
					}
				})
				// create new commit on GitHub
				.flatMap(new Func1<Tree, Observable<Commit>>() {
					@Override
					public Observable<Commit> call(Tree newTree) {
						Commit commit = new Commit();
						commit.setMessage(commitMessage);
						commit.setTree(newTree);

						CommitUser author = new CommitUser();
						author.setName(loginManager.getAccount().getLogin());
						author.setEmail(loginManager.getAccount().getEmail());
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
						resetRepository();
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


	public Observable<Set<String>> getDeletedFiles() {
		return gitManager.getDeletedFiles();
	}


	public void resetRepository() {
		this.cachedTree = null;
		this.cachedBaseCommitSha = null;
		try {
			delete(rootDir);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
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


	private TreeEntry createTreeEntryFromBlob(SavedBlob blob) {
		TreeEntry entry = new TreeEntry();
		entry.setPath(blob.path);
		entry.setMode(TreeEntry.MODE_BLOB);
		entry.setType(TreeEntry.TYPE_BLOB);
		entry.setSha(blob.sha);
		entry.setSize(blob.blob.getContent().length());
		return entry;
	}


	/**
	 * Ensures that the local git repository exists.
	 */
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


	/**
	 * Parse a tree returned by GitHub into a tree like structure.
	 */
	private final class GitHubParseFunc implements Func1<Tree, Observable<DirNode>> {

		@Override
		public Observable<DirNode> call(Tree gitTree) {
			Timber.d("parsing GitHub tree");
			final DirNode rootNode = new DirNode(null, "", null);
			for (TreeEntry gitEntry : gitTree.getTree()) {
				String[] paths = gitEntry.getPath().split("/");

				DirNode parentNode = rootNode;
				for (int i = 0; i < paths.length; ++i) {
					String path = paths[i];
					if (i == paths.length - 1) {
						// commit leaf
						if (gitEntry.getMode().equals(TreeEntry.MODE_DIRECTORY)) {
							parentNode.getEntries().put(path, new DirNode(parentNode, path, gitEntry));
						} else {
							parentNode.getEntries().put(path, new FileNode(parentNode, path, gitEntry));
						}

					} else {
						parentNode = (DirNode) parentNode.getEntries().get(path);
					}
				}
			}
			return Observable.just(rootNode);
		}

	}


	/**
	 * Loads changes which have not yet been pushed to GitHub and adds those to the local tree
	 * representation.
	 */
	private final class LoadLocalFilesFunc implements Func1<DirNode, Observable<DirNode>> {
		@Override
		public Observable<DirNode> call(final DirNode rootNode) {
			addLocalNodes("", rootDir, rootNode);
			return removeDeletedNodes(rootNode);
		}

		private void addLocalNodes(String rootPath, File dir, DirNode rootNode) {
			for (File file : dir.listFiles()) {
				String fileName = file.getName();
				String path = rootPath + fileName;

				// ignore .git folder
				if (fileName.equals(".git")) continue;

				// load local files
				if (file.isDirectory()) {
					DirNode newRootNode = (DirNode) rootNode.getEntries().get(fileName);
					if (newRootNode == null) {
						Timber.d("loading dir " + fileName);
						newRootNode = new DirNode(rootNode, fileName, new DummyTreeEntry(path, TreeEntry.MODE_DIRECTORY));
						rootNode.getEntries().put(fileName, newRootNode);
					}
					addLocalNodes(path + "/", file, newRootNode);

				} else {
					if (rootNode.getEntries().containsKey(fileName)) continue;
					Timber.d("loading file " + fileName);
					rootNode.getEntries().put(fileName, new FileNode(rootNode, fileName, new DummyTreeEntry(path, TreeEntry.MODE_BLOB)));
				}
			}

		}

		private Observable<DirNode> removeDeletedNodes(final DirNode rootNode) {
			return gitManager.getDeletedFiles()
					.flatMap(new Func1<Set<String>, Observable<DirNode>>() {
						@Override
						public Observable<DirNode> call(Set<String> deletedFiles) {
							for (String file : deletedFiles) {
								Timber.d("deleting file " + file + " from tree");
								DirNode iter = rootNode;
								String[] paths = file.split("/");
								for (int i = 0; i < paths.length - 1; ++i) {
									iter = (DirNode) iter.getEntries().get(paths[i]);
								}
								iter.getEntries().remove(paths[paths.length - 1]);
							}
							return Observable.just(rootNode);
						}
					});
		}


	}


	/**
	 * Represents a local tree entry which has not yet been pushed to GitHub.
	 */
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


	/**
	 * Helper structure for passing information down a RxJava chain.
	 */
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
