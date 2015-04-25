package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboListFragment;
import rx.subscriptions.CompositeSubscription;

abstract class AbstractListFragment extends RoboListFragment {

	private final int layoutResource;

	@Inject UiUtils uiUtils;
	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();
	protected ActivityListener activityListener;


	AbstractListFragment(int layoutResource) {
		this.layoutResource = layoutResource;
	}

		@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityListener = UiUtils.activityToActionBarListener(activity);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(layoutResource, container, false);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
	}


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
