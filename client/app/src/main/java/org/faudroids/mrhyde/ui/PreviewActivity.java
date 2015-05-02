package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
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

    @InjectView(R.id.web_view) WebView webView;
    @Inject PreviewManager previewManager;

    private String previewUrl;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enable javascript + ignore SSL errors
        webView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        // load arguments
        Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPO);
        String diff = getIntent().getStringExtra(EXTRA_DIFF);

        // load preview
        if (savedInstanceState != null) {
            this.previewUrl = savedInstanceState.getString(STATE_URL, null);
            Timber.d("restoring url " + previewUrl);
            webView.loadUrl(previewUrl);
        } else {
            showSpinner();
            compositeSubscription.add(previewManager
                    .loadPreview(repository, diff)
                    .delay(1, TimeUnit.SECONDS)
                    .compose(new DefaultTransformer<String>())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String previewUrl) {
                            hideSpinner();
                            Timber.d("getting url " + previewUrl);
                            webView.loadUrl(previewUrl);
                            PreviewActivity.this.previewUrl = previewUrl;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_URL, previewUrl);
    }

}
