
package com.ggt.airshare.urlshortener;

/**
 * Interface to implement to listen to Google Shortener events
 * 
 * @author gduche
 */
public interface UrlShortenerListener {

    public void onUrlShortened(String sourceUrl, String urlShortened);

    public void onUrlShorteningFailed(String sourceUrl, UrlShortenerException exception);
}
