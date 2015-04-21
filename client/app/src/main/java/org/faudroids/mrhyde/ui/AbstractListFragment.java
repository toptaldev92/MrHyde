package org.faudroids.mrhyde.ui;

import android.widget.BaseAdapter;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboListFragment;

abstract class AbstractListFragment extends RoboListFragment {

	@Inject UiUtils uiUtils;


	protected abstract class AbstractListAdapter<T> extends BaseAdapter {

		protected final List<T> items = new LinkedList<>();

		public void setItems(List<T> items) {
			this.items.clear();
			this.items.addAll(items);
			notifyDataSetChanged();
		}

		@Override
		public final int getCount() {
			return items.size();
		}

		@Override
		public final T getItem(int position) {
			return items.get(position);
		}

		@Override
		public final long getItemId(int position) {
			return position;
		}

		public List<T> getItems() {
			return items;
		}

	}

}
