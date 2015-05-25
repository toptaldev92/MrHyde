package org.faudroids.mrhyde.app;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.faudroids.mrhyde.github.LoginManager;

import javax.inject.Inject;

import timber.log.Timber;

public class MigrationManager {

	private static final String PREFS_NAME = MigrationManager.class.getSimpleName();
	private static final String KEY_VERSION = "VERSION";

	private static final int NO_VERSION = -1;

	private final Context context;
	private final LoginManager loginManager;

	@Inject
	MigrationManager(Context context, LoginManager loginManager) {
		this.context = context;
		this.loginManager = loginManager;
	}


	public void doMigration() {
		int currentVersionCode = getCurrentVersionCode();
		int storedVersionCode = getStoredVersionCode();
		storeVersionCode(currentVersionCode);

		if (storedVersionCode == currentVersionCode) {
			return;
		}

		Timber.i("migrating from " + storedVersionCode + " to " + currentVersionCode);
		if (storedVersionCode == NO_VERSION) storedVersionCode = 3;


		if (storedVersionCode < 4 && currentVersionCode >= 4) {
			Timber.d("migrating to version 4");
			migrateToVersion4();
		}
	}


	/**
	 * Version 4 fixes reading emails not just from the public GitHub profile,
	 * but also from the private list of mails. If no mail is stored with the
	 * {@link LoginManager} logout to ensure mail fetched during next login.
	 */
	private void migrateToVersion4() {
		LoginManager.Account account = loginManager.getAccount();
		if (account != null && (account.getEmail() == null || account.getEmail().isEmpty())) {
			loginManager.clearAccount();
		}
	}


	private int getCurrentVersionCode() {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (PackageManager.NameNotFoundException nnfe) {
			Timber.e(nnfe, "failed to get package info");
			return NO_VERSION;
		}
	}


	private int getStoredVersionCode() {
		return context
				.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
				.getInt(KEY_VERSION, NO_VERSION);
	}


	private void storeVersionCode(int versionCode) {
		SharedPreferences.Editor editor= context
				.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
				.edit();
		editor.putInt(KEY_VERSION, versionCode);
		editor.apply();
	}

}
