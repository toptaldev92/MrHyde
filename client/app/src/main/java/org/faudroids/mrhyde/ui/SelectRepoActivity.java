package org.faudroids.mrhyde.ui;

import android.content.Intent;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;

import roboguice.inject.ContentView;

@ContentView(R.layout.activity_select_repo)
public class SelectRepoActivity extends AbstractActionBarActivity {

    static final String RESULT_REPOSITORY = "RESULT_REPOSITORY";

    protected void returnRepository(Repository repository) {
        Intent data = new Intent();
        data.putExtra(RESULT_REPOSITORY, repository);
        setResult(RESULT_OK, data);
        finish();
    }


    public static final class SelectRepoFragment extends AllReposFragment {

        @Override
        protected void onRepositorySelected(Repository repository) {
            ((SelectRepoActivity) getActivity()).returnRepository(repository);
        }

    }

}
