package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.faudroids.mrhyde.R;

class JekyllActionModeListener<T> implements ActionMode.Callback {

	private final Activity activity;
	private final ActionSelectionListener<T> selectionListener;

	private T selectedItem = null;
	private ActionMode actionMode;
	private final int moveActionStringResource;


	public JekyllActionModeListener(Activity activity, ActionSelectionListener<T> selectionListener, int moveActionStringResource) {
		this.activity = activity;
		this.selectionListener = selectionListener;
		this.moveActionStringResource = moveActionStringResource;
	}


	public boolean startActionMode(T item ) {
		if (this.selectedItem != null) return false;
		this.actionMode = this.activity.startActionMode(this);
		this.selectedItem = item;
		return true;
	}


	public void stopActionMode() {
		if (actionMode != null) actionMode.finish();
	}


	public T getSelectedItem() {
		return selectedItem;
	}


	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.posts_drafts_action_mode, menu);
		return true;
	}


	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.findItem(R.id.action_move).setTitle(moveActionStringResource);
		return true;
	}


	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_edit:
				selectionListener.onEdit(selectedItem);
				stopActionMode();
				return true;

			case R.id.action_delete:
				selectionListener.onDelete(selectedItem);
				stopActionMode();
				return true;

			case R.id.action_move:
				selectionListener.onMove(selectedItem);
				stopActionMode();
				return true;
		}
		return false;
	}


	@Override
	public void onDestroyActionMode(ActionMode mode) {
		this.selectedItem = null;
		this.actionMode = null;
		selectionListener.onStopActionMode();
	}


	public interface ActionSelectionListener<T> {

		void onDelete(T item);
		void onEdit(T item);
		void onMove(T item);
		void onStopActionMode();

	}

}
