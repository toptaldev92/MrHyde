package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
		recyclerView.setLayoutManager(layoutManager);
		pathNodeAdapter = new PathNodeAdapter();
		recyclerView.setAdapter(pathNodeAdapter);

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
				Fragment commitFragment = CommitFragment.createInstance(repository);
				// TODO
				// uiUtils.replaceFragment(this, commitFragment);
				return true;
			case R.id.action_preview:
				fileManager = repositoryManager.getFileManager(repository);
				fileManager.getDiff().subscribe(new Action1<String>() {
					@Override
					public void call(String diff) {
						Fragment previewFragment = PreviewFragment.createInstance(repository.getCloneUrl(), diff);
						// uiUtils.replaceFragment(DirActivity.this, previewFragment);
						// TODO
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
			setTitle(newSelectedNode.getPath());
			selectedNode = newSelectedNode;
			nodeList.clear();
			nodeList.addAll(newSelectedNode.getEntries().values());
			notifyDataSetChanged();
		}


		public void onSaveInstanceState(Bundle outState) {
			String selectedPath = "";

			AbstractNode iter = selectedNode;
			do {
				selectedPath = "/" + iter.getPath() + selectedPath;
				Timber.d(selectedPath);
				iter = iter.getParent();
			} while (iter.getParent() != null);

			Timber.d("saving path " + selectedPath);
			outState.putString(STATE_SELECTED_NODE, selectedPath);
		}


		public void onRestoreInstanceState(Bundle inState) {
			String selectedPath = inState.getString(STATE_SELECTED_NODE);
			String[] paths = selectedPath.split("/");
			DirNode iter = selectedNode;
			for (int i = 1; i < paths.length; ++i) {
				Timber.d("found node " + iter.getPath());
				iter = (DirNode) iter.getEntries().get(paths[i]);
			}
			setSelectedNode(iter);
		}


		public class PathNodeViewHolder extends RecyclerView.ViewHolder {

			private final View view;
			private final TextView titleView;

			public PathNodeViewHolder(View view) {
				super(view);
				this.view = view;
				this.titleView = (TextView) view.findViewById(R.id.title);
			}

			public void setPathNode(final AbstractNode pathNode) {
				titleView.setText(pathNode.getPath());
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (pathNode instanceof DirNode) {
							// navigate down
							setSelectedNode((DirNode) pathNode);
						} else {
							// open file
							FileNode fileNode = (FileNode) pathNode;
							FileFragment newFragment = FileFragment.createInstance(repository, fileNode.getTreeEntry());
							// uiUtils.replaceFragment(DirActivity.this, newFragment);
							// TODO
						}
					}
				});
			}
		}
	}


}
