package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.PreviewManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_preview)
public class PreviewActivity extends AbstractActionBarActivity {

    private static final int REQUEST_BROWSER = 44;

    static final String
            EXTRA_REPO = "EXTRA_REPO",
            EXTRA_DIFF = "EXTRA_DIFF";

    @Inject PreviewManager previewManager;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load arguments
        Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPO);
        String diff = getIntent().getStringExtra(EXTRA_DIFF);

		showSpinner();
		compositeSubscription.add(previewManager
				.loadPreview(repository, diff)
				.delay(0, TimeUnit.SECONDS)
				.compose(new DefaultTransformer<String>())
				.subscribe(new Action1<String>() {
					@Override
					public void call(String previewUrl) {
						hideSpinner();
						Timber.d("getting url " + previewUrl);
						startBrowser(previewUrl);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						hideSpinner();
						Timber.e(throwable, "failed to get results from server");
					}
				}));
    }


    @Override
    public void onActivityResult(int requestCode, int result, Intent data) {
        finish();
    }


    private void startBrowser(String previewUrl) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl));
        startActivityForResult(browserIntent, REQUEST_BROWSER);
    }

}
