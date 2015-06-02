package org.faudroids.mrhyde.jekyll;

import java.util.List;

/**
 * Data given to the server in case of a post
 */
public final class RepoDetails {

    private String gitCheckoutUrl;
    private String gitDiff;
    private List<BinaryFile> staticFiles;
    private String clientSecret;

    public RepoDetails() { }

    public RepoDetails(String gitCheckoutUrl, String gitDiff, List<BinaryFile> staticFiles, String clientSecret) {
        this.gitCheckoutUrl = gitCheckoutUrl;
        this.gitDiff = gitDiff;
        this.staticFiles = staticFiles;
        this.clientSecret = clientSecret;
    }

    public String getGitCheckoutUrl() {
        return gitCheckoutUrl;
    }

    public void setGitCheckoutUrl(String gitCheckoutUrl) {
        this.gitCheckoutUrl = gitCheckoutUrl;
    }

    public String getGitDiff() {
        return gitDiff;
    }

    public void setGitDiff(String gitDiff) {
        this.gitDiff = gitDiff;
    }

    public List<BinaryFile> getStaticFiles() {
        return staticFiles;
    }

    public void setStaticFiles(List<BinaryFile> staticFiles) {
        this.staticFiles = staticFiles;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
