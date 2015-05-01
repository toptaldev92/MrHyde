package org.faudroids.mrhyde.ui;

import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.List;

import rx.functions.Action1;
import timber.log.Timber;

public final class ReposFragment extends AbstractReposFragment {

	@Override
	protected void loadRepositories(final RepositoryAdapter repoAdapter) {
		showSpinner();
		compositeSubscription.add(repositoryManager.getRepositories()
				.compose(new DefaultTransformer<List<Repository>>())
				.subscribe(new Action1<List<Repository>>() {
					@Override
					public void call(List<Repository> repositories) {
						hideSpinner();
						repoAdapter.setItems(repositories);
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
