package org.faudroids.mrhyde.jekyll;

/**
 * Data given to the server in case of a post
 */
public final class RepoDetails {

    private String gitCheckoutUrl;
    private String gitDiff;
    private String clientSecret;

    public RepoDetails() { }

    public RepoDetails(String gitCheckoutUrl, String gitDiff, String clientSecret) {
        this.gitCheckoutUrl = gitCheckoutUrl;
        this.gitDiff = gitDiff;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
