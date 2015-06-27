package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.ImageUtils;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;
import org.faudroids.mrhyde.ui.utils.UiUtils;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.io.IOException;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_dir)
public final class DirActivity extends AbstractDirActivity implements DirActionModeListener.ActionSelectionListener {

	private static final String EXTRA_NODE_TO_MOVE = "EXTRA_NODE_TO_MOVE"; // marks which file should be moved

	private static final int
			REQUEST_COMMIT = 42,
			REQUEST_EDIT_FILE = 43,
			REQUEST_SELECT_PHOTO = 44,
			REQUEST_SELECT_DIR = 45;

	@InjectView(R.id.tint) private View tintView;
	@InjectView(R.id.add) private FloatingActionsMenu addButton;
	@InjectView(R.id.add_file) private FloatingActionButton addFileButton;
	@InjectView(R.id.add_image) private FloatingActionButton addImageButton;
	@InjectView(R.id.add_folder) private FloatingActionButton addFolderButton;
	@InjectView(R.id.add_post) private FloatingActionButton addPostButton;
	@InjectView(R.id.add_draft) private FloatingActionButton addDraftButton;

	@Inject private ActivityIntentFactory intentFactory;
	@Inject private ImageUtils imageUtils;

	@Inject private JekyllManagerFactory jekyllManagerFactory;
	private JekyllManager jekyllManager;
	@Inject private JekyllUiUtils jekyllUiUtils;

	private DirActionModeListener actionModeListener = null;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		jekyllManager = jekyllManagerFactory.createJekyllManager(repository);
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
		addPostButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				jekyllUiUtils.showNewPostDialog(jekyllManager, repository, new JekyllUiUtils.OnContentCreatedListener<Post>() {
					@Override
					public void onContentCreated(Post content) {
						refreshTree();
					}
				});
			}
		});
		addDraftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
				jekyllUiUtils.showNewDraftDialog(jekyllManager, repository, new JekyllUiUtils.OnContentCreatedListener<Draft>() {
					@Override
					public void onContentCreated(Draft content) {
						refreshTree();
					}
				});
			}
		});
		tintView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addButton.collapse();
			}
		});

		// prepare action mode
		actionModeListener = new DirActionModeListener(this, this, uiUtils);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.files, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isSpinnerVisible()) {
			menu.findItem(R.id.action_commit).setVisible(false);
			menu.findItem(R.id.action_preview).setVisible(false);
			menu.findItem(R.id.action_discard_changes).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
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

			case REQUEST_SELECT_DIR:
				if (resultCode != RESULT_OK) return;

				// get root note
				DirNode rootNode = pathNodeAdapter.getSelectedNode();
				while (rootNode.getParent() != null) rootNode = rootNode.getParent();

				// get selected dir and file
				final DirNode selectedDir = (DirNode) nodeUtils.restoreNode(SelectDirActivity.EXTRA_SELECTED_DIR, data, rootNode);
				final FileNode selectedFile = (FileNode) nodeUtils.restoreNode(EXTRA_NODE_TO_MOVE, data, rootNode);

				// confirm overwriting files
				if (selectedDir.getEntries().containsKey(selectedFile.getPath())) {
					new AlertDialog.Builder(this)
							.setTitle(getString(R.string.overwrite_file_title))
							.setMessage(getString(R.string.overwrite_file_message, selectedFile.getPath()))
							.setPositiveButton(getString(R.string.overwrite_confirm), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									moveFile(selectedFile, selectedDir);
								}
							})
							.setNegativeButton(android.R.string.cancel, null)
							.show();
				} else {
					moveFile(selectedFile, selectedDir);
				}

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
	public void onMoveTo(FileNode fileNode) {
		Intent intent = new Intent(this, SelectDirActivity.class);
		intent.putExtra(SelectDirActivity.EXTRA_REPOSITORY, repository);
		nodeUtils.saveNode(EXTRA_NODE_TO_MOVE, intent, fileNode);
		for (String key : intent.getExtras().keySet()) Timber.d("found key " + key);
		startActivityForResult(intent, REQUEST_SELECT_DIR);
	}


	private void moveFile(FileNode fileNodeToMove, DirNode targetDirNode) {
		showSpinner();
		compositeSubscription.add(fileManager.moveFile(fileNodeToMove, targetDirNode)
				.compose(new DefaultTransformer<FileNode>())
				.subscribe(new Action1<FileNode>() {
					@Override
					public void call(FileNode newFileNode) {
						hideSpinner();
						Toast.makeText(DirActivity.this, getString(R.string.file_moved), Toast.LENGTH_SHORT).show();
						refreshTree();
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(DirActivity.this, "failed to move file"))
						.add(new HideSpinnerAction(DirActivity.this))
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


	@Override
	protected PathNodeAdapter createAdapter() {
		return new LongClickPathNodeAdapter();
	}


	@Override
	protected void onDirSelected(DirNode node) {
		// show / hide posts add button
		if (jekyllManager.isPostsDir(node)) addPostButton.setVisibility(View.VISIBLE);
		else addPostButton.setVisibility(View.GONE);

		// show / hide drafts add button
		if (jekyllManager.isDraftsDir(node)) addDraftButton.setVisibility(View.VISIBLE);
		else addDraftButton.setVisibility(View.GONE);
	}


	@Override
	protected void onFileSelected(FileNode node) {
		startFileActivity(node, false);
	}


	@Override
	public void onBackPressed() {
		// hide open add buttons
		if (addButton.isExpanded()) addButton.collapse();
		else super.onBackPressed();
	}


	public class LongClickPathNodeAdapter extends PathNodeAdapter {

		@Override
		public LongClickPathNodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
			return new LongClickPathNodeViewHolder(view);
		}

		public class LongClickPathNodeViewHolder extends PathNodeViewHolder {

			public LongClickPathNodeViewHolder(View view) {
				super(view);
			}

			@Override
			public void setViewForNode(final AbstractNode pathNode) {
				super.setViewForNode(pathNode);

				// check for action mode
				if (actionModeListener.getSelectedNode() != null && actionModeListener.getSelectedNode().equals(pathNode)) {
					view.setSelected(true);
				} else {
					view.setSelected(false);
				}

				// setup long click
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
