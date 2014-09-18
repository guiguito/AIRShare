package com.ggt.airshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.ipaulpro.afilechooser.utils.FileUtils;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Launcher activity of the app.
 * <p/>
 * Small help and possibility to pick a file.
 *
 * @author guiguito
 */
public class LauncherActivity extends MotherActivity {

    public static final String KEY_NO_BUTTON = "KEY_NO_BUTTON";

    @InjectView(R.id.mPickAFileButton)
    Button mPickAFileButton;

    private static final int REQUEST_CHOOSER = 1234;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_launcher);

        if (getIntent().hasExtra(KEY_NO_BUTTON) && getIntent().getBooleanExtra(KEY_NO_BUTTON, false)) {
            mPickAFileButton.setVisibility(View.GONE);
        }
        mShowHelpMenuItem = false;
    }

    @OnClick(R.id.mPickAFileButton)
    public void onPickAFileClicked() {
        // Create the ACTION_GET_CONTENT Intent
        Intent getContentIntent = FileUtils.createGetContentIntent();
        Intent intent = Intent.createChooser(getContentIntent,
                getString(R.string.pick_a_file));
        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHOOSER:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent(this, ShairingActivity.class);
                    Uri uri = data.getData();
                    intent.setAction(Intent.ACTION_SEND);
                    String mimeType = FileUtils.getMimeType(this, uri);
                    //TODO add google drive and one drive support
                    if (mimeType != null) {
                        intent.setType(mimeType);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(intent);
                    } else {
                        //EasyTracker.getTracker(this).sendEvent("Event", "data not supported", uri.toString(), null);
                        Toast.makeText(this, getString(R.string.we_cant_share_this), Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
}
