package org.faudroids.mrhyde.jekyll_server;

/**
 * Data given to the server in case of a put
 */
public class JekyllInputPut {

    private String diff;

    public String getDiff()
    {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }
}
