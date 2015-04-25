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
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

public final class DirFragment extends AbstractFragment {

	private static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	public static DirFragment createInstance(Repository repository) {
		DirFragment fragment = new DirFragment();
		Bundle extras = new Bundle();
		extras.putSerializable(EXTRA_REPOSITORY, repository);
		fragment.setArguments(extras);
		return fragment;
	}

	@InjectView(R.id.list) RecyclerView recyclerView;
	private PathNodeAdapter pathNodeAdapter;
	private RecyclerView.LayoutManager layoutManager;

	@Inject RepositoryManager repositoryManager;
	private Repository repository;
	private FileManager fileManager;

	public DirFragment() {
		super(R.layout.fragment_dir);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// get arguments
		repository = (Repository) getArguments().getSerializable(EXTRA_REPOSITORY);
		FileManager fileManager = repositoryManager.getFileManager(repository);
		activityListener.setTitle(repository.getName());

		// setup list
		layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);
		pathNodeAdapter = new PathNodeAdapter();
		recyclerView.setAdapter(pathNodeAdapter);

		compositeSubscription.add(fileManager.getTree()
				.compose(new DefaultTransformer<Tree>())
				.subscribe(new Action1<Tree>() {
					@Override
					public void call(Tree tree) {
						pathNodeAdapter.setSelectedNode(parseGitHubTree(tree));

					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				}));
	}


	@Override
	public void onResume() {
		super.onResume();
		activityListener.setOnBackPressedListener(pathNodeAdapter);
	}


	@Override
	public void onPause() {
		activityListener.removeOnBackPressedListener();
		super.onPause();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.editor, menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_commit:
				Fragment commitFragment = CommitFragment.createInstance(repository);
				uiUtils.replaceFragment(this, commitFragment);
				return true;
			case R.id.action_preview:
				fileManager = repositoryManager.getFileManager(repository);
				fileManager.getDiff().subscribe(new Action1<String>() {
					@Override
					public void call(String diff) {
						Fragment previewFragment = PreviewFragment.createInstance(repository.getCloneUrl(), diff);
						uiUtils.replaceFragment(DirFragment.this, previewFragment);
					}
				}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								Timber.e(throwable, "failed to load changes");
							}
				});
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	private DirNode parseGitHubTree(Tree gitTree) {
		final DirNode rootNode = new DirNode(null, repository.getName());
		for (TreeEntry gitEntry : gitTree.getTree()) {
			String[] paths = gitEntry.getPath().split("/");

			DirNode parentNode = rootNode;
			for (int i = 0; i < paths.length; ++i) {
				String path = paths[i];
				if (i == paths.length - 1) {
					// commit leaf
					if (gitEntry.getMode().equals(TreeEntry.MODE_DIRECTORY)) {
						parentNode.entries.put(path, new DirNode(parentNode, path));
					} else {
						parentNode.entries.put(path, new FileNode(parentNode, path, gitEntry));
					}

				} else {
					parentNode = (DirNode) parentNode.entries.get(path);
				}
			}
		}
		return rootNode;
	}


	public class PathNodeAdapter extends RecyclerView.Adapter<PathNodeAdapter.PathNodeViewHolder>
			implements OnBackPressedListener {

		private final List<PathNode> nodeList = new ArrayList<>();
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


		@Override
		public boolean onBackPressed() {
			// if no parent let activity handle back press
			if (selectedNode.parent == null) return false;
			// otherwise navigate up
			setSelectedNode((DirNode) selectedNode.parent);
			return true;
		}


		public void setSelectedNode(DirNode newSelectedNode) {
			activityListener.setTitle(newSelectedNode.path);
			selectedNode = newSelectedNode;
			nodeList.clear();
			nodeList.addAll(newSelectedNode.entries.values());
			notifyDataSetChanged();
		}


		public class PathNodeViewHolder extends RecyclerView.ViewHolder {

			private final View view;
			private final TextView titleView;

			public PathNodeViewHolder(View view) {
				super(view);
				this.view = view;
				this.titleView = (TextView) view.findViewById(R.id.title);
			}

			public void setPathNode(final PathNode pathNode) {
				titleView.setText(pathNode.path);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (pathNode instanceof DirNode) {
							// navigate down
							setSelectedNode((DirNode) pathNode);
						} else {
							// open file
							FileNode fileNode = (FileNode) pathNode;
							FileFragment newFragment = FileFragment.createInstance(repository, fileNode.treeEntry);
							uiUtils.replaceFragment(DirFragment.this, newFragment);
						}
					}
				});
			}
		}
	}


	private static abstract class PathNode {

		final PathNode parent;
		final String path;

		public PathNode(PathNode parent, String path) {
			this.parent = parent;
			this.path = path;
		}

	}


	private static final class DirNode extends PathNode {

		final Map<String, PathNode> entries = new HashMap<>();

		public DirNode(PathNode parent, String path) {
			super(parent, path);
		}
	}


	private static final class FileNode extends PathNode {

		final TreeEntry treeEntry;

		public FileNode(PathNode parent, String path, TreeEntry treeEntry) {
			super(parent, path);
			this.treeEntry = treeEntry;
		}

	}

}
