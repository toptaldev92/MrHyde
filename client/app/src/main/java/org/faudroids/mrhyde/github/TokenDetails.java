package org.faudroids.mrhyde.github;


import com.google.gson.annotations.SerializedName;

/**
 * GitHub OAuth credentials (and some meta information ...).
 */
public final class TokenDetails {

	@SerializedName("access_token")
	private String accessToken;
	@SerializedName("token_type")
	private String tokenType;
	private String scope;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

}
