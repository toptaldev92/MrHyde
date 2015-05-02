package org.faudroids.mrhyde.jekyll;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.inject.Inject;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.github.LoginManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

public class PreviewManager {

	private static final String
			PREFS_NAME = PreviewManager.class.getName(),
			PREFS_KEY_PREVIEW_EXPIRATION = "_preview_expiration",
			PREFS_KEY_PREVIEW_ID = "_preview_id";

	private final Context context;
	private final JekyllApi jekyllApi;
	private final String clientSecret;
	private final LoginManager loginManager;

	@Inject
	PreviewManager(Context context, JekyllApi jekyllApi, LoginManager loginManager) {
		this.context = context;
		this.jekyllApi = jekyllApi;
		this.clientSecret = context.getString(R.string.jekyllServerClientSecret);
		this.loginManager = loginManager;
	}


	public Observable<String> loadPreview(Repository repository, String diff) {
		// TODO this is not that great security wise. In the long run use https://help.github.com/articles/git-automation-with-oauth-tokens/
		String repoName = repository.getOwner().getLogin() + "/" + repository.getName();
		String cloneUrl = "https://" + loginManager.getAccount().getAccessToken() + ":x-oauth-basic@" + repository.getCloneUrl().replaceFirst("https://", "");
		long expirationDate = getPreviewExpiration(repoName);

		// if (expirationDate <= (System.currentTimeMillis() / 1000)) {
			// create new preview
			Timber.d("starting new preview");
			final RepoDetails repoDetails = new RepoDetails(cloneUrl, diff, clientSecret);
			return jekyllApi.createPreview(repoDetails)
					.compose(new DefaultTransformer<PreviewResult>())
					.flatMap(new StorePreviewFunction(repoName));

		/*
		} else {
			// update old preview
			Timber.d("updating preview");
			final RepoDiff repoDiff = new RepoDiff(diff, clientSecret);
			final String previewId = getPreviewId(repoName);

			return jekyllApi.updatePreview(previewId, repoDiff)
					.compose(new DefaultTransformer<PreviewResult>())
					.flatMap(new StorePreviewFunction(repoName));
		}
		*/

	}


	private long getPreviewExpiration(String repoName) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return prefs.getLong(getExpirationKey(repoName), 0);
	}


	private String getPreviewId(String repoName) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return prefs.getString(getIdKey(repoName), null);
	}


	private void storePreview(String repoName, String previewId, long previewExpirationDate) {
		SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(getIdKey(repoName), previewId);
		editor.putLong(getExpirationKey(repoName), previewExpirationDate);
		editor.commit();
	}


	private String getExpirationKey(String repoName) {
		return repoName + PREFS_KEY_PREVIEW_EXPIRATION;
	}


	private String getIdKey(String repoName) {
		return repoName + PREFS_KEY_PREVIEW_ID;
	}


	private final class StorePreviewFunction implements Func1<PreviewResult, Observable<String>> {

		private final String repoName;

		public StorePreviewFunction(String repoName) {
			this.repoName = repoName;
		}

		@Override
		public Observable<String> call(PreviewResult previewResult) {
			// store preview id and expiration date for future updates
			Timber.d("preview loaded, expiration date is " + previewResult.getExpirationDatexpirationDate() + ", id is " + previewResult.getPreviewId());
			storePreview(repoName, previewResult.getPreviewId(), previewResult.getExpirationDatexpirationDate());
			return Observable.just(previewResult.getPreviewUrl());
		}

	}

}
