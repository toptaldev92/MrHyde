package org.faudroids.mrhyde;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.faudroids.mrhyde.github.GitHubApi;
import org.faudroids.mrhyde.github.TokenDetails;

import java.util.UUID;

import javax.inject.Inject;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;


@ContentView(R.layout.activity_login)
public final class LoginActivity extends RoboActivity{

	private static final  String
			OAUTH_URL = "https://github.com/login/oauth/authorize",
			OAUTH_SCOPE = "user%2Crepo",
			CLIENT_ID = "bec1da051d782d9fc8b6";


	@InjectView(R.id.login_button) Button loginButton;
	@Inject GitHubApi gitHubApi;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loginButton.setOnClickListener(new View.OnClickListener() {
			private Dialog loginDialog;

			@Override
			public void onClick(View arg0) {
				final String state = UUID.randomUUID().toString();
				loginDialog = new Dialog(LoginActivity.this);
				loginDialog.setContentView(R.layout.dialog_login);
				WebView webView = (WebView) loginDialog.findViewById(R.id.webview);
				// webView.getSettings().setJavaScriptEnabled(true);
				webView.loadUrl(OAUTH_URL + "?"
						+ "&client_id=" + CLIENT_ID
						+ "&scope=" + OAUTH_SCOPE
						+ "&state=" + state);
				webView.setWebViewClient(new WebViewClient() {
					@Override
					public void onPageFinished(WebView view, String url) {
						super.onPageFinished(view, url);
						if (url.contains("code=")) {
							Uri uri = Uri.parse(url);
							String code = uri.getQueryParameter("code");
							if (!state.equals(uri.getQueryParameter("state"))) {
								Timber.w("state did not match");
								return;
							}

							getAccessToken(code);
							loginDialog.dismiss();

						} else if (url.contains("error=access_denied")) {
							Timber.d("access denied");
							loginDialog.dismiss();
							// TODO show to user
						}
					}
				});
				loginDialog.show();
				loginDialog.setTitle("Authorize me!!");
				loginDialog.setCancelable(true);
			}
		});
	}


	private void getAccessToken(String code) {
		String clientSecret = getString(R.string.gitHubClientSecret);
		gitHubApi.getAccessToken(CLIENT_ID, clientSecret, code)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<TokenDetails>() {
					@Override
					public void call(TokenDetails tokenDetails) {
						Timber.d("gotten token " + tokenDetails.getAccessToken() + " for scope " + tokenDetails.getScope());
					}
				},new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable)  {
						Timber.d(throwable, "failed to get token");
					}
				});
	}
}
