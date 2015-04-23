package org.faudroids.mrhyde.jekyll;

/**
 * Data given to the server in case of a put
 */
public class RepoDiff {

    private String gitDiff;
    private String clientSecret;

    public RepoDiff() { }

    public RepoDiff(String gitDiff, String clientSecret) {
        this.gitDiff = gitDiff;
        this.clientSecret = clientSecret;
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
