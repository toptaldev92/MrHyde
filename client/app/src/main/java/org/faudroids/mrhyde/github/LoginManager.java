package org.faudroids.mrhyde.github;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Responsible for storing and clearing (e.g. on logout) the GitHub user credentials.
 */
public final class LoginManager {

	private static final String
			KEY_ACCESS_TOKEN = "ACCESS_TOKEN",
			KEY_LOGIN = "LOGIN",
			KEY_EMAIL = "EMAIL";

	private static final String AVATAR_FILE_NAME = "avatar.png";

	private final Context context;
	private Account accountCache = null;

	@Inject
	LoginManager(Context context) {
		this.context = context;
	}


	public void setAccount(Account account) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(KEY_ACCESS_TOKEN, account.getAccessToken());
		editor.putString(KEY_LOGIN, account.getLogin());
		editor.putString(KEY_EMAIL, account.getEmail());
		editor.commit();
		storeAvatar(account.getAvatar());
		accountCache = account;
	}


	public Account getAccount() {
		if (accountCache == null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (!prefs.contains(KEY_ACCESS_TOKEN)) return null;
			accountCache = new Account(
					prefs.getString(KEY_ACCESS_TOKEN, null),
					prefs.getString(KEY_LOGIN, null),
					prefs.getString(KEY_EMAIL, null),
					loadAvatar());
		}
		return accountCache;
	}


	@SuppressWarnings("deprecation")
	public void clearAccount() {
		// clear local credentials
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(KEY_ACCESS_TOKEN);
		editor.remove(KEY_LOGIN);
		editor.remove(KEY_EMAIL);
		editor.commit();

		// clear credentials stored in cookies
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
	}


	private void storeAvatar(Bitmap avatar) {
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(new File(context.getFilesDir(), AVATAR_FILE_NAME));
			avatar.compress(Bitmap.CompressFormat.PNG, 100, outStream);
		} catch (Exception e) {
			Timber.e(e, "failed to store bitmap");
		} finally {
			try {
				if (outStream != null) outStream.close();
			} catch (IOException e) {
				Timber.e(e, "failed to close stream");
			}
		}
	}


	private Bitmap loadAvatar() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeFile(new File(context.getFilesDir(), AVATAR_FILE_NAME).getAbsolutePath(), options);
	}


	private void deleteAvatar() {
		File file = new File(AVATAR_FILE_NAME);
		file.deleteOnExit();
	}


	public static final class Account {

		private final String accessToken, login, email;
		private final Bitmap avatar;

		public Account(String accessToken, String login, String email, Bitmap avatar) {
			this.accessToken = accessToken;
			this.login = login;
			this.email = email;
			this.avatar = avatar;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public String getLogin() {
			return login;
		}

		public String getEmail() {
			return email;
		}

		public Bitmap getAvatar() {
			return avatar;
		}
	}

}
