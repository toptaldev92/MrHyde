package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileManagerFactory;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.FileUtils;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.DividerItemDecoration;
import org.faudroids.mrhyde.ui.utils.ImageUtils;
import org.faudroids.mrhyde.ui.utils.UiUtils;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_dir)
public final class DirActivity extends AbstractActionBarActivity implements DirActionModeListener.ActionSelectionListener {

	private static final int
			REQUEST_COMMIT = 42,
			REQUEST_EDIT_FILE = 43,
			REQUEST_SELECT_PHOTO = 44;

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	private static final String STATE_SELECTED_NODE = "STATE_SELECTED_NODE";

	@InjectView(R.id.list) private RecyclerView recyclerView;
	private PathNodeAdapter pathNodeAdapter;
	private RecyclerView.LayoutManager layoutManager;

	@InjectView(R.id.tint) private View tintView;
	@InjectView(R.id.add) private FloatingActionsMenu addButton;
	@InjectView(R.id.add_file) private FloatingActionButton addFileButton;
	@InjectView(R.id.add_image) private FloatingActionButton addImageButton;
	@InjectView(R.id.add_folder) private FloatingActionButton addFolderButton;

	@Inject private RepositoryManager repositoryManager;
	@Inject private FileManagerFactory fileManagerFactory;
	private Repository repository;
	private FileManager fileManager;
	@Inject private NodeUtils nodeUtils;
	@Inject private FileUtils fileUtils;
	@Inject private ImageUtils imageUtils;
	@Inject private ActivityIntentFactory intentFactory;

	private DirActionModeListener actionModeListener = null;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = fileManagerFactory.createFileManager(repository);
		setTitle(repository.getName());

		// setup add buttons
		addButton.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
			@Override
			public void onMenuExpanded() {
				tintView.animate().alpha(1).setDuration(200).start();
			}

			@Override
			public void onMenuCollapsed() {
				tintView.animate().alpha(0).setDuration(200).start();
			}
		});
		addFileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				addAndOpenFile();
			}
		});
		addImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO);
			}
		});
		addFolderButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				addDirectory();
			}
		});
		tintView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
			}
		});

		// setup list
		layoutManager = new LinearLayoutManager(this);
		pathNodeAdapter = new PathNodeAdapter();
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(pathNodeAdapter);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

		// get tree
		updateTree(savedInstanceState);

		// prepare action mode
		actionModeListener = new DirActionModeListener(this, this, uiUtils);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		pathNodeAdapter.onSaveInstanceState(outState);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.files, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.action_mark_repository);
		if (repositoryManager.isRepositoryFavourite(repository))
			item.setTitle(getString(R.string.action_unmark_repository));
		else item.setTitle(getString(R.string.action_mark_repository));

		// hide menu during loading
		if (isSpinnerVisible()) {
			menu.findItem(R.id.action_commit).setVisible(false);
			menu.findItem(R.id.action_preview).setVisible(false);
			menu.findItem(R.id.action_discard_changes).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public void onBackPressed() {
		// only go back when at the top dir
		if (!pathNodeAdapter.onBackPressed()) {
			super.onBackPressed();
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_commit:
				startActivityForResult(intentFactory.createCommitIntent(repository), REQUEST_COMMIT);
				return true;

			case R.id.action_preview:
				startActivity(intentFactory.createPreviewIntent(repository));
				return true;

			case R.id.action_mark_repository:
				if (repositoryManager.isRepositoryFavourite(repository)) {
					repositoryManager.unmarkRepositoryAsFavourite(repository);
					Toast.makeText(this, getString(R.string.unmarked_toast), Toast.LENGTH_SHORT).show();
				} else {
					repositoryManager.markRepositoryAsFavourite(repository);
					Toast.makeText(this, getString(R.string.marked_toast), Toast.LENGTH_SHORT).show();
				}
				return true;

			case R.id.action_discard_changes:
				new AlertDialog.Builder(this)
						.setTitle(R.string.discard_changes_title)
						.setMessage(R.string.discard_changes_message)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								fileManager.resetRepository();
								updateTree(null);
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_COMMIT:
				if (resultCode != RESULT_OK) return;
			case REQUEST_EDIT_FILE:
				// refresh tree after successful commit or updated file (in case of new files)
				refreshTree();
				break;

			case REQUEST_SELECT_PHOTO:
				if (resultCode != RESULT_OK) return;
				final Uri selectedImage = data.getData();
				Timber.d(selectedImage.toString());

				// get image name
				uiUtils.createInputDialog(
						R.string.image_new_title,
						R.string.image_new_message,
						new UiUtils.OnInputListener() {
							@Override
							public void onInput(String imageName) {
								// store image
								FileNode imageNode = fileManager.createNewFile(pathNodeAdapter.getSelectedNode(), imageName);
								showSpinner();
								compositeSubscription.add(imageUtils.loadImage(imageNode, selectedImage)
										.compose(new DefaultTransformer<FileData>())
										.subscribe(new Action1<FileData>() {
											@Override
											public void call(FileData data) {
												hideSpinner();
												try {
													fileManager.writeFile(data);
													refreshTree();
												} catch (IOException ioe) {
													Timber.e(ioe, "failed to write file");
												}
											}
										}, new ErrorActionBuilder()
												.add(new DefaultErrorAction(DirActivity.this, "failed to write file"))
												.add(new HideSpinnerAction(DirActivity.this))
												.build()));
							}
						})
						.show();
				break;
		}
	}


	@Override
	public void onDelete(final FileNode fileNode) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete_title)
				.setMessage(getString(R.string.delete_message, fileNode.getPath()))
				.setPositiveButton(getString(R.string.action_delete), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showSpinner();
						compositeSubscription.add(fileManager.deleteFile(fileNode)
								.compose(new DefaultTransformer<Void>())
								.subscribe(new Action1<Void>() {
									@Override
									public void call(Void aVoid) {
										hideSpinner();
										updateTree(null);
									}
								}, new ErrorActionBuilder()
										.add(new DefaultErrorAction(DirActivity.this, "failed to delete file"))
										.add(new HideSpinnerAction(DirActivity.this))
										.build()));
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}


	@Override
	public void onEdit(FileNode fileNode) {
		startFileActivity(fileNode, false);
	}


	@Override
	public void onRename(FileNode fileNode, String newFileName) {
		showSpinner();
		compositeSubscription.add(fileManager.renameFile(fileNode, newFileName)
				.compose(new DefaultTransformer<FileNode>())
				.subscribe(new Action1<FileNode>() {
					@Override
					public void call(FileNode newFileNode) {
						hideSpinner();
						updateTree(null);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this, "failed to rename file"))
						.add(new HideSpinnerAction(this))
						.build()));
	}


	@Override
	public void onStopActionMode() {
		pathNodeAdapter.notifyDataSetChanged();
	}


	private void addAndOpenFile() {
		uiUtils.createInputDialog(
				R.string.file_new_title,
				R.string.file_new_message,
				new UiUtils.OnInputListener() {
					@Override
					public void onInput(String input) {
						FileNode fileNode = fileManager.createNewFile(pathNodeAdapter.getSelectedNode(), input);
						startFileActivity(fileNode, true);
					}
				})
				.show();
	}


	private void addDirectory() {
		uiUtils.createInputDialog(
				R.string.dir_new_title,
				R.string.dir_new_message,
				new UiUtils.OnInputListener() {
					@Override
					public void onInput(String input) {
						fileManager.createNewDir(pathNodeAdapter.getSelectedNode(), input);
						Bundle state = new Bundle();
						pathNodeAdapter.onSaveInstanceState(state);
						updateTree(state);
					}
				})
				.show();
	}


	/**
	 * Recreates the file tree without changing directory
	 */
	private void refreshTree() {
		Bundle tmpSavedState = new Bundle();
		pathNodeAdapter.onSaveInstanceState(tmpSavedState);
		updateTree(tmpSavedState);
	}


	/**
	 * Recreates the file tree
	 */
	private void updateTree(final Bundle savedInstanceState) {
		showSpinner();
		compositeSubscription.add(fileManager.getTree()
				.compose(new DefaultTransformer<DirNode>())
				.subscribe(new Action1<DirNode>() {
					@Override
					public void call(DirNode rootNode) {
						hideSpinner();

						// re-enable options menu
						invalidateOptionsMenu();

						// check for empty repository
						if (rootNode == null) {
							new AlertDialog.Builder(DirActivity.this)
									.setTitle(R.string.error_empty_repo_title)
									.setMessage(R.string.error_empty_repo_message)
									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											finish();
										}
									})
									.setOnCancelListener(new DialogInterface.OnCancelListener() {
										@Override
										public void onCancel(DialogInterface dialog) {
											finish();
										}
									})
									.show();
							return;
						}

						// update files list
						pathNodeAdapter.setSelectedNode(rootNode);
						if (savedInstanceState != null)
							pathNodeAdapter.onRestoreInstanceState(savedInstanceState);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this, "failed to get tree"))
						.add(new HideSpinnerAction(this))
						.build()));
	}


	private void startFileActivity(FileNode fileNode, boolean isNewFile) {
		actionModeListener.stopActionMode();

		// start text or image activity depending on file name
		if (!fileUtils.isImage(fileNode.getPath())) {
			// start text editor
			Intent editorIntent = intentFactory.createTextEditorIntent(repository, fileNode, isNewFile);
			startActivityForResult(editorIntent, REQUEST_EDIT_FILE);

		} else {
			// start image viewer
			Intent viewerIntent = intentFactory.createImageViewerIntent(repository, fileNode);
			startActivity(viewerIntent);
		}

	}


	public class PathNodeAdapter extends RecyclerView.Adapter<PathNodeAdapter.PathNodeViewHolder> {

		private final List<AbstractNode> nodeList = new ArrayList<>();
		private DirNode selectedNode;


		@Override
		public PathNodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
			return new PathNodeViewHolder(view);
		}


		@Override
		public void onBindViewHolder(PathNodeViewHolder holder, int position) {
			holder.setPathNode(nodeList.get(position));
		}


		@Override
		public int getItemCount() {
			return nodeList.size();
		}


		public boolean onBackPressed() {
			// if no parent (or not loaded) let activity handle back press
			if (selectedNode == null || selectedNode.getParent() == null) return false;
			// otherwise navigate up
			setSelectedNode((DirNode) selectedNode.getParent());
			return true;
		}


		public void setSelectedNode(DirNode newSelectedNode) {
			actionModeListener.stopActionMode();
			if (newSelectedNode.getPath().equals("")) setTitle(repository.getName());
			else setTitle(newSelectedNode.getPath());
			selectedNode = newSelectedNode;
			nodeList.clear();
			nodeList.addAll(sortEntries(newSelectedNode.getEntries().values()));
			notifyDataSetChanged();
		}


		public DirNode getSelectedNode() {
			return selectedNode;
		}


		public void onSaveInstanceState(Bundle outState) {
			nodeUtils.saveNode(STATE_SELECTED_NODE, outState, selectedNode);
		}


		public void onRestoreInstanceState(Bundle inState) {
			AbstractNode restoredSelectedNode = nodeUtils.restoreNode(STATE_SELECTED_NODE, inState, selectedNode);
			if (restoredSelectedNode == null) return;

			setSelectedNode((DirNode) restoredSelectedNode);
		}


		private List<AbstractNode> sortEntries(Collection<AbstractNode> entries) {
			List<AbstractNode> dirs = new ArrayList<>();
			List<AbstractNode> files = new ArrayList<>();
			for (AbstractNode node : entries) {
				if (node instanceof DirNode) dirs.add(node);
				else files.add(node);
			}
			Collections.sort(dirs);
			Collections.sort(files);
			dirs.addAll(files);
			return dirs;
		}


		public class PathNodeViewHolder extends RecyclerView.ViewHolder {

			private final View view;
			private final TextView titleView;
			private final ImageView iconView;

			public PathNodeViewHolder(View view) {
				super(view);
				this.view = view;
				this.titleView = (TextView) view.findViewById(R.id.title);
				this.iconView = (ImageView) view.findViewById(R.id.icon);
			}

			public void setPathNode(final AbstractNode pathNode) {
				// check for action mode
				if (actionModeListener.getSelectedNode() != null && actionModeListener.getSelectedNode().equals(pathNode)) {
					view.setSelected(true);
				} else {
					view.setSelected(false);
				}

				// setup node content
				titleView.setText(pathNode.getPath());
				if (pathNode instanceof DirNode) {
					iconView.setImageResource(R.drawable.folder);
				} else if (fileUtils.isImage(pathNode.getPath())) {
					iconView.setImageResource(R.drawable.image);
				} else {
					iconView.setImageResource(R.drawable.file);
				}

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (pathNode instanceof DirNode) {
							// navigate down
							setSelectedNode((DirNode) pathNode);
						} else {
							// open file
							startFileActivity((FileNode) pathNode, false);
						}
					}
				});

				if (pathNode instanceof FileNode) {
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							// only highlight item when selection was successful
							if (actionModeListener.startActionMode((FileNode) pathNode)) {
								view.setSelected(true);
							}
							return true;
						}
					});
				} else {
					view.setLongClickable(false);
				}
			}
		}
	}

}
