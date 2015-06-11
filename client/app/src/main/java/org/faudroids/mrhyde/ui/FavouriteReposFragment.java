package org.faudroids.mrhyde.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.getbase.floatingactionbutton.AddFloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.ui.utils.AbstractReposFragment;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.Collection;

import roboguice.inject.InjectView;
import rx.functions.Action1;

public final class FavouriteReposFragment extends AbstractReposFragment {

	private static final int REQUEST_SELECT_REPOSITORY = 42;

	@InjectView(R.id.empty) TextView emptyView;
	@InjectView(R.id.add) AddFloatingActionButton addButton;


	public FavouriteReposFragment() {
		super(R.layout.fragment_repos_favourite);
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
		if (resultCode != Activity.RESULT_OK) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		switch (requestCode) {
			case REQUEST_SELECT_REPOSITORY:
				Repository repository = (Repository) data.getSerializableExtra(SelectRepoActivity.RESULT_REPOSITORY);
				repositoryManager.markRepositoryAsFavourite(repository);
				loadRepositories();
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	protected void loadRepositories() {
		showSpinner();
		compositeSubscription.add(repositoryManager.getFavouriteRepositories()
				.compose(new DefaultTransformer<Collection<Repository>>())
				.subscribe(new Action1<Collection<Repository>>() {
					@Override
					public void call(Collection<Repository> repositories) {
						hideSpinner();
						if (repositories.size() == 0) {
							emptyView.setVisibility(View.VISIBLE);
							recyclerView.setVisibility(View.GONE);
						} else {
							emptyView.setVisibility(View.GONE);
							recyclerView.setVisibility(View.VISIBLE);
							repoAdapter.setItems(repositories);
						}
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this.getActivity(), "failed to get favourite repos"))
						.add(new HideSpinnerAction(this))
						.build()));
	}

}
