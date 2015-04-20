package org.faudroids.mrhyde.jekyll;

/**
 * Data given to the server in case of a put
 */
public class RepoDiff {

    private String diff;

    public String getDiff()
    {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }
}
