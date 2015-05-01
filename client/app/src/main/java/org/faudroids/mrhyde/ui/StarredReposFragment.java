package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.AddFloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.Collection;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

public final class StarredReposFragment extends AbstractReposFragment {

	private static final int REQUEST_SELECT_REPOSITORY = 42;

	@InjectView(R.id.empty) TextView emptyView;
	@InjectView(R.id.add) AddFloatingActionButton addButton;


	public StarredReposFragment() {
		super(R.layout.fragment_repos_starred);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getActivity(), SelectRepoActivity.class), REQUEST_SELECT_REPOSITORY);
			}
		});
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) return;
		switch (requestCode) {
			case REQUEST_SELECT_REPOSITORY:
				Repository repository = (Repository) data.getSerializableExtra(SelectRepoActivity.RESULT_REPOSITORY);
				repositoryManager.starRepository(repository);
				loadRepositories();
				break;
		}
	}


	@Override
	protected void loadRepositories() {
		showSpinner();
		compositeSubscription.add(repositoryManager.getStarredRepositories()
				.compose(new DefaultTransformer<Collection<Repository>>())
				.subscribe(new Action1<Collection<Repository>>() {
					@Override
					public void call(Collection<Repository> repositories) {
						hideSpinner();
						if (repositories.size() == 0) emptyView.setVisibility(View.VISIBLE);
						else {
							emptyView.setVisibility(View.GONE);
							repoAdapter.setItems(repositories);
						}
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						hideSpinner();
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get repos");
					}
				}));
	}

}
