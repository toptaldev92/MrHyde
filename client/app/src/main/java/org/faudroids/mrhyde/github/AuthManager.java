package org.faudroids.mrhyde.github;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Responsible for storing and clearing (e.g. on logout) the GitHub user credentials.
 */
@Singleton
public final class AuthManager {

	private TokenDetails tokenDetails;


	@Inject
	AuthManager() { }


	public void setTokenDetails(TokenDetails tokenDetails) {
		this.tokenDetails = tokenDetails;
	}


	public TokenDetails getTokenDetails() {
		return tokenDetails;
	}

}
