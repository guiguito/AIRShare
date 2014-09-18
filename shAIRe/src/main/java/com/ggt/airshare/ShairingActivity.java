package com.ggt.airshare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ggt.airshare.httpserver.ShaireService;
import com.ggt.airshare.httpserver.ShaireServiceListener;
import com.ggt.airshare.utils.NetworkUtils;
import com.google.analytics.tracking.android.EasyTracker;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Activity that performs data sharing.
 * <p/>
 * TODO in a next version. handle phone numbers. handle multiple files sharing.
 * TODO Improve notifying/displaying message on download start/end.
 * TODO Add chromecast support on image/audio/video/
 * in a service and notification (cancel in notification).
 *
 * @author gduche
 */
public class ShairingActivity extends MotherActivity implements
        ShaireServiceListener {

    @InjectView(R.id.mUrlShortenedTextView)
    TextView mUrlShortenedTextView;

    @InjectView(R.id.mSharingFileLinearLayout)
    LinearLayout mSharingFileLinearLayout;

    @InjectView(R.id.mSharingFileNameTextView)
    TextView mSharingFileNameTextView;

    @InjectView(R.id.mWifiNetworkNameTextView)
    TextView mWifiNetworkNameTextView;

    @InjectView(R.id.mQrcodeImageView)
    ImageView mQrcodeImageView;

    @InjectView(R.id.mUrlDirectTextView)
    TextView mUrlDirectTextView;

    @InjectView(R.id.mNfcStatusTextView)
    TextView mNfcStatusTextView;

    @InjectView(R.id.mNfcStatusImageView)
    ImageView mNfcStatusImageView;

    boolean mIsNewFileToShare = false;

    boolean mJustCreated = true;

    public static final class StopSharingDialogFragment extends DialogFragment {
        ShairingActivity mMainActivity;

        public void setMainActivity(ShairingActivity mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.do_you_want_to_keep_sharing_active)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    if (mMainActivity != null) {
                                        mMainActivity.finish();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    if (mMainActivity != null
                                            && mMainActivity.mBound) {
                                        mMainActivity.mShaireService
                                                .stopSharing();
                                    }
                                    mMainActivity.finish();
                                }
                            });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static final class NewSharingDialogFragment extends DialogFragment {
        ShairingActivity mMainActivity;

        public void setMainActivity(ShairingActivity mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.already_sharing)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    if (mMainActivity != null
                                            && mMainActivity.mBound) {
                                        mMainActivity.mIsNewFileToShare = true;
                                        mMainActivity.mShaireService
                                                .stopSharing();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dismiss();
                                }
                            });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shairing);
    }

    @Override
    public void onResumeAndBoundToService() {
        super.onResumeAndBoundToService();
        mShaireService.addListener(this);
        if (NetworkUtils.getWifiDetails(getApplicationContext()) == null) {
            // need to activate wifi
            Toast.makeText(
                    this,
                    getString(R.string.please_make_sure_your_wifi_is_actvivated),
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(
                    android.provider.Settings.ACTION_WIRELESS_SETTINGS));
        } else if (Intent.ACTION_SEND.equals(getIntent().getAction())
                && mShaireService.isSharing() && mJustCreated) {
            // handle new share (propose to stop previous one)
            NewSharingDialogFragment newFragment = new NewSharingDialogFragment();
            newFragment.setMainActivity(this);
            newFragment.show(getSupportFragmentManager(), "");
        } else if (Intent.ACTION_SEND.equals(getIntent().getAction()) && mJustCreated) {
            startSharing();
        } else if (getIntent().getBooleanExtra(
                ShaireService.SHAIRE_ACTION_STOP, false)) {
            mShaireService.stopSharing();
            finish();
        }
        mShaireService.activateNFCSharing(this);
        updateGui();
        mJustCreated = false;
    }

    private void startSharing() {
        // Start sharing
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        //EasyTracker.getTracker(this).sendEvent("Event", action, type, null);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Intent intentShaireService = new Intent(this, ShaireService.class);
            intentShaireService.setType(getIntent().getType());
            intentShaireService.setAction(getIntent().getAction());
            intentShaireService.putExtras(getIntent());
            startService(intentShaireService);
        } else {
            Toast.makeText(this, getString(R.string.unknown_file_type), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            mShaireService.removeListener(this);
        }
    }

    @Override
    public void onBackPressed() {
        //TODO
        // show popup to stop sharing.
        if (mBound && mShaireService.isSharing()) {
            StopSharingDialogFragment newFragment = new StopSharingDialogFragment();
            newFragment.setMainActivity(this);
            newFragment.show(getSupportFragmentManager(), "");
        } else {
            super.onBackPressed();
        }
    }

    private void updateGui() {
        // QR code
        if (mShaireService != null && mShaireService.getSharingQRCode() != null) {
            mQrcodeImageView.setVisibility(View.VISIBLE);
            mQrcodeImageView.setImageBitmap(mShaireService.getSharingQRCode());
        } else {
            mQrcodeImageView.setVisibility(View.GONE);
        }
        // display shortened url
        if (mShaireService != null && mShaireService.getShortenedUrl() != null
                && !TextUtils.isEmpty(mShaireService.getShortenedUrl())) {
            mUrlShortenedTextView.setVisibility(View.VISIBLE);
            mUrlShortenedTextView.setText(mShaireService.getShortenedUrl());
        } else {
            mUrlShortenedTextView.setVisibility(View.GONE);
        }
        //direct url
        if (mShaireService != null && mShaireService.getDirectUrl() != null
                && !TextUtils.isEmpty(mShaireService.getDirectUrl())) {
            mUrlDirectTextView.setVisibility(View.VISIBLE);
            mUrlDirectTextView.setText(mShaireService.getDirectUrl());
        } else {
            mUrlDirectTextView.setVisibility(View.GONE);
        }
        // Wifi name
        if (mShaireService != null && mShaireService.getWifiName() != null
                && !TextUtils.isEmpty(mShaireService.getWifiName())) {
            mWifiNetworkNameTextView.setVisibility(View.VISIBLE);
            mWifiNetworkNameTextView.setText(mShaireService.getWifiName());
        } else {
            mWifiNetworkNameTextView.setVisibility(View.GONE);
        }
        // filename of the file shared
        if (mShaireService != null && mShaireService.getFileName() != null
                && !TextUtils.isEmpty(mShaireService.getFileName())) {
            mSharingFileLinearLayout.setVisibility(View.VISIBLE);
            mSharingFileNameTextView.setText(mShaireService.getFileName());
        } else {
            mSharingFileLinearLayout.setVisibility(View.GONE);
        }
        //NfcAdapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            NfcAdapter nfcAdpt = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdpt != null && nfcAdpt.isEnabled()) {
                mNfcStatusTextView.setText(getString(R.string.nfc_enabled));
                mNfcStatusImageView.setImageResource(R.drawable.nfc);
            } else {
                mNfcStatusTextView.setText(getString(R.string.nfc_disabled));
                mNfcStatusImageView.setImageResource(R.drawable.nfc_disabled);
            }
        } else {
            mNfcStatusTextView.setVisibility(View.GONE);
            mNfcStatusImageView.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.mNfcStatusImageView)
    public void onNfClicked() {
        Intent setnfc = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(setnfc);
    }

    @Override
    public void onSharingStarted() {
        updateGui();
        if (mResumed) {
            mShaireService.activateNFCSharing(this);
        }
    }

    @Override
    public void onSharingUpdated() {
        updateGui();
    }

    @Override
    public void onSharingStopped() {
        updateGui();
        if (mIsNewFileToShare) {
            startSharing();
            mIsNewFileToShare = false;
        } else {
            finish();
        }
    }

    @Override
    public void onSharingError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

}
