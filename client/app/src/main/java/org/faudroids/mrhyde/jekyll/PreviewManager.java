package org.faudroids.mrhyde.jekyll;

import com.google.inject.Inject;

import org.faudroids.mrhyde.git.FileManager;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

public class PreviewManager {

	private final JekyllApi jekyllApi;
	private final RepoDetailsFactory repoDetailsFactory;

	@Inject
	PreviewManager(JekyllApi jekyllApi, RepoDetailsFactory repoDetailsFactory) {
		this.jekyllApi = jekyllApi;
		this.repoDetailsFactory = repoDetailsFactory;
	}


	/**
	 * Triggers a new preview and returns the preview URL.
	 */
	public Observable<String> loadPreview(FileManager fileManager) {
		Timber.d("starting new preview");
		return repoDetailsFactory.createRepoDetails(fileManager )
				.flatMap(new Func1<RepoDetails, Observable<PreviewResult>>() {
					@Override
					public Observable<PreviewResult> call(RepoDetails repoDetails) {
						Timber.d("found " + repoDetails.getStaticFiles().size() + " binary files for preview");
						for (BinaryFile file : repoDetails.getStaticFiles()) Timber.d(file.getPath());
						return jekyllApi.createPreview(repoDetails);
					}
				})
				.flatMap(new Func1<PreviewResult, Observable<String>>() {
					@Override
					public Observable<String> call(PreviewResult previewResult) {
						return Observable.just(previewResult.getPreviewUrl());
					}
				});

	}

}
