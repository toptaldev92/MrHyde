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

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_preview)
public class PreviewActivity extends AbstractActionBarActivity {

    static final String
            EXTRA_REPO = "EXTRA_REPO",
            EXTRA_DIFF = "EXTRA_DIFF";

    @InjectView(R.id.web_view) WebView webView;
    @Inject PreviewManager previewManager;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enable javascript + ignore SSL errors
        webView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
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
        previewManager.loadPreview(repository.getOwner().getName() + "/" + repository.getName(), repository.getCloneUrl(), diff)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String previewUrl) {
                        Timber.d("getting url " + previewUrl);
                        webView.loadUrl(previewUrl);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "failed to get results from server");
                    }
                });

    }
}
