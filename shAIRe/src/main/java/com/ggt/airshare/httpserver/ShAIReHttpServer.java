package com.ggt.airshare.httpserver;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.ggt.airshare.R;
import com.ggt.airshare.urlshortener.UrlShortener;
import com.ggt.airshare.urlshortener.UrlShortenerException;
import com.ggt.airshare.urlshortener.UrlShortenerListener;
import com.ggt.airshare.utils.ContactsUtils;
import com.ggt.airshare.utils.HTMLUtils;
import com.ggt.airshare.utils.NetworkUtils;
import com.ggt.airshare.utils.ShAIReConstants;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

/**
 * Sharing nano server .
 *
 * @author gduche
 */
public class ShAIReHttpServer extends NanoHTTPD implements UrlShortenerListener {

    private String mFilePath;
    private String mMimeType;
    private String mTextToShare;
    private Context mContext;
    private String mRealUrl;
    private String mRandomString;
    private QRCodeEncoder mQrCodeEncoder;

    private ShAIReHttpServerListener mShAIReHttpServerListener;

    private static final String GET_STRING = "GET";
    private static final String URL_BEGIN = "http://";

    public ShAIReHttpServer(Context context,
                            ShAIReHttpServerListener shAIReHttpServerListener, Intent intent)
            throws IOException {
        super(ShAIReConstants.SERVER_PORT, null);
        mContext = context.getApplicationContext();
        mShAIReHttpServerListener = shAIReHttpServerListener;
        share(intent, context);
    }

    /**
     * Share the data from the intent.
     *
     * @param intent
     */
    private void share(Intent intent, final Context context) {
        // try to start server
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            mMimeType = type;
            if (type.startsWith("text/x-vcard")) {
                handleSendContactText(intent); // Handle text being sent
            } else if (type.startsWith("text/")) {
                handleSendRawText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendFile(intent); // Handle single image being sent
            } else if (type.startsWith("audio/")) {
                handleSendFile(intent); // Handle single audio being sent
            } else if (type.startsWith("video/")) {
                handleSendFile(intent); // Handle single video being sent
            } else if (type.startsWith("application/")) {
                handleSendFile(intent); // Handle single video being sent
            }
            NetworkUtils.WifiDetails wifiDetails = NetworkUtils
                    .getWifiDetails(mContext);
            if (wifiDetails != null) {
                // detect wifi network name
                notifyOnServerWifiNetworkNameDetected(wifiDetails.ssid);
                // security string
                mRandomString = ("" + Math.abs(new Random().nextInt())).substring(0, 4);
                mRealUrl = URL_BEGIN + wifiDetails.ipAddress + ":"
                        + ShAIReConstants.SERVER_PORT + "/" + mRandomString;
                // generate shorten url
                UrlShortener.getInstance(mContext).shortenUrlonTinyUrl(mRealUrl,
                        this);

                // generate QR code
                try {
                    Intent intentText = new Intent();
                    intentText.setAction(Intents.Encode.ACTION);
                    intentText.putExtra(Intents.Encode.FORMAT,
                            BarcodeFormat.QR_CODE.toString());
                    intentText
                            .putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
                    intentText.putExtra(Intents.Encode.DATA, mRealUrl);
                    mQrCodeEncoder = new QRCodeEncoder(mContext, intentText,
                            500, false);
                    Bitmap qrCode = mQrCodeEncoder.encodeAsBitmap();
                    notifyServerUrlQRCodeGenerated(qrCode);
                } catch (WriterException e) {
                    notifyOnServerUrlQRCodeGeneratioFailed();
                }
            } else {
                notifyError(mContext
                        .getString(R.string.please_make_sure_your_wifi_is_actvivated));
            }
        } else {
            notifyError(mContext.getString(R.string.you_cant_share_this));
        }
    }

    private void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    private void setTextToShare(String textToShare) {
        mTextToShare = textToShare;
    }

    @Override
    public void onUrlShortened(String sourceUrl, String urlShortened) {
        notifyOnServerUrlShortened(sourceUrl, urlShortened);
    }

    @Override
    public void onUrlShorteningFailed(String sourceUrl,
                                      UrlShortenerException exception) {
        // display original url
        notifyOnServerUrlShortened(sourceUrl, sourceUrl);
    }

    private void handleSendRawText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            setTextToShare(sharedText);
        } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String filePath = FileUtils.getPath(mContext, imageUri);
            if (filePath != null && filePath.startsWith("http")) {
                setTextToShare(filePath);
            } else {
                handleSendFile(filePath);
            }
        }
    }

    private void handleSendFile(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            String filePath = FileUtils.getPath(mContext, imageUri);
            handleSendFile(filePath);
        }
    }

    private void handleSendFile(String filePath) {
        notifyOnServerFileNameSharedDetected(filePath.substring(filePath
                .lastIndexOf('/') + 1));
        setFilePath(filePath);
    }

    private void handleSendContactText(Intent intent) {
        Uri contactUri = intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
        if (contactUri != null) {
            AddressBookParsedResult addressBookParsedResult = ContactsUtils
                    .getContactDetailsFromUri(mContext, contactUri);
            if (addressBookParsedResult != null) {
                setTextToShare(addressBookParsedResult.toString());
            }
        }
    }

    private void notifyError(String message) {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener.onServerError(message);
        }
    }

    private void notifyServerUrlQRCodeGenerated(Bitmap qrCode) {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener.onServerUrlQRCodeGenerated(qrCode);

        }
    }

    private void notifyOnServerUrlQRCodeGeneratioFailed() {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener.onServerUrlQRCodeGenerationFailed();

        }
    }

    private void notifyOnServerUrlShortened(String sourceUrl, String shortenedUrl) {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener.onServerUrlShortened(sourceUrl, shortenedUrl);
        }
    }

    private void notifyOnServerWifiNetworkNameDetected(String wifiNetworkName) {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener
                    .onServerWifiNetworkNameDetected(wifiNetworkName);
        }
    }

    private void notifyOnServerFileNameSharedDetected(String filename) {
        if (mShAIReHttpServerListener != null) {
            mShAIReHttpServerListener.onServerFileNameSharedDetected(filename);
        }
    }

    /**
     * pure server methods *
     */

    @Override
    public Response serve(String uri, String method, Properties header,
                          Properties parms, Properties files) {
        if (uri != null && method != null && method.equals(GET_STRING)) {
            // if security is activated check it.
            if (mRandomString != null && !TextUtils.isEmpty(mRandomString)
                    && !uri.endsWith(mRandomString)) {
                return buildHtmlResponse(mContext
                        .getString(R.string.html_wrong_security_text));
            }
            if (mFilePath != null) {
                // file sharing
                return super.serveFile(
                        mFilePath.substring(mFilePath.lastIndexOf('/') + 1),
                        header,
                        new File(mFilePath.substring(0,
                                mFilePath.lastIndexOf('/') + 1)), false);
            } else if (mTextToShare != null && !TextUtils.isEmpty(mTextToShare)) {
                // Text sharing
                if (HTMLUtils.isUrl(mTextToShare)) {
                    // string is a url perform redirection
                    return buildUrlRedirection(mTextToShare);
                } else {
                    // the text is something else. Transform it to html and
                    // handle links.
                    return buildHtmlResponse(mTextToShare);
                }
            } else {
                // nothing to share
                return buildHtmlResponse(mContext
                        .getString(R.string.html_nothing_to_share));
            }
        } else {
            return buildHtmlResponse(mContext
                    .getString(R.string.html_wrong_request));
        }
    }

    private NanoHTTPD.Response buildHtmlResponse(String text) {
        StringBuilder responsePage = new StringBuilder();
        responsePage.append(mContext.getString(R.string.html_header));
        responsePage.append(HTMLUtils.convertStringToHTML(text));
        responsePage.append(mContext.getString(R.string.html_footer));
        return new NanoHTTPD.Response(HTTP_OK, MIME_HTML,
                responsePage.toString());
    }

    private NanoHTTPD.Response buildUrlRedirection(String url) {
        Response res = new Response(HTTP_REDIRECT, MIME_HTML, mTextToShare);
        res.addHeader("Location", url);
        return res;
    }

    public String getRealUrl() {
        return mRealUrl;
    }
}
