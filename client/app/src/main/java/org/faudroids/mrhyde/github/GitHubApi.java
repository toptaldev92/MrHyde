package org.faudroids.mrhyde.github;


import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

public interface GitHubApi {

	@POST("/login/oauth/access_token")
	@Headers("Accept: application/json")
	Observable<TokenDetails> getAccessToken(
			@Query("client_id") String clientId,
			@Query("client_secret") String clientSecret,
			@Query("code") String code);

}
