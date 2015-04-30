package org.faudroids.mrhyde.jekyll;


import android.content.Context;

import com.google.inject.Inject;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import rx.Observable;
import rx.functions.Func1;

public class PreviewManager {

	private final JekyllApi jekyllApi;
	private final String clientSecret;

	@Inject
	PreviewManager(Context context, JekyllApi jekyllApi) {
		this.jekyllApi = jekyllApi;
		this.clientSecret = context.getString(R.string.jekyllServerClientSecret);
	}


	public Observable<String> loadPreview(String repoUrl, String diff) {
		final RepoDetails repoDetails = new RepoDetails(repoUrl, diff, clientSecret);
		return jekyllApi.createPreview(repoDetails)
				.compose(new DefaultTransformer<PreviewResult>())
				.flatMap(new Func1<PreviewResult, Observable<String>>() {
					@Override
					public Observable<String> call(PreviewResult previewResult) {
						return Observable.just(previewResult.getPreviewUrl());
					}
				});
	}

}
