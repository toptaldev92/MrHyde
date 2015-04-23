package org.faudroids.mrhyde.jekyll;

/**
 * Data given to the server in case of a post
 */
public final class RepoDetails {

    private String url;
    private String diff;
    private String secret;

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public String getDiff()
    {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
