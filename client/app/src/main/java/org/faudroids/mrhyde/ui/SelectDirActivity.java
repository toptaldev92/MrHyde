package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileManagerFactory;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.FileUtils;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.DividerItemDecoration;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;

@ContentView(R.layout.dialog_select_dir)
public final class SelectDirActivity extends AbstractActionBarActivity {

	static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY", 		// argument for this activity
			EXTRA_SELECTED_DIR = "EXTRA_SELECTED_DIR";	// part of result of this activity

	private final String
			STATE_SELECTED_NODE = "STATE_SELECTED_NODE";

	@InjectView(R.id.list) private RecyclerView recyclerView;
	private PathNodeAdapter pathNodeAdapter;

	@Inject private RepositoryManager repositoryManager;
	@Inject private FileManagerFactory fileManagerFactory;
	private Repository repository;
	private FileManager fileManager;
	@Inject private NodeUtils nodeUtils;
	@Inject private FileUtils fileUtils;

	@InjectView(R.id.back) private View backView;
	@InjectView(R.id.cancel) private View cancelView;
	@InjectView(R.id.confirm) private View confirmView;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make dialog fill screen
		getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		// get arguments
		repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = fileManagerFactory.createFileManager(repository);

		// hide action bar
		getSupportActionBar().hide();

		// setup list
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		pathNodeAdapter = new PathNodeAdapter();
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(pathNodeAdapter);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

		// get tree
		updateTree(savedInstanceState);

		// setup buttons
		backView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		cancelView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		confirmView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// return result
				Intent resultIntent = new Intent();
				nodeUtils.saveNode(EXTRA_SELECTED_DIR, resultIntent, pathNodeAdapter.getSelectedNode());
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		pathNodeAdapter.onSaveInstanceState(outState);
	}


	@Override
	public void onBackPressed() {
		// only go back when at the top dir
		if (!pathNodeAdapter.onBackPressed()) {
			super.onBackPressed();
		}
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
							new AlertDialog.Builder(SelectDirActivity.this)
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


	private void onDirSelected(DirNode node) {
		// navigate "down"
		pathNodeAdapter.setSelectedNode(node);
	}


	private void onFileSelected(FileNode node) {
		// nothing to do
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
				// setup node content
				titleView.setText(pathNode.getPath());

				int imageResource = R.drawable.folder;
				if ((pathNode instanceof FileNode) && fileUtils.isImage(pathNode.getPath())) imageResource = R.drawable.image;
				else if (pathNode instanceof FileNode) imageResource = R.drawable.file;
				float alpha = (pathNode instanceof DirNode) ? 1f : 0.3f;

				iconView.setImageResource(imageResource);
				iconView.setAlpha(alpha);
				titleView.setAlpha(alpha);

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (pathNode instanceof DirNode) {
							onDirSelected((DirNode) pathNode);
						} else {
							onFileSelected((FileNode) pathNode);
						}
					}
				});
			}
		}
	}

}
