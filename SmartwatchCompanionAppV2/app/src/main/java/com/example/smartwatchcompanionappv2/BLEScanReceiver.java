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

/**
 * BroadcastReceiver for handling Bluetooth Low Energy (BLE) scan-related events and
 * Bluetooth adapter state changes.
 * It primarily listens for classic Bluetooth device discovery (ACTION_FOUND) and
 * changes in the Bluetooth adapter's state (ACTION_STATE_CHANGED).
 * It also includes a callback for BLE scan results, which is used to populate a list of
 * discovered LE devices and update the UI in MainActivity.
 */
public class BLEScanReceiver extends BroadcastReceiver {
    // Tag for logging
    private static final String TAG = BLEScanReceiver.class.getSimpleName();
    // BluetoothAdapter instance for interacting with Bluetooth hardware
    private BluetoothAdapter mBluetoothAdapter;
    // List to store discovered Bluetooth LE devices. Accessible statically for other components.
    public static ArrayList<BluetoothDevice> mLeDevices = new ArrayList<>();

    /**
     * Default constructor.
     * Required for the Android system to instantiate the receiver when an intent is received.
     */
    public BLEScanReceiver() {
        // Default constructor
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     * It handles Bluetooth device discovery and Bluetooth adapter state changes.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @SuppressLint("MissingPermission") // Permissions are checked before scan/discovery operations
    @SuppressWarnings("deprecation")   // For usage of deprecated BluetoothAdapter.stopLeScan
    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize BluetoothAdapter if it's not already initialized.
        // This is crucial as the receiver might be instantiated multiple times or mBluetoothAdapter might not be set.
        if (mBluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        }

        // If BluetoothAdapter is still null (e.g., device doesn't support Bluetooth), log an error and return.
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter could not be initialized in onReceive.");
            return;
        }

        final String action = intent.getAction();
        Log.d(TAG, "onReceive: Action: " + action);

        // Handle classic Bluetooth device discovery (ACTION_FOUND).
        // Note: This receiver is named BLEScanReceiver, but it also handles classic discovery.
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                String deviceName;
                String deviceAddress;
                // Attempt to get device name, handling potential SecurityException if BLUETOOTH_CONNECT is missing.
                try {
                    deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = "Unknown Device";
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on device.getName() in ACTION_FOUND: " + e.getMessage());
                    deviceName = "Permission Denied for Name"; // Indicate missing permission
                }
                // Attempt to get device address
                try {
                    deviceAddress = device.getAddress();
                    if (deviceAddress == null || deviceAddress.isEmpty()) {
                        deviceAddress = "Unknown Address";
                    }
                } catch (SecurityException e) {
                     Log.e(TAG, "SecurityException on device.getAddress() in ACTION_FOUND: " + e.getMessage());
                     deviceAddress = "Permission Denied for Address"; // Should not happen for getAddress
                }
                Log.d(TAG, "Classic Bluetooth device found: " + deviceName + " (" + deviceAddress + ")");
            }
        // Handle changes in Bluetooth adapter state (e.g., turned on/off).
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "Bluetooth off");
                    // If Bluetooth is turned off, attempt to stop any ongoing LE scan.
                    if (mBluetoothAdapter != null) {
                        // Check for BLUETOOTH_SCAN permission before stopping LE scan.
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                Log.d(TAG, "Attempting to stop LE scan due to Bluetooth turning off.");
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } catch (SecurityException e) {
                                // This catch is defensive; permission check should prevent this.
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
                    // Potentially start a scan here if required by app logic when Bluetooth turns on.
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "Turning Bluetooth on...");
                    break;
            }
        }
    }


    /**
     * Callback for Bluetooth LE scan results.
     * This callback is invoked when an LE device is found during a scan.
     * It checks if the device is already in the list {@link #mLeDevices},
     * adds it if new, and then attempts to update the UI in {@link MainActivity}.
     */
    @SuppressLint("MissingPermission") // Permissions are checked before scan operations that use this callback
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> {
                if (device != null) {
                    // Check if the discovered device is already in our list to avoid duplicates.
                    boolean found = false;
                    for (BluetoothDevice d : mLeDevices) {
                        if (d.getAddress().equals(device.getAddress())) {
                            found = true;
                            break;
                        }
                    }
                    // If the device is new, add it to the list and update the UI.
                    if (!found) {
                        mLeDevices.add(device);
                        String deviceName = null;
                        // Attempt to get device name. Requires BLUETOOTH_CONNECT permission.
                        // Also checks if MainActivity reference is available to get context.
                        Context mainActivityContext = MainActivity.reference != null ? MainActivity.reference.getApplicationContext() : null;
                        if (mainActivityContext != null && ContextCompat.checkSelfPermission(mainActivityContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            try {
                               deviceName = device.getName();
                            } catch (SecurityException e) {
                               // This catch is defensive; permission check should prevent this.
                               Log.e(TAG, "SecurityException on device.getName() in callback", e);
                            }
                        } else {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for getting device name or context is null in callback.");
                        }
                        Log.i(TAG, "Added LE device: " + (deviceName != null ? deviceName : "Unknown") + " with address " + device.getAddress());

                        // If MainActivity reference is available, update its device list on the UI thread.
                        if (MainActivity.reference != null) {
                            MainActivity.reference.runOnUiThread(() -> {
                                if (MainActivity.reference != null) { // Double-check reference
                                    MainActivity.reference.updateDeviceList(device);
                                }
                            });
                        }
                    }
                }
            };

}
