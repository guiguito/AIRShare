package com.ggt.airshare.httpserver;

/**
 * Interface to implements to listen to ShaireService events.
 * 
 * @author guiguito
 * 
 */
public interface ShaireServiceListener {

	public void onSharingStarted();

	public void onSharingUpdated();

	public void onSharingStopped();

	public void onSharingError(String errorMessage);

}
