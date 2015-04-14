package org.faudroids.mrhyde.github;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.eclipse.egit.github.core.service.RepositoryService;

import javax.inject.Inject;

import retrofit.RestAdapter;

public final class GitHubModule implements Module {

	@Override
	public void configure(Binder binder) {
		// nothing to do for now
	}


	@Provides
	public RestAdapter provideRestAdapter() {
		return new RestAdapter.Builder()
				.setEndpoint("https://github.com")
				.build();
	}


	@Provides
	@Inject
	public AuthApi provideGitHubApi(RestAdapter restAdapter) {
		return restAdapter.create(AuthApi.class);
	}


	@Provides
	@Inject
	public RepositoryService provideRepositoryService(AuthManager authManager) {
		RepositoryService service = new RepositoryService();
		service.getClient().setOAuth2Token(authManager.getTokenDetails().getAccessToken());
		return service;
	}

}
