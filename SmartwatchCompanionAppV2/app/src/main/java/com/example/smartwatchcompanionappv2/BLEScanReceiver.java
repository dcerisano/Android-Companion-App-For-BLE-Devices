package com.example.smartwatchcompanionappv2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
// Reverted: import android.bluetooth.le.ScanCallback; // Not used with deprecated startLeScan
// Reverted: import android.bluetooth.le.ScanResult; // Not used with deprecated startLeScan
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class BLEScanReceiver extends BroadcastReceiver {
    private static final String TAG = BLEScanReceiver.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    public static ArrayList<BluetoothDevice> mLeDevices = new ArrayList<>();
    public static BLEScanReceiver reference;

    // Constructor to initialize adapter and handler
    public BLEScanReceiver(BluetoothAdapter adapter, Handler handler) {
        this.mBluetoothAdapter = adapter;
        this.mHandler = handler;
        reference = this;
    }

    // Default constructor for manifest registration, if needed
    public BLEScanReceiver() {
        reference = this;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: Action: " + action);

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                // Classic Bluetooth discovery, not LE.
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "Bluetooth off");
                    if (mScanning && mBluetoothAdapter != null) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } catch (SecurityException e) {
                                Log.e(TAG, "SecurityException on stopLeScan (STATE_OFF)", e);
                            }
                        } else {
                             Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot stop LE scan (STATE_OFF).");
                        }
                    }
                    mScanning = false;
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "Turning Bluetooth off...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "Bluetooth on");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "Turning Bluetooth on...");
                    break;
            }
        }
    }

    // Method to start/stop LE device scanning
    public void scanLeDevice(final boolean enable, Context context) {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not initialized, cannot scan.");
            return;
        }
        if (mHandler == null) {
            Log.e(TAG, "Handler not initialized, cannot scan.");
            return;
        }

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } catch (SecurityException e) {
                                Log.e(TAG, "SecurityException on stopLeScan (timeout)", e);
                            }
                        } else {
                            Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot stop LE scan (timeout).");
                        }
                        if (MainActivity.reference != null) MainActivity.updateScanStatus();
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    mBluetoothAdapter.startLeScan(mLeScanCallback); // This is the permission-sensitive call
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on startLeScan", e);
                    mScanning = false;
                }
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot start LE scan.");
                mScanning = false;
            }
        } else { // enable == false
            mScanning = false;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (mBluetoothAdapter != null) {
                       mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on stopLeScan (manual stop)", e);
                }
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot stop LE scan (manual stop).");
            }
        }
        if (MainActivity.reference != null) MainActivity.updateScanStatus();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device != null) {
                        boolean found = false;
                        for (BluetoothDevice d : mLeDevices) {
                            if (d.getAddress().equals(device.getAddress())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            mLeDevices.add(device);
                            String deviceName = null;
                            // Getting device name requires BLUETOOTH_CONNECT on API 31+
                            // Assuming MainActivity.reference provides a valid context for this check
                            Context mainActivityContext = MainActivity.reference != null ? MainActivity.reference.getApplicationContext() : null; 
                            if (mainActivityContext != null && ContextCompat.checkSelfPermission(mainActivityContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                   deviceName = device.getName();
                                } catch (SecurityException e) {
                                   Log.e(TAG, "SecurityException on device.getName()", e);
                                }
                            } else {
                                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for getting device name or context is null.");
                            }
                            Log.i(TAG, "Added LE device: " + (deviceName != null ? deviceName : "Unknown") + " with address " + device.getAddress());
                            if (MainActivity.reference != null) {
                                MainActivity.reference.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.updateDeviceList(device);
                                    }
                                });
                            }
                        }
                    }
                }
            };

    public boolean isScanning() {
        return mScanning;
    }

    public static BLEScanReceiver getReference() {
        return reference;
    }
}
