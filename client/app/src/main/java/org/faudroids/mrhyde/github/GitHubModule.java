package org.faudroids.mrhyde.github;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.OrganizationService;
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
	public GitHubAuthApi provideGitHubApi(RestAdapter restAdapter) {
		return restAdapter.create(GitHubAuthApi.class);
	}


	@Provides
	@Inject
	public RepositoryService provideRepositoryService(LoginManager loginManager) {
		return setAuthToken(new RepositoryService(), loginManager);
	}


	@Provides
	@Inject
	public CommitService provideCommitService(LoginManager loginManager) {
		return setAuthToken(new CommitService(), loginManager);
	}


	@Provides
	@Inject
	public DataService provideDataService(LoginManager loginManager) {
		return setAuthToken(new DataService(), loginManager);
	}


	@Provides
	@Inject
	public OrganizationService provideOrganizationService(LoginManager loginManager) {
		return setAuthToken(new OrganizationService(), loginManager);
	}


	private <T extends GitHubService> T setAuthToken(T service, LoginManager loginManager) {
		service.getClient().setOAuth2Token(loginManager.getAccount().getAccessToken());
		return service;
	}

}
