package org.faudroids.mrhyde.jekyll_server;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Result returned from the server
 */
public class JekyllOutput {

    @SerializedName("URL")
    private String url;
    @SerializedName("expiration_date")
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
