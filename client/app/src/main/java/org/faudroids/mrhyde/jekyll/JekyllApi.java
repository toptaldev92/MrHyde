package org.faudroids.mrhyde.jekyll;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

public interface JekyllApi {

    static final String
            JEKYLL_URL = "/jekyll_testing/",
            ID_URL = JEKYLL_URL + "{previewId}/";

    /**
     * Returns the repo URL and the expiration date encoded in {@link PreviewResult}.
     */
    @POST(JEKYLL_URL)
    public Observable<PreviewResult> createPreview(@Body RepoDetails input);


    /**
     * Returns the repo URL and the expiration date encoded in {@link PreviewResult}.
     */
    @PUT(ID_URL)
    public Observable<PreviewResult> updatePreview(
            @Path("previewId") String previewId,
            @Body RepoDiff input);


    /**
     * Deletes the specified site.
     */
    @DELETE(ID_URL)
    public void deletePreview(@Path("previewId") String previewId);

}
