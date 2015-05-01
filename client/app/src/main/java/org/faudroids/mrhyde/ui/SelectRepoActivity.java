package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;

import roboguice.inject.ContentView;

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
        protected void onRepositorySelected(Repository repository) {
            ((SelectRepoActivity) getActivity()).returnRepository(repository);
        }

    }

}
