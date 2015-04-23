package org.faudroids.mrhyde.jekyll;

/**
 * Data given to the server in case of a put
 */
public class RepoDiff {

    private String diff;
    private String secret;

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
