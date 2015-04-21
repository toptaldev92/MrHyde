package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.faudroids.mrhyde.R;

import roboguice.inject.InjectView;

public class PreviewFragment extends AbstractFragment {

    private static final String EXTRA_URL = "EXTRA_URL";

    private String url;
    @InjectView(R.id.webPage) WebView webView;

    public static PreviewFragment createInstance(String url) {
        PreviewFragment fragment = new PreviewFragment();
        Bundle extras = new Bundle();
        extras.putSerializable(EXTRA_URL, url);
        fragment.setArguments(extras);
        return fragment;
    }

    public PreviewFragment() {
        super(R.layout.fragment_preview);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        actionBarListener.setTitle(getString(R.string.title_preview));

        url = (String) getArguments().getSerializable(EXTRA_URL);
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }
}
