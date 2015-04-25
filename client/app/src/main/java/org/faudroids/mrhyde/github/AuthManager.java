package org.faudroids.mrhyde.github;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;

/**
 * Responsible for storing and clearing (e.g. on logout) the GitHub user credentials.
 */
public final class AuthManager {

	private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";

	private final Context context;

	@Inject
	AuthManager(Context context) {
		this.context = context;
	}


	public void setAccessToken(String accessToken) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(KEY_ACCESS_TOKEN, accessToken);
		editor.commit();
	}


	public String getAccessToken() {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_ACCESS_TOKEN, null);
	}


	public void clearAccessToken() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(KEY_ACCESS_TOKEN);
		editor.commit();
	}

}
