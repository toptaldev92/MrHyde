package org.faudroids.mrhyde.jekyll_server;

import com.google.gson.annotations.SerializedName;

/**
 * Data given to the server
 */
public final class JekyllInput {

    @SerializedName("URL")
    private String url;
    @SerializedName("Diff")
    private String diff;
    private String scope;

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

    public void setDiff(String diff)
    {
        this.diff = diff;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }
}
