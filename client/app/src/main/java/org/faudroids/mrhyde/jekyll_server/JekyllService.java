package org.faudroids.mrhyde.jekyll_server;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

public interface JekyllService {

    static final String
            JEKYLL_URL = "/jekyll",
            ID_URL = JEKYLL_URL + "/{id}";

    /**
     * Returns the repo URL and the expiration date encoded in JekyllOutput
     */
    @POST(JEKYLL_URL)
    public Observable<JekyllOutput> post(
            @Body JekyllInputPost input);


    /**
     * Returns the repo URL and the expiration date encoded in JekyllOutput
     */
    @PUT(ID_URL)
    public Observable<JekyllOutput> post(
            @Path("id") String id,
            @Body JekyllInputPut input);


    /**
     * Deletes the specified site
     */
    @DELETE(ID_URL)
    public void deleteSite(
            @Path("id") String id);

}
