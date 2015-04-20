package org.faudroids.mrhyde.jekyll_server;

import java.util.Date;

/**
 * Result returned from the server
 */
public class JekyllOutput {

    private String url;
    private Date expirationDate;

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public Date getExpirationDatexpirationDate()
    {
        return expirationDate;
    }

    public void setExpireDatation(Date expirationDate)
    {
        this.expirationDate = expirationDate;
    }
}
