package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllApi;
import org.faudroids.mrhyde.jekyll.JekyllModule;
import org.faudroids.mrhyde.jekyll.JekyllOutput;
import org.faudroids.mrhyde.jekyll.RepoDetails;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

public class PreviewFragment extends AbstractFragment {

    private static final String EXTRA_URLREPO = "EXTRA_URLREPO";
    private static final String EXTRA_DIFF = "EXTRA_DIFF";

    private String urlRepo;
    private String diff;
    private long expirationDate;
    @InjectView(R.id.web_view) WebView webView;

    public static PreviewFragment createInstance(String urlRepo, String diff) {
        PreviewFragment fragment = new PreviewFragment();
        Bundle extras = new Bundle();
        extras.putSerializable(EXTRA_URLREPO, urlRepo);
        extras.putSerializable(EXTRA_DIFF, diff);
        fragment.setArguments(extras);
        return fragment;
    }

    public PreviewFragment() {
        super(R.layout.fragment_preview);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        urlRepo = (String) getArguments().getSerializable(EXTRA_URLREPO);
        diff = (String) getArguments().getSerializable(EXTRA_DIFF);

        // load preview
        JekyllApi jekyllApi = new JekyllModule().provideJekyllApi();
        RepoDetails repoDetails = new RepoDetails();
        repoDetails.setDiff(diff);
        repoDetails.setURL(urlRepo);
        repoDetails.setSecret(getString(R.string.jekyllServerClientSecret));
        jekyllApi.createPreview(repoDetails)
                .compose(new DefaultTransformer<JekyllOutput>())
                .subscribe(new Action1<JekyllOutput>() {
                    @Override
                    public void call(JekyllOutput jekyllOutput) {
                        expirationDate = jekyllOutput.getExpirationDatexpirationDate();
                        Timber.d("getting url " + jekyllOutput.getURL());
                        webView.loadUrl(jekyllOutput.getURL());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "failed to get results from server");
                    }
                });

    }
}
