package com.ggt.airshare.urlshortener;

import android.content.Context;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Singleton to shorten URL.
 *
 * @author gduche
 */
public class UrlShortener {

    private static UrlShortener mInstance;

    private static AQuery mAQuery;

    private static final String ERROR_MESSAGE = "Url shortening failed";

    private UrlShortener(Context context) {
        if (mAQuery == null) {
            mAQuery = new AQuery(context);
        }
    }

    public static UrlShortener getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new UrlShortener(context);
        }
        return mInstance;
    }

	/* Google service */

    private static final String URL_GOOGLE_SHORTENER_SERVICE = "https://www.googleapis.com/urlshortener/v1/url";
    private static final String URL_GOOGLE_SHORTENER_SERVICE_API_KEY = "?key=AIzaSyBLgWRPpTgIZcXt6Ya-wbOM_D_kn3NJl4k";
    private static final String LONG_URL_KEY = "longUrl";
    private static final String ID_KEY = "id";

    public void shortenUrlonGoogle(final String sourceUrl,
                                   final UrlShortenerListener googleUrlShortenerListener) {
        JSONObject input = new JSONObject();
        try {
            input.putOpt(LONG_URL_KEY, sourceUrl);
        } catch (JSONException e1) {
            // cant'happen
        }
        // TODO activate key on google api console
        mAQuery.post(URL_GOOGLE_SHORTENER_SERVICE /*
                                                 * +
												 * URL_GOOGLE_SHORTENER_SERVICE_API_KEY
												 */, input, JSONObject.class,
                new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject jsonobject,
                                         AjaxStatus status) {
                        super.callback(url, jsonobject, status);
                        try {
                            if (status.getCode() == HttpsURLConnection.HTTP_OK
                                    && jsonobject != null
                                    && jsonobject.getString(LONG_URL_KEY) != null) {
                                googleUrlShortenerListener.onUrlShortened(
                                        sourceUrl, jsonobject.getString(ID_KEY));
                            } else {
                                googleUrlShortenerListener
                                        .onUrlShorteningFailed(sourceUrl,
                                                new UrlShortenerException(
                                                        ERROR_MESSAGE));
                            }
                        } catch (JSONException e) {
                            googleUrlShortenerListener.onUrlShorteningFailed(
                                    sourceUrl,
                                    new UrlShortenerException(ERROR_MESSAGE
                                            + " : " + e.getMessage()));
                        }
                    }
                });
    }

	/* is.gd service */

    private static final String URL_IS_GD_SHORTENER_SERVICE = "http://is.gd/create.php?format=json&url=%s";
    private static final String SHORT_URL_KEY = "shorturl";

    public void shortenUrlonIsGd(final String sourceUrl,
                                 final UrlShortenerListener googleUrlShortenerListener) {
        mAQuery.ajax(
                String.format(URL_IS_GD_SHORTENER_SERVICE,
                        URLEncoder.encode(sourceUrl)), JSONObject.class,
                new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject jsonobject,
                                         AjaxStatus status) {
                        super.callback(url, jsonobject, status);
                        try {
                            if (status.getCode() == HttpsURLConnection.HTTP_OK
                                    && jsonobject != null
                                    && jsonobject.getString(SHORT_URL_KEY) != null) {
                                googleUrlShortenerListener.onUrlShortened(
                                        sourceUrl,
                                        jsonobject.getString(SHORT_URL_KEY));
                            } else {
                                googleUrlShortenerListener
                                        .onUrlShorteningFailed(sourceUrl,
                                                new UrlShortenerException(
                                                        ERROR_MESSAGE));
                            }
                        } catch (JSONException e) {
                            googleUrlShortenerListener.onUrlShorteningFailed(
                                    sourceUrl,
                                    new UrlShortenerException(ERROR_MESSAGE
                                            + " : " + e.getMessage()));
                        }
                    }
                });
    }

    /* shorten url on tiny url */
    private static final String URL_TINY_URL_SHORTENER_SERVICE = "http://tiny-url.info/api/v1/create";
    private static final String URL_SHORTEN_KEY = "shorturl";

    public void shortenUrlonTinyUrl(final String sourceUrl,
                                  final UrlShortenerListener googleUrlShortenerListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        params.put("apikey", "8206D9G737DD879F69C");
        params.put("provider", "clicky_me");
        params.put("url", sourceUrl);
        mAQuery.ajax(URL_TINY_URL_SHORTENER_SERVICE, params, JSONObject.class,
                new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject jsonobject,
                                         AjaxStatus status) {
                        try {
                            if (status.getCode() == HttpsURLConnection.HTTP_OK
                                    && jsonobject != null
                                    && jsonobject.getString(URL_SHORTEN_KEY) != null) {
                                googleUrlShortenerListener.onUrlShortened(
                                        sourceUrl,
                                        jsonobject.getString(URL_SHORTEN_KEY));
                            } else {
                                googleUrlShortenerListener
                                        .onUrlShorteningFailed(sourceUrl,
                                                new UrlShortenerException(
                                                        ERROR_MESSAGE));
                            }
                        } catch (JSONException e) {
                            googleUrlShortenerListener.onUrlShorteningFailed(
                                    sourceUrl,
                                    new UrlShortenerException(ERROR_MESSAGE
                                            + " : " + e.getMessage()));
                        }
                    }
                });
    }
}
