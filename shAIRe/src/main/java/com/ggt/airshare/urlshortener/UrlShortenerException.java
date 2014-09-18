
package com.ggt.airshare.urlshortener;

/**
 * Exception from google url shortener service.
 * 
 * @author gduche
 */
public class UrlShortenerException extends Exception {

    private static final long serialVersionUID = 6087218793856704661L;

    public UrlShortenerException(String message) {
        super(message);
    }

}
