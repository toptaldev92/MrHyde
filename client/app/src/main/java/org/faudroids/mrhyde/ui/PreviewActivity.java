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
import org.faudroids.mrhyde.git.FileManagerFactory;
import org.faudroids.mrhyde.jekyll.PreviewManager;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

@ContentView(R.layout.activity_preview)
public class PreviewActivity extends AbstractActionBarActivity {

    private static final String STATE_URL = "STATE_URL";

    static final String EXTRA_REPO = "EXTRA_REPO";

    @Inject private PreviewManager previewManager;
	@Inject private FileManagerFactory fileManagerFactory;
    @InjectView(R.id.web_view) private WebView webView;

    private String previewUrl;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_preview));

        // load arguments
        Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPO);

		// setup preview view
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
					.loadPreview(fileManagerFactory.createFileManager(repository))
					.compose(new DefaultTransformer<String>())
					.subscribe(new Action1<String>() {
								   @Override
								   public void call(String previewUrl) {
									   Timber.d("getting url " + previewUrl);
									   webView.loadUrl(previewUrl);
									   hideSpinner();
								   }
							   },
							new ErrorActionBuilder()
									.add(new DefaultErrorAction(this, "failed to get preview from server"))
									.add(new HideSpinnerAction(this))
									.build()));
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
