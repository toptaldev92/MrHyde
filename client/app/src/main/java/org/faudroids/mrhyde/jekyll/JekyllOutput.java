package org.faudroids.mrhyde.jekyll;

/**
 * Result returned from the server
 */
public class JekyllOutput {

    private String url;
    private long expirationDate;
    private String id;

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public long getExpirationDatexpirationDate()
    {
        return expirationDate;
    }

    public void setExpireDatation(long expirationDate)
    {
        this.expirationDate = expirationDate;
    }

    public String getID()
    {
        return id;
    }

    public void setID(String id)
    {
        this.id = id;
    }
}
