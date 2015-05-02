package org.faudroids.mrhyde.app;


import android.app.Application;

import com.crashlytics.android.Crashlytics;

import org.faudroids.mrhyde.BuildConfig;
import org.faudroids.mrhyde.github.GitHubModule;
import org.faudroids.mrhyde.jekyll.JekyllModule;

import io.fabric.sdk.android.Fabric;
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

		if (!BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		} else {
			Fabric.with(this, new Crashlytics());
			Timber.plant(new CrashReportingTree());
		}
	}


	private static final class CrashReportingTree extends Timber.HollowTree {

		@Override
		public void e(String msg, Object... args) {
			Crashlytics.log(msg);
		}

		@Override
		public void e(Throwable e, String msg, Object... args) {
			Crashlytics.log(msg);
			Crashlytics.logException(e);
		}


		@Override
		public void w(String msg, Object... args) {
			Crashlytics.log(msg);
		}

		@Override
		public void w(Throwable e, String msg, Object... args) {
			Crashlytics.log(msg);
			Crashlytics.logException(e);
		}

	}
}
