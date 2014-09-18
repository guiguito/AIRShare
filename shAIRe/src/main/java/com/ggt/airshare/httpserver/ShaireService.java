package com.ggt.airshare.httpserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.ggt.airshare.R;
import com.ggt.airshare.ShairingActivity;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Shaire service.
 *
 * @author guiguito
 */
public class ShaireService extends Service implements ShAIReHttpServerListener {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private ShAIReHttpServer mShAIReHttpServer;
    private Bitmap mSharingQRCode;
    private String mDirectUrl;
    private String mShortenedUrl;
    private String mServerErrorMessage;
    private String mWifiName;
    private String mFileName;
    private static final int ONGOING_NOTIFICATION_ID = 1;
    public static final String SHAIRE_ACTION_STOP = "SHAIRE_ACTION_STOP";

    private ArrayList<ShaireServiceListener> mListeners = new ArrayList<ShaireServiceListener>();

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ShaireService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return ShaireService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null
                && SHAIRE_ACTION_STOP.equals(intent.getAction())) {
            // coming from a notification
            stopSharing();
        } else {
            // starting service for sharing
            if (mShAIReHttpServer != null) {
                stopSharing();
            }
            startSharing(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startSharing(Intent intent) {
        try {
            mShAIReHttpServer = new ShAIReHttpServer(this, this, intent);

            Intent openShaireIntent = new Intent(this, ShairingActivity.class);
            PendingIntent openShairePendingIntent = PendingIntent.getActivity(
                    this, 0, openShaireIntent, 0);

            Intent stopShaireIntent = new Intent(this, ShaireService.class);
            stopShaireIntent.setAction(SHAIRE_ACTION_STOP);
            PendingIntent stopShairePendingIntent = PendingIntent.getService(
                    this, 0, stopShaireIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getText(R.string.notification_title))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentText(
                            String.format(getText(R.string.notification_message)
                                    .toString()))
                    .addAction(android.R.drawable.btn_minus,
                            getText(R.string.notification_stop),
                            stopShairePendingIntent);
            mBuilder.setContentIntent(openShairePendingIntent);
            startForeground(ONGOING_NOTIFICATION_ID, mBuilder.build());
            notifyOnSharingStarted();
        } catch (IOException e) {
            mShAIReHttpServer = null;
            notifyOnSharingError(getString(R.string.you_cant_share_this));
        }
    }

	/* public interface */

    public void addListener(ShaireServiceListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ShaireServiceListener listener) {
        mListeners.remove(listener);
    }

    public void stopSharing() {
        stopForeground(true);
        if (mShAIReHttpServer != null) {
            mShAIReHttpServer.stop();
        }
        if (mSharingQRCode != null) {
            mSharingQRCode.recycle();
        }
        mShAIReHttpServer = null;
        mSharingQRCode = null;
        mDirectUrl = null;
        mShortenedUrl = null;
        mServerErrorMessage = null;
        mWifiName = null;
        mFileName = null;
        notifyOnSharingStopped();
    }

    public boolean isSharing() {
        return mShAIReHttpServer != null;
    }

    @SuppressLint("NewApi")
    public void activateNFCSharing(Activity activity) {
        if (mShAIReHttpServer != null && mShAIReHttpServer.getRealUrl() != null) {
            // NFC
            if (android.os.Build.VERSION.SDK_INT > 14) {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
                if (nfcAdapter != null) {
                    nfcAdapter.setNdefPushMessageCallback(
                            new NfcAdapter.CreateNdefMessageCallback() {

                                @Override
                                public NdefMessage createNdefMessage(
                                        NfcEvent event) {
                                    return new NdefMessage(
                                            new NdefRecord[]{NdefRecord.createUri(Uri
                                                    .parse(mShAIReHttpServer
                                                            .getRealUrl()))});
                                }
                            }, activity);
                }
            }
        }
    }

    public Bitmap getSharingQRCode() {
        return mSharingQRCode;
    }

    public String getDirectUrl() {
        return mDirectUrl;
    }

    public String getShortenedUrl() {
        return mShortenedUrl;
    }

    public String getServerErrorMessage() {
        return mServerErrorMessage;
    }

    public String getWifiName() {
        return mWifiName;
    }

    public String getFileName() {
        return mFileName;
    }

    /* server events */
    @Override
    public void onServerWifiNetworkNameDetected(String wifiNetworkName) {
        mWifiName = wifiNetworkName;
        notifyOnSharingUpdated();
    }

    @Override
    public void onServerFileNameSharedDetected(String filename) {
        mFileName = filename;
        notifyOnSharingUpdated();
    }

    @Override
    public void onServerUrlQRCodeGenerated(Bitmap qrCode) {
        if (mSharingQRCode != null) {
            mSharingQRCode.recycle();
        }
        mSharingQRCode = qrCode;
        notifyOnSharingUpdated();
    }

    @Override
    public void onServerUrlQRCodeGenerationFailed() {
        notifyOnSharingUpdated();
        notifyOnSharingError(getString(R.string.cant_generate_qr_code));
    }

    @Override
    public void onServerUrlShortened(String sourceUrl, String shortenedUrl) {
        mDirectUrl = sourceUrl;
        mShortenedUrl = shortenedUrl;
        notifyOnSharingUpdated();
    }

    @Override
    public void onServerError(String message) {
        mServerErrorMessage = message;
        stopSharing();
        notifyOnSharingError(message);
    }

    /* listeners notifications */
    private void notifyOnSharingStarted() {
        for (ShaireServiceListener listener : mListeners) {
            listener.onSharingStarted();
        }
    }

    private void notifyOnSharingUpdated() {
        for (ShaireServiceListener listener : mListeners) {
            listener.onSharingUpdated();
        }
    }

    private void notifyOnSharingStopped() {
        for (ShaireServiceListener listener : mListeners) {
            listener.onSharingStopped();
        }
    }

    private void notifyOnSharingError(String errorMessage) {
        for (ShaireServiceListener listener : mListeners) {
            listener.onSharingError(errorMessage);
        }
    }
}
