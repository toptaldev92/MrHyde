package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllApi;
import org.faudroids.mrhyde.jekyll.JekyllModule;
import org.faudroids.mrhyde.jekyll.JekyllOutput;
import org.faudroids.mrhyde.jekyll.RepoDetails;

import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

public class PreviewFragment extends AbstractFragment {

    private static final String EXTRA_URLREPO = "EXTRA_URLREPO";
    private static final String EXTRA_DIFF = "EXTRA_DIFF";

    private String urlRepo;
    private String diff;
    private String urlSite;
    private long expirationDate;
    @InjectView(R.id.webPage) WebView webView;

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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        urlRepo = (String) getArguments().getSerializable(EXTRA_URLREPO);
        diff = (String) getArguments().getSerializable(EXTRA_DIFF);

        JekyllApi jekyllApi = new JekyllModule().provideJekyllApi();
        RepoDetails repoDetails = new RepoDetails();
        repoDetails.setDiff(diff);
        repoDetails.setURL(urlRepo);
        repoDetails.setSecret(getString(R.string.jekyllServerClientSecret));

        Observable<JekyllOutput> output = jekyllApi.createPreview(repoDetails);
        output.subscribe(new Action1<JekyllOutput>() {
            @Override
            public void call(JekyllOutput jekyllOutput) {
                urlSite = jekyllOutput.getURL();
                expirationDate = jekyllOutput.getExpirationDatexpirationDate();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e(throwable, "failed to get results from server");
            }
        } );

        webView.loadUrl(urlSite);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

    }
}
