package org.faudroids.mrhyde.ui;

import android.content.Intent;
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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.ui.tree.AbstractNode;
import org.faudroids.mrhyde.ui.tree.DirNode;
import org.faudroids.mrhyde.ui.tree.FileNode;
import org.faudroids.mrhyde.utils.DefaultTransformer;

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
public final class DirActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";
	private static final String STATE_TREE = "STATE_TREE";


	@InjectView(R.id.list) RecyclerView recyclerView;
	private PathNodeAdapter pathNodeAdapter;
	private RecyclerView.LayoutManager layoutManager;

	@Inject RepositoryManager repositoryManager;
	private Repository repository;
	private FileManager fileManager;

	private Tree filesTree; // gotten from github, used to restore state


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// show action bar back button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// get arguments
		repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		FileManager fileManager = repositoryManager.getFileManager(repository);
		setTitle(repository.getName());

		// setup list
		layoutManager = new LinearLayoutManager(this);
		pathNodeAdapter = new PathNodeAdapter();
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(pathNodeAdapter);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));


		// get tree
		if (savedInstanceState != null) {
			filesTree = (Tree) savedInstanceState.getSerializable(STATE_TREE);
			pathNodeAdapter.setSelectedNode(parseGitHubTree(filesTree));
			pathNodeAdapter.onRestoreInstanceState(savedInstanceState);

		} else {
			compositeSubscription.add(fileManager.getTree()
					.compose(new DefaultTransformer<Tree>())
					.subscribe(new Action1<Tree>() {
						@Override
						public void call(Tree tree) {
							filesTree = tree;
							pathNodeAdapter.setSelectedNode(parseGitHubTree(tree));
						}
					}, new Action1<Throwable>() {
						@Override
						public void call(Throwable throwable) {
							Toast.makeText(DirActivity.this, "That didn't work, check log", Toast.LENGTH_LONG).show();
							Timber.e(throwable, "failed to get content");
						}
					}));
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(STATE_TREE, filesTree);
		pathNodeAdapter.onSaveInstanceState(outState);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.editor, menu);
		return true;
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
		switch(item.getItemId()) {
			case R.id.action_commit:
				Intent commitIntent = new Intent(this, CommitActivity.class);
				commitIntent.putExtra(CommitActivity.EXTRA_REPOSITORY, repository);
				startActivity(commitIntent);
				return true;

			case R.id.action_preview:
				fileManager = repositoryManager.getFileManager(repository);
				fileManager.getDiff().subscribe(new Action1<String>() {
					@Override
					public void call(String diff) {
						Intent previewIntent = new Intent(DirActivity.this, PreviewActivity.class);
						previewIntent.putExtra(PreviewActivity.EXTRA_REPO_CHECKOUT_URL, repository.getCloneUrl());
						previewIntent.putExtra(PreviewActivity.EXTRA_DIFF, diff);
						startActivity(previewIntent);
					}
				}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								Timber.e(throwable, "failed to load changes");
							}
				});
				return true;

			case android.R.id.home:
				onBackPressed();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	private DirNode parseGitHubTree(Tree gitTree) {
		final DirNode rootNode = new DirNode(null, "");
		for (TreeEntry gitEntry : gitTree.getTree()) {
			String[] paths = gitEntry.getPath().split("/");

			DirNode parentNode = rootNode;
			for (int i = 0; i < paths.length; ++i) {
				String path = paths[i];
				if (i == paths.length - 1) {
					// commit leaf
					if (gitEntry.getMode().equals(TreeEntry.MODE_DIRECTORY)) {
						parentNode.getEntries().put(path, new DirNode(parentNode, path));
					} else {
						parentNode.getEntries().put(path, new FileNode(parentNode, path, gitEntry));
					}

				} else {
					parentNode = (DirNode) parentNode.getEntries().get(path);
				}
			}
		}
		return rootNode;
	}


	public class PathNodeAdapter extends RecyclerView.Adapter<PathNodeAdapter.PathNodeViewHolder> {

		private static final String STATE_SELECTED_NODE = "STATE_SELECTED_NODE";

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
			// if no parent let activity handle back press
			if (selectedNode.getParent() == null) return false;
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


		public void onSaveInstanceState(Bundle outState) {
			AbstractNode iter = selectedNode;
			String selectedPath = iter.getPath();
			iter = iter.getParent();

			while (iter != null) {
				selectedPath = iter.getPath() + "/" + selectedPath;
				Timber.d(selectedPath);
				iter = iter.getParent();
			}

			outState.putString(STATE_SELECTED_NODE, selectedPath);
		}


		public void onRestoreInstanceState(Bundle inState) {
			String selectedPath = inState.getString(STATE_SELECTED_NODE);
			String[] paths = selectedPath.split("/");
			DirNode iter = selectedNode;
			for (int i = 1; i < paths.length; ++i) {
				iter = (DirNode) iter.getEntries().get(paths[i]);
			}
			setSelectedNode(iter);
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
				titleView.setText(pathNode.getPath());
				if (pathNode instanceof DirNode) {
					iconView.setImageResource(R.drawable.folder);
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
							FileNode fileNode = (FileNode) pathNode;
							Intent fileIntent = new Intent(DirActivity.this, FileActivity.class);
							fileIntent.putExtra(FileActivity.EXTRA_REPOSITORY, repository);
							fileIntent.putExtra(FileActivity.EXTRA_TREE_ENTRY, fileNode.getTreeEntry());
							startActivity(fileIntent);
						}
					}
				});
			}
		}
	}


}
