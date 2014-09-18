package com.ggt.airshare;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ggt.airshare.httpserver.ShaireService;
import com.ggt.airshare.httpserver.ShaireService.LocalBinder;
import com.google.analytics.tracking.android.EasyTracker;

import butterknife.ButterKnife;
import de.psdev.licensesdialog.LicensesDialogFragment;

/**
 * Mother class of all activities.
 *
 * @author guiguito
 */
public abstract class MotherActivity extends ActionBarActivity {

    private static final String APP_PACKAGE = "com.ggt.airshare";
    boolean mShowHelpMenuItem = true;
    ShaireService mShaireService;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mShaireService = binder.getService();
            mBound = true;
            if (mBound && mResumed) {
                onResumeAndBoundToService();
            }
            onServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    boolean mBound = false;
    boolean mResumed = false;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
        // Bind to LocalService
        Intent intent = new Intent(this, ShaireService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        mResumed = true;
        super.onResume();
        if (mBound && mResumed) {
            onResumeAndBoundToService();
        }
    }

    protected void onResumeAndBoundToService() {
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
        mResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    protected void onServiceBound() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mShowHelpMenuItem) {
            menu.findItem(R.id.action_help).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rate:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + APP_PACKAGE)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id="
                                    + APP_PACKAGE)));
                }
                break;
            case R.id.action_help:
                Intent intent = new Intent(this, LauncherActivity.class);
                intent.putExtra(LauncherActivity.KEY_NO_BUTTON, true);
                startActivity(intent);
                break;
            case R.id.action_about:
                final LicensesDialogFragment fragment = LicensesDialogFragment
                        .newInstance(R.raw.notices, false);
                fragment.show(getSupportFragmentManager(), null);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
