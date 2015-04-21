package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

public final class ReposFragment extends AbstractListFragment {

	@InjectView(R.id.progressbar) ProgressBar progressBar;
	@InjectView(android.R.id.empty) TextView emptyView;

	@Inject RepositoryManager repositoryManager;
	private RepositoryListAdapter listAdapter;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
		return inflater.inflate(R.layout.fragment_repos, parent, false);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		listAdapter = new RepositoryListAdapter();
		setListAdapter(listAdapter);

		progressBar.setVisibility(View.VISIBLE);
		emptyView.setVisibility(View.GONE);
		compositeSubscription.add(repositoryManager.getRepositories()
						.compose(new DefaultTransformer<List<Repository>>())
						.subscribe(new Action1<List<Repository>>() {
							@Override
							public void call(List<Repository> repositories) {
								listAdapter.setItems(repositories);
								progressBar.setVisibility(View.GONE);
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
								progressBar.setVisibility(View.GONE);
								emptyView.setVisibility(View.VISIBLE);
								Timber.e(throwable, "failed to get repos");
							}
						}));
	}


	private final class RepositoryListAdapter extends AbstractListAdapter<Repository> {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Repository repository = getItem(position);
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			((TextView) view.findViewById(android.R.id.text1)).setText(repository.getOwner().getLogin() + "/" + repository.getName());
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
