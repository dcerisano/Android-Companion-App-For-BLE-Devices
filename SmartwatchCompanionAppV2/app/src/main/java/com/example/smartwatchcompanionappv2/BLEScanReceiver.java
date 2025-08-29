package com.example.smartwatchcompanionappv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class BLEScanReceiver extends BroadcastReceiver {
    private static final String TAG = BLEScanReceiver.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    public static ArrayList<BluetoothDevice> mLeDevices = new ArrayList<>();

    public BLEScanReceiver() {
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (mBluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        }

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter could not be initialized in onReceive.");
            return;
        }

        final String action = intent.getAction();
        Log.d(TAG, "onReceive: Action: " + action);

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                String deviceName;
                String deviceAddress;
                try {
                    deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = "Unknown Device";
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on device.getName() in ACTION_FOUND: " + e.getMessage());
                    deviceName = "Permission Denied for Name";
                }
                try {
                    deviceAddress = device.getAddress();
                    if (deviceAddress == null || deviceAddress.isEmpty()) {
                        deviceAddress = "Unknown Address";
                    }
                } catch (SecurityException e) {
                     Log.e(TAG, "SecurityException on device.getAddress() in ACTION_FOUND: " + e.getMessage());
                     deviceAddress = "Permission Denied for Address";
                }
                Log.d(TAG, "Classic Bluetooth device found: " + deviceName + " (" + deviceAddress + ")");
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "Bluetooth off");
                    if (mBluetoothAdapter != null) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                Log.d(TAG, "Attempting to stop LE scan due to Bluetooth turning off.");
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } catch (SecurityException e) {
                                Log.e(TAG, "SecurityException on stopLeScan (STATE_OFF)", e);
                            }
                        } else {
                             Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot stop LE scan (STATE_OFF).");
                        }
                    }
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

    @SuppressLint("MissingPermission")
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> {
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
                        Context mainActivityContext = MainActivity.reference != null ? MainActivity.reference.getApplicationContext() : null;
                        if (mainActivityContext != null && ContextCompat.checkSelfPermission(mainActivityContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            try {
                               deviceName = device.getName();
                            } catch (SecurityException e) {
                               Log.e(TAG, "SecurityException on device.getName() in callback", e);
                            }
                        } else {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for getting device name or context is null in callback.");
                        }
                        Log.i(TAG, "Added LE device: " + (deviceName != null ? deviceName : "Unknown") + " with address " + device.getAddress());

                        if (MainActivity.reference != null) {
                            MainActivity.reference.runOnUiThread(() -> {
                                if (MainActivity.reference != null) {
                                    MainActivity.reference.updateDeviceList(device);
                                }
                            });
                        }
                    }
                }
            };
}
