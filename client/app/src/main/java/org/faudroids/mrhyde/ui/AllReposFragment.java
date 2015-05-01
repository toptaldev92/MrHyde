package org.faudroids.mrhyde.ui;

import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.Collection;

import rx.functions.Action1;
import timber.log.Timber;

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
						   }, new Action1<Throwable>() {
							   @Override
							   public void call(Throwable throwable) {
								   hideSpinner();
								   Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
								   Timber.e(throwable, "failed to get repos");
							   }
						   }
				));
	}

}
