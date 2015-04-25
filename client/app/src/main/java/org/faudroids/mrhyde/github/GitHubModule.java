package org.faudroids.mrhyde.github;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

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
	public UserService provideUserService(AuthManager authManager) {
		return setAuthToken(new UserService(), authManager);
	}


	@Provides
	@Inject
	public RepositoryService provideRepositoryService(AuthManager authManager) {
		return setAuthToken(new RepositoryService(), authManager);
	}


	@Provides
	@Inject
	public CommitService provideCommitService(AuthManager authManager) {
		return setAuthToken(new CommitService(), authManager);
	}


	@Provides
	@Inject
	public DataService provideDataService(AuthManager authManager) {
		return setAuthToken(new DataService(), authManager);
	}


	@Provides
	@Inject
	public OrganizationService provideOrganizationService(AuthManager authManager) {
		return setAuthToken(new OrganizationService(), authManager);
	}


	private <T extends GitHubService> T setAuthToken(T service, AuthManager authManager) {
		service.getClient().setOAuth2Token(authManager.getAccessToken());
		return service;
	}

}
