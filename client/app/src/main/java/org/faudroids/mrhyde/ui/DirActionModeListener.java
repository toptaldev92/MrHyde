package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileNode;

class DirActionModeListener implements ActionMode.Callback {

	private final Activity activity;
	private final ActionSelectionListener selectionListener;

	private View selectedView = null;
	private FileNode selectedNode = null;
	private ActionMode actionMode;


	public DirActionModeListener(Activity activity, ActionSelectionListener selectionListener) {
		this.activity = activity;
		this.selectionListener = selectionListener;
	}


	public boolean startActionMode(View selectedView, FileNode selectedNode) {
		if (this.selectedNode != null) return false;
		this.actionMode = this.activity.startActionMode(this);
		this.selectedView = selectedView;
		this.selectedView.setSelected(true);
		this.selectedNode = selectedNode;
		return true;
	}



	public void stopActionMode() {
		this.actionMode.finish();
	}


	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.files, menu);
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
		}
		return false;
	}


	@Override
	public void onDestroyActionMode(ActionMode mode) {
		this.selectedView.setSelected(false);
		this.selectedView = null;
		this.selectedNode = null;
		this.actionMode = null;
	}


	public interface ActionSelectionListener {

		void onDelete(FileNode fileNode);
		void onEdit(FileNode fileNode);

	}

}
