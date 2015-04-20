package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.github.ApiWrapper;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import rx.functions.Action1;
import timber.log.Timber;

public final class ReposFragment extends AbstractListFragment {

	@Inject ApiWrapper apiWrapper;

	private RepositoryListAdapter listAdapter;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		listAdapter = new RepositoryListAdapter();
		setListAdapter(listAdapter);

		apiWrapper.getRepositories()
				.compose(new DefaultTransformer<List<Repository>>())
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


	private final class RepositoryListAdapter extends AbstractListAdapter<Repository> {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Repository repository = getItem(position);
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			((TextView) view.findViewById(android.R.id.text1)).setText(repository.getName());
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Fragment newFragment = DirFragment.createInstance(repository);
					uiUtils.replaceFragment(ReposFragment.this, newFragment);
				}
			});
			return view;
		}

	}


}
