package com.example.pluginbluetooth.bluetooth.device.scanner;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.pluginbluetooth.bluetooth.gatt.DeviceScanner;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.bluetooth.gatt.ScanListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import qrom.component.log.QRomLog;


public class WatchScanner implements ScanListener {

    private static final String TAG = WatchScanner.class.getSimpleName();

    private static final int SCAN_MINIMUM_DURATION_MS = 11000;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final Set<GattDevice> mDevices = new CopyOnWriteArraySet<GattDevice>();
    private final Set<WatchScannerListener> mListeners = new CopyOnWriteArraySet<WatchScannerListener>();

    private DeviceScanner mScanner;
    private boolean mMinimumScanDurationElapsed;
    private boolean mFoundOneDeviceWhenScanning;

    private final Runnable mMinimumScanRunnable = new Runnable() {
        @Override
        public void run() {
            onMinimumScanDurationElapsed();
        }
    };

    public WatchScanner(final Context context) {
        mContext = context;

        init();
    }

    public void registerListener(final WatchScannerListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(final WatchScannerListener listener) {
        mListeners.remove(listener);
    }

    public boolean hasOneDeviceBeenFound() {
        return mFoundOneDeviceWhenScanning;
    }

    public void startScan(final int time) {
        init();

        mHandler.postDelayed(mMinimumScanRunnable, time);

        mScanner = new DeviceScanner(mContext, this);
        mScanner.start();
    }

    public void startScan() {
        startScan(SCAN_MINIMUM_DURATION_MS);
    }

    public void stopScan() {
        mScanner.stop();
        mHandler.removeCallbacks(mMinimumScanRunnable);
    }

    public boolean isDeviceFound(final String address) {
        if (address != null) {
            for (final GattDevice device : mDevices) {
                if (device.getAddress().equals(address)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onScanResult(final GattDevice device) {
        QRomLog.i("kaelpu_ble","[onScanResult] form WatchScanner = " + device.getAddress());

        mDevices.add(device);

        handleScanResults();
    }

    private void init() {
        mMinimumScanDurationElapsed = false;
        mFoundOneDeviceWhenScanning = false;

        mHandler.removeCallbacks(mMinimumScanRunnable);
        mDevices.clear();
    }

    private void onMinimumScanDurationElapsed() {
        mMinimumScanDurationElapsed = true;

        handleScanResults();
    }

    private void handleScanResults() {
        if (mDevices.size() == 1 && !mFoundOneDeviceWhenScanning) {
            onScanFirstWatchFound();

            mFoundOneDeviceWhenScanning = true;
        }

        if (mMinimumScanDurationElapsed) {
            final GattDevice device = selectGattDevice();

            onScanFinished(device);
        }
    }

    private GattDevice selectGattDevice() {
        if (mDevices.size() == 0) {
            return null;
        }

        final List<GattDevice> devices = new ArrayList<GattDevice>(mDevices);

        Collections.sort(devices, new Comparator<GattDevice>() {
            @Override
            public int compare(final GattDevice lhs, final GattDevice rhs) {
                return Integer.compare(rhs.getRssi(), lhs.getRssi());
            }
        });

        for (GattDevice gattDevice : devices) {
            QRomLog.d(TAG, "Found device: " + gattDevice.getAddress() + " " + gattDevice.getRssi());
        }

        if (mDevices.isEmpty()) {
            throw new RuntimeException("No devices! This should never happen.");
        }
        return devices.get(0);
    }

    private void onScanFirstWatchFound() {
        for (final WatchScannerListener listener : mListeners) {
            listener.onScanFirstWatchFound();
        }
    }

    private void onScanFinished(final GattDevice device) {
        if(device != null ){
            QRomLog.i("kaelpu_ble","[onScanFinished] = " +device );
        }
        for (final WatchScannerListener listener : mListeners) {
            listener.onScanFinished(device);
        }
    }

    public interface WatchScannerListener {
        void onScanFirstWatchFound();
        void onScanFinished(final GattDevice device);
    }
}