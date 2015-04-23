package org.faudroids.mrhyde.jekyll;

import java.util.List;

/**
 * Result returned from the server
 */
public class PreviewResult {

    private String previewId;
    private String previewUrl;
    private long previewExpirationDate;
    private List<String> jekyllErrors;

    public PreviewResult() { }

    public PreviewResult(String previewId, String previewUrl, long previewExpirationDate, List<String> jekyllErrors) {
        this.previewId = previewId;
        this.previewUrl = previewUrl;
        this.previewExpirationDate = previewExpirationDate;
        this.jekyllErrors = jekyllErrors;
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

    public List<String> getJekyllErrors() {
        return jekyllErrors;
    }

    public void setJekyllErrors(List<String> jekyllErrors) {
        this.jekyllErrors = jekyllErrors;
    }

}
