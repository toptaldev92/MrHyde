package org.faudroids.mrhyde.app;


import android.app.Application;

import org.faudroids.mrhyde.BuildConfig;
import org.faudroids.mrhyde.github.GitHubModule;
import org.faudroids.mrhyde.jekyll.JekyllModule;

import roboguice.RoboGuice;
import timber.log.Timber;

public final class MrHydeApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		RoboGuice.getOrCreateBaseApplicationInjector(
				this,
				RoboGuice.DEFAULT_STAGE,
				RoboGuice.newDefaultRoboModule(this),
				new GitHubModule(),
				new JekyllModule());

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		} else {
			throw new UnsupportedOperationException("setup logging and crashlytics");
		}
	}
}
