package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.HashMap;
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



	@InjectView(R.id.container) LinearLayout containerView;
	@Inject RepositoryManager repositoryManager;
	private Repository repository;

	public DirFragment() {
		super(R.layout.fragment_dir);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		repository = (Repository) getArguments().getSerializable(EXTRA_REPOSITORY);
		FileManager fileManager = repositoryManager.getFileManager(repository);

		fileManager.getTree()
				.compose(new DefaultTransformer<Tree>())
				.subscribe(new Action1<Tree>() {
					@Override
					public void call(Tree tree) {
						DirNode rootDir = parseGitHubTree(tree);
						TreeNode rootView = parseViewTree(rootDir);
						AndroidTreeView treeView = new AndroidTreeView(getActivity(), rootView);
						treeView.setDefaultViewHolder(PathViewHolder.class);
						treeView.setDefaultAnimation(true);
						treeView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
						treeView.setDefaultNodeClickListener(new FileClickListener());
						containerView.addView(treeView.getView());
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				});
	}


	private DirNode parseGitHubTree(Tree gitTree) {
		// parse tree
		final DirNode rootNode = new DirNode("");
		for (TreeEntry gitEntry : gitTree.getTree()) {
			String[] paths = gitEntry.getPath().split("/");

			DirNode parentNode = rootNode;
			for (int i = 0; i < paths.length; ++i) {
				String path = paths[i];
				if (i == paths.length - 1) {
					// commit leaf
					if (gitEntry.getMode().equals(TreeEntry.MODE_DIRECTORY)) {
						parentNode.entries.put(path, new DirNode(path));
					} else {
						parentNode.entries.put(path, new FileNode(path, gitEntry));
					}

				} else {
					parentNode = (DirNode) parentNode.entries.get(path);
				}
			}
		}
		return rootNode;
	}


	private TreeNode parseViewTree(DirNode dirNode) {
		TreeNode rootView = TreeNode.root();
		parseViewTree(rootView, dirNode);
		return rootView;
	}


	private void parseViewTree(TreeNode parentView, DirNode dirNode) {
		for (PathNode node : dirNode.entries.values()) {
			if (node instanceof DirNode) {
				TreeNode newParentView = new TreeNode(node);
				parentView.addChild(newParentView);
				parseViewTree(newParentView, (DirNode) node);

			} else {
				parentView.addChild(new TreeNode(node));
			}
		}
	}


	private class FileClickListener implements TreeNode.TreeNodeClickListener {
		@Override
		public void onClick(TreeNode treeNode, Object object) {
			if (object instanceof FileNode) {
				FileNode fileNode = (FileNode) object;
				FileFragment newFragment = FileFragment.createInstance(repository, fileNode.treeEntry);
				uiUtils.replaceFragment(DirFragment.this, newFragment);
			}
		}
	}


	private static abstract class PathNode {

		private final String path;

		public PathNode(String path) {
			this.path = path;
		}
	}


	private static final class DirNode extends PathNode {

		private final Map<String, PathNode> entries = new HashMap<>();

		public DirNode(String path) {
			super(path);
		}
	}


	private static final class FileNode extends PathNode {

		private final TreeEntry treeEntry;

		public FileNode(String path, TreeEntry treeEntry) {
			super(path);
			this.treeEntry = treeEntry;
		}

	}


	public static final class PathViewHolder extends TreeNode.BaseNodeViewHolder<PathNode> {

		public PathViewHolder(Context context) {
			super(context);
		}

		@Override
		public View createNodeView(TreeNode treeNode, PathNode pathNode) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final View view = inflater.inflate(R.layout.item_file, null, false);
			TextView textView = (TextView) view.findViewById(R.id.name);
			textView.setText(pathNode.path);
			return view;
		}
	}

}
