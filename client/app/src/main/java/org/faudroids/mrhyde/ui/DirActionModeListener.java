package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.ui.utils.UiUtils;

class DirActionModeListener implements ActionMode.Callback {

	private final Activity activity;
	private final ActionSelectionListener selectionListener;
	private final UiUtils uiUtils;

	private FileNode selectedNode = null;
	private ActionMode actionMode;


	public DirActionModeListener(Activity activity, ActionSelectionListener selectionListener, UiUtils uiUtils) {
		this.activity = activity;
		this.selectionListener = selectionListener;
		this.uiUtils = uiUtils;
	}


	public boolean startActionMode(FileNode selectedNode) {
		if (this.selectedNode != null) return false;
		this.actionMode = this.activity.startActionMode(this);
		this.selectedNode = selectedNode;
		return true;
	}


	public void stopActionMode() {
		if (actionMode != null) actionMode.finish();
	}


	public FileNode getSelectedNode() {
		return selectedNode;
	}


	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.files_action_mode, menu);
		return true;
	}


	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}


	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_edit:
				selectionListener.onEdit(selectedNode);
				stopActionMode();
				return true;

			case R.id.action_delete:
				selectionListener.onDelete(selectedNode);
				stopActionMode();
				return true;

			case R.id.action_rename:
				uiUtils.createInputDialog(activity.getString(R.string.rename_title), selectedNode.getPath(), new UiUtils.OnInputListener() {
					@Override
					public void onInput(String newFileName) {
						selectionListener.onRename(selectedNode, newFileName);
						stopActionMode();
					}
				}).show();
				return true;

			case R.id.action_move:
				selectionListener.onMoveTo(selectedNode);
				stopActionMode();
				return true;
		}
		return false;
	}


	@Override
	public void onDestroyActionMode(ActionMode mode) {
		this.selectedNode = null;
		this.actionMode = null;
		selectionListener.onStopActionMode();
	}


	public interface ActionSelectionListener {

		void onDelete(FileNode fileNode);
		void onEdit(FileNode fileNode);
		void onRename(FileNode fileNode, String newFileName);
		void onMoveTo(FileNode fileNode);
		void onStopActionMode();

	}

}
