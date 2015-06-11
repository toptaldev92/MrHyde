package org.faudroids.mrhyde.ui;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.ui.utils.AbstractReposFragment;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.Collection;

import rx.functions.Action1;

public class AllReposFragment extends AbstractReposFragment {

	@Override
	protected void loadRepositories() {
		showSpinner();
		compositeSubscription.add(repositoryManager.getAllRepositories()
				.compose(new DefaultTransformer<Collection<Repository>>())
				.subscribe(new Action1<Collection<Repository>>() {
					@Override
					public void call(Collection<Repository> repositories) {
						hideSpinner();
						repoAdapter.setItems(repositories);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this.getActivity(), "failed to get repos"))
						.add(new HideSpinnerAction(this))
						.build()));
	}

}
