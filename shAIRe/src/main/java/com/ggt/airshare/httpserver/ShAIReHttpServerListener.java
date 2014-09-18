package com.ggt.airshare.httpserver;

import android.graphics.Bitmap;

/**
 * Implements this class to listen to ShAIReHttpServer events.
 * 
 * @author guiguito
 */
public interface ShAIReHttpServerListener {

	public void onServerWifiNetworkNameDetected(String wifiNetworkName);

	public void onServerFileNameSharedDetected(String filename);

	public void onServerUrlQRCodeGenerated(Bitmap qrCode);

	public void onServerUrlQRCodeGenerationFailed();

	public void onServerUrlShortened(String sourceUrl, String shortenedUrl);

	public void onServerError(String message);

}
