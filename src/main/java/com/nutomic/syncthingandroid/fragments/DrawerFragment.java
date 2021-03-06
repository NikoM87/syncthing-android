package com.nutomic.syncthingandroid.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.activities.MainActivity;
import com.nutomic.syncthingandroid.activities.SettingsActivity;
import com.nutomic.syncthingandroid.activities.WebGuiActivity;
import com.nutomic.syncthingandroid.syncthing.RestApi;
import com.nutomic.syncthingandroid.syncthing.SyncthingService;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_VIEW;

/**
 * Displays information about the local device.
 */
public class DrawerFragment extends Fragment implements RestApi.OnReceiveSystemInfoListener,
        RestApi.OnReceiveConnectionsListener, View.OnClickListener {

    private TextView mDeviceId;

    private TextView mCpuUsage;

    private TextView mRamUsage;

    private TextView mDownload;

    private TextView mUpload;

    private TextView mAnnounceServer;

    private TextView mExitButton;

    private Timer mTimer;

    private MainActivity mActivity;

    public void onDrawerOpened() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateGui();
            }

        }, 0, SyncthingService.GUI_UPDATE_INTERVAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateExitButtonVisibility();
    }

    public void onDrawerClosed() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDrawerClosed();
    }

    /**
     * Populates views and menu.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mDeviceId       = (TextView) view.findViewById(R.id.device_id);
        mCpuUsage       = (TextView) view.findViewById(R.id.cpu_usage);
        mRamUsage       = (TextView) view.findViewById(R.id.ram_usage);
        mDownload       = (TextView) view.findViewById(R.id.download);
        mUpload         = (TextView) view.findViewById(R.id.upload);
        mAnnounceServer = (TextView) view.findViewById(R.id.announce_server);
        mExitButton     = (TextView) view.findViewById(R.id.drawerActionExit);

        view.findViewById(R.id.deviceIdContainer)
                .setOnClickListener(this);
        view.findViewById(R.id.drawerActionWebGui)
                .setOnClickListener(this);
        view.findViewById(R.id.drawerActionDonate)
                .setOnClickListener(this);
        view.findViewById(R.id.drawerActionSettings)
                .setOnClickListener(this);
        mExitButton.setOnClickListener(this);

        updateExitButtonVisibility();
    }

    private void updateExitButtonVisibility() {
        boolean alwaysInBackground = SyncthingService.alwaysRunInBackground(getActivity());
        mExitButton.setVisibility(alwaysInBackground ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();

        if (savedInstanceState != null && savedInstanceState.getBoolean("active")) {
            onDrawerOpened();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("active", mTimer != null);
    }

    /**
     * Invokes status callbacks.
     */
    private void updateGui() {
        if (mActivity.getApi() == null || getActivity().isFinishing())
            return;
        mActivity.getApi().getSystemInfo(this);
        mActivity.getApi().getConnections(this);
    }

    /**
     * This will not do anything if gui updates are already scheduled.
     */
    public void requestGuiUpdate() {
        if (mTimer == null) {
            updateGui();
        }
    }

    /**
     * Populates views with status received via {@link RestApi#getSystemInfo}.
     */
    @Override
    public void onReceiveSystemInfo(RestApi.SystemInfo info) {
        if (getActivity() == null)
            return;

        mDeviceId.setText(info.myID);
        mDeviceId.setVisibility(View.VISIBLE);
        mDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.getApi().copyDeviceId(mDeviceId.getText().toString());
            }
        });
        mCpuUsage.setText(new DecimalFormat("0.00").format(info.cpuPercent) + "%");
        mRamUsage.setText(RestApi.readableFileSize(mActivity, info.sys));
        mAnnounceServer.setText(Integer.toString(info.extAnnounceConnected) + "/" +
                Integer.toString(info.extAnnounceTotal));
        if (info.extAnnounceConnected > 0) {
            mAnnounceServer.setTextColor(getResources().getColor(R.color.text_green));
        } else {
            mAnnounceServer.setTextColor(getResources().getColor(R.color.text_red));
        }
    }

    /**
     * Populates views with status received via {@link RestApi#getConnections}.
     */
    @Override
    public void onReceiveConnections(Map<String, RestApi.Connection> connections) {
        RestApi.Connection c = connections.get(RestApi.TOTAL_STATS);
        mDownload.setText(RestApi.readableTransferRate(mActivity, c.inBits));
        mUpload.setText(RestApi.readableTransferRate(mActivity, c.outBits));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.deviceIdContainer:
                RestApi.shareDeviceId(getActivity(), mDeviceId.getText().toString());
                return;
            case R.id.drawerActionWebGui:
                startActivity(new Intent(mActivity, WebGuiActivity.class));
                mActivity.closeDrawer();
                return;
            case R.id.drawerActionDonate:
                startActivity(new Intent(ACTION_VIEW, Uri.parse(
                        getString(R.string.donate_url))));
                mActivity.closeDrawer();
                return;
            case R.id.drawerActionSettings:
                startActivity(new Intent(mActivity, SettingsActivity.class)
                        .setAction(SettingsActivity.ACTION_APP_SETTINGS_FRAGMENT));
                mActivity.closeDrawer();
                return;
            case R.id.drawerActionExit:
                mActivity.stopService(new Intent(mActivity, SyncthingService.class));
                mActivity.finish();
                mActivity.closeDrawer();
                return;
        }
    }
}
