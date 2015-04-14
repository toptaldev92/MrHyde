package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.provided.RoboListFragment;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public final class ReposFragment extends RoboListFragment {

	@Inject ApiWrapper apiWrapper;
	private RepositoryListAdapter listAdapter;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		listAdapter = new RepositoryListAdapter();
		setListAdapter(listAdapter);

		apiWrapper.getRepositories()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<List<Repository>>() {
					@Override
					public void call(List<Repository> repositories) {
						listAdapter.setItems(repositories);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get repos");
					}
				});
	}


	private final class RepositoryListAdapter extends BaseAdapter {

		private final List<Repository> repositoryList = new LinkedList<>();


		public void setItems(List<Repository> repositoryList) {
			this.repositoryList.clear();
			this.repositoryList.addAll(repositoryList);
			notifyDataSetChanged();
		}


		@Override
		public int getCount() {
			return repositoryList.size();
		}


		@Override
		public Repository getItem(int position) {
			return repositoryList.get(position);
		}


		@Override
		public long getItemId(int position) {
			return position;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).getName());
			return view;
		}
	}


}
