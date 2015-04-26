package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.AuthApi;
import org.faudroids.mrhyde.github.LoginManager;
import org.faudroids.mrhyde.github.TokenDetails;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;


@ContentView(R.layout.activity_login)
public final class LoginActivity extends AbstractActionBarActivity {

	@InjectView(R.id.login_button) Button loginButton;
	@Inject AuthApi authApi;
	@Inject
	LoginManager loginManager;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (loginManager.getAccount() != null) {
			onLoginSuccess();
			return;
		}

		loginButton.setOnClickListener(new View.OnClickListener() {
			private Dialog loginDialog;

			@Override
			public void onClick(View arg0) {
				final String state = UUID.randomUUID().toString();
				loginDialog = new Dialog(LoginActivity.this);
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
							loginDialog.dismiss();
							onAccessDenied();
						}
					}
				});
				loginDialog.show();
				loginDialog.setTitle(getString(R.string.login_title));
				loginDialog.setCancelable(true);
			}
		});
	}


	private void getAccessToken(String code) {
		String clientId = getString(R.string.gitHubClientId);
		String clientSecret = getString(R.string.gitHubClientSecret);
		compositeSubscription.add(authApi.getAccessToken(clientId, clientSecret, code)
				.flatMap(new Func1<TokenDetails, Observable<LoginManager.Account>>() {
					@Override
					public Observable<LoginManager.Account> call(TokenDetails tokenDetails) {
						try {
							// load user
							UserService service = new UserService();
							service.getClient().setOAuth2Token(tokenDetails.getAccessToken());
							User user = service.getUser();

							// load avatar
							URL url = new URL(user.getAvatarUrl());
							HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
							connection.setDoInput(true);
							connection.connect();
							InputStream input = connection.getInputStream();
							Bitmap avatar = BitmapFactory.decodeStream(input);
							return Observable.just(new LoginManager.Account(tokenDetails.getAccessToken(), user.getLogin(), user.getEmail(), avatar));

						} catch (IOException e) {
							return Observable.error(e);
						}
					}
				})
				.compose(new DefaultTransformer<LoginManager.Account>())
				.subscribe(new Action1<LoginManager.Account>() {
					@Override
					public void call(LoginManager.Account account) {
						Timber.d("gotten token " + account.getAccessToken());
						loginManager.setAccount(account);
						onLoginSuccess();
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Timber.d(throwable, "failed to get token");
					}
				}));
	}


	private void onLoginSuccess() {
		startActivity(new Intent(LoginActivity.this, MainDrawerActivity.class));
		finish();
	}


	private void onAccessDenied() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.login_error_title)
				.setMessage(R.string.login_error_message)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

}
