package org.faudroids.mrhyde.jekyll_server;

/**
 * Data given to the server in case of a post
 */
public final class JekyllInputPost {

    private String url;
    private String diff;

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
}
