package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.PreviewManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_preview)
public class PreviewActivity extends AbstractActionBarActivity {

    private static final String STATE_URL = "STATE_URL";

    static final String
            EXTRA_REPO = "EXTRA_REPO",
            EXTRA_DIFF = "EXTRA_DIFF";

    @Inject PreviewManager previewManager;
    @InjectView(R.id.web_view) WebView webView;

    private String previewUrl;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_preview));

        // load arguments
        Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPO);
        String diff = getIntent().getStringExtra(EXTRA_DIFF);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Timber.d(message.message());
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                view.loadUrl(url);
                return false;
            }
        });

        // load preview
        if (savedInstanceState != null) {
            previewUrl = savedInstanceState.getString(STATE_URL);
            webView.restoreState(savedInstanceState);

        } else {
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
                            webView.loadUrl(previewUrl);
                            // startBrowser(previewUrl);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            hideSpinner();
                            Timber.e(throwable, "failed to get results from server");
                        }
                    }));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preview, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_close:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_URL, previewUrl);
        webView.saveState(outState);
    }


    @Override
    public void onBackPressed() {
        WebBackForwardList backForwardList = webView.copyBackForwardList();
        if (backForwardList.getCurrentIndex() > 1) webView.goBack();
        else super.onBackPressed();
    }

}
