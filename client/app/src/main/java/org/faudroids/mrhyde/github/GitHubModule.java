package org.faudroids.mrhyde.github;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import javax.inject.Inject;

import retrofit.RestAdapter;
import roboguice.inject.ContextSingleton;

public final class GitHubModule implements Module {

	@Override
	public void configure(Binder binder) {
		// nothing to do for now
	}


	@Provides
	@ContextSingleton
	public RestAdapter provideRestAdapter() {
		return new RestAdapter.Builder()
				.setEndpoint("https://github.com")
				.build();
	}


	@Provides
	@ContextSingleton
	@Inject
	public GitHubApi provideGitHubApi(RestAdapter restAdapter) {
		return restAdapter.create(GitHubApi.class);
	}

}
