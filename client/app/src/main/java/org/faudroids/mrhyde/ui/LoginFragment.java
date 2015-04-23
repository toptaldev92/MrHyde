package org.faudroids.mrhyde.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.AuthApi;
import org.faudroids.mrhyde.github.AuthManager;
import org.faudroids.mrhyde.github.TokenDetails;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.UUID;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;


public final class LoginFragment extends AbstractFragment {

	@InjectView(R.id.login_button) Button loginButton;
	@Inject AuthApi authApi;
	@Inject AuthManager authManager;

	public LoginFragment() {
		super(R.layout.fragment_login);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		loginButton.setOnClickListener(new View.OnClickListener() {
			private Dialog loginDialog;

			@Override
			public void onClick(View arg0) {
				final String state = UUID.randomUUID().toString();
				loginDialog = new Dialog(getActivity());
				loginDialog.setContentView(R.layout.dialog_login);
				WebView webView = (WebView) loginDialog.findViewById(R.id.webview);
				webView.loadUrl("https://github.com/login/oauth/authorize?"
						+ "&client_id=" + getString(R.string.gitHubClientId)
						+ "&scope=user%2Crepo"
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
		String clientId = getString(R.string.gitHubClientId);
		String clientSecret = getString(R.string.gitHubClientSecret);
		compositeSubscription.add(authApi.getAccessToken(clientId, clientSecret, code)
				.compose(new DefaultTransformer<TokenDetails>())
				.subscribe(new Action1<TokenDetails>() {
					@Override
					public void call(TokenDetails tokenDetails) {
						Timber.d("gotten token " + tokenDetails.getAccessToken() + " for scope " + tokenDetails.getScope());
						authManager.setTokenDetails(tokenDetails);
						uiUtils.replaceFragment(LoginFragment.this, new ReposFragment());
					}
				},new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable)  {
						Timber.d(throwable, "failed to get token");
					}
				}));
	}
}
