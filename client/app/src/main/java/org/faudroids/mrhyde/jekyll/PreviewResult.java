package org.faudroids.mrhyde.jekyll;

/**
 * Result returned from the server
 */
public class PreviewResult {

    private String previewId;
    private String previewUrl;
    private long previewExpirationDate;

    public PreviewResult() { }

    public PreviewResult(String previewId, String previewUrl, long previewExpirationDate) {
        this.previewId = previewId;
        this.previewUrl = previewUrl;
        this.previewExpirationDate = previewExpirationDate;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public long getExpirationDatexpirationDate() {
        return previewExpirationDate;
    }

    public void setExpireDatation(long expirationDate) {
        this.previewExpirationDate = expirationDate;
    }

    public String getPreviewId() {
        return previewId;
    }

    public void setPreviewId(String previewId) {
        this.previewId = previewId;
    }

}
