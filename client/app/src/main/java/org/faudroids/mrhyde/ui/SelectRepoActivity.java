package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.ArrayList;
import java.util.Collection;

import roboguice.inject.ContentView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;

@ContentView(R.layout.activity_select_repo)
public class SelectRepoActivity extends AbstractActionBarActivity {

    static final String RESULT_REPOSITORY = "RESULT_REPOSITORY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_select_repository));
    }
    

    protected void returnRepository(Repository repository) {
        Intent data = new Intent();
        data.putExtra(RESULT_REPOSITORY, repository);
        setResult(RESULT_OK, data);
        finish();
    }


    public static final class SelectRepoFragment extends AllReposFragment {

        @Override
        protected void loadRepositories() {
            showSpinner();
            compositeSubscription.add(
                    Observable.zip(
                            repositoryManager.getAllRepositories(),
                            repositoryManager.getFavouriteRepositories(),
                            new Func2<Collection<Repository>, Collection<Repository>, Collection<Repository>>() {
                                @Override
                                public Collection<Repository> call(Collection<Repository> allRepos, Collection<Repository> favouriteRepos) {
                                    // show only not favourite repositories
                                    Collection<Repository> filteredRepos = new ArrayList<>();
                                    for (Repository repo : allRepos) {
                                        boolean found = false;
                                        for (Repository favouriteRepo : favouriteRepos) {
                                            if (repo.getId() == favouriteRepo.getId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) filteredRepos.add(repo);
                                    }
                                    return filteredRepos;
                                }
                            })
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

        @Override
        protected void onRepositorySelected(Repository repository) {
            ((SelectRepoActivity) getActivity()).returnRepository(repository);
        }

    }

}
