package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllApi;
import org.faudroids.mrhyde.jekyll.JekyllModule;
import org.faudroids.mrhyde.jekyll.PreviewResult;
import org.faudroids.mrhyde.jekyll.RepoDetails;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_preview)
public class PreviewActivity extends AbstractActionBarActivity {

    static final String
            EXTRA_REPO_CHECKOUT_URL = "EXTRA_REPO_CHECKOUT_URL",
            EXTRA_DIFF = "EXTRA_DIFF";

    @InjectView(R.id.web_view) WebView webView;
    private String repoUrl;
    private String diff;


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
        repoUrl = getIntent().getStringExtra(EXTRA_REPO_CHECKOUT_URL);
        diff = getIntent().getStringExtra(EXTRA_DIFF);

        // load preview
        JekyllApi jekyllApi = new JekyllModule().provideJekyllApi();
        RepoDetails repoDetails = new RepoDetails(repoUrl, diff, getString(R.string.jekyllServerClientSecret));
        jekyllApi.createPreview(repoDetails)
                .compose(new DefaultTransformer<PreviewResult>())
                .subscribe(new Action1<PreviewResult>() {
                    @Override
                    public void call(PreviewResult previewResult) {
                        Timber.d("getting url " + previewResult.getPreviewUrl());
                        webView.loadUrl(previewResult.getPreviewUrl());
                        for (String error : previewResult.getJekyllErrors()) {
                            Timber.w("Jekyll error: " + error);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "failed to get results from server");
                    }
                });

    }
}
