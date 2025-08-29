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
 * BLEScanReceiver is a BroadcastReceiver that handles Bluetooth Low Energy (BLE)
 * scan results and Bluetooth adapter state changes. It populates a list of
 * discovered BLE devices and can interact with MainActivity to update the UI.
 */
public class BLEScanReceiver extends BroadcastReceiver {
    // Tag for logging, using the class's simple name for easy identification
    private static final String TAG = BLEScanReceiver.class.getSimpleName();
    // BluetoothAdapter for managing Bluetooth operations like scanning
    private BluetoothAdapter mBluetoothAdapter;
    // List to store discovered Bluetooth LE devices to avoid duplicates and manage connections
    public static ArrayList<BluetoothDevice> mLeDevices = new ArrayList<>();

    /**
     * Default constructor for BLEScanReceiver.
     * Required for the Android system to instantiate the receiver when a broadcast is received.
     */
    public BLEScanReceiver() {
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     * It handles Bluetooth device discovery (ACTION_FOUND for classic devices, though this
     * receiver focuses on BLE) and Bluetooth adapter state changes (ACTION_STATE_CHANGED).
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received, containing the action and any relevant extras.
     */
    @SuppressLint("MissingPermission") // Permissions are checked before BLE operations
    @SuppressWarnings("deprecation") // Suppresses warnings for deprecated Bluetooth APIs like stopLeScan
    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize BluetoothAdapter if it hasn't been already.
        // This ensures the adapter is available for use within onReceive.
        if (mBluetoothAdapter == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        }

        // If BluetoothAdapter is still null (e.g., Bluetooth not supported or service not available),
        // log an error and return, as no Bluetooth operations can be performed.
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter could not be initialized in onReceive.");
            return;
        }

        final String action = intent.getAction();
        Log.d(TAG, "onReceive: Action: " + action);

        // Handle classic Bluetooth device discovery (ACTION_FOUND).
        // Although this receiver is primarily for BLE, ACTION_FOUND might still be received.
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                String deviceName;
                String deviceAddress;
                // Get device name, handling potential SecurityException if BLUETOOTH_CONNECT permission is missing.
                try {
                    deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = "Unknown Device"; // Default if name is not available
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on device.getName() in ACTION_FOUND: " + e.getMessage());
                    deviceName = "Permission Denied for Name"; // Placeholder if permission denied
                }
                // Get device address, handling potential SecurityException.
                try {
                    deviceAddress = device.getAddress();
                    if (deviceAddress == null || deviceAddress.isEmpty()) {
                        deviceAddress = "Unknown Address"; // Default if address is not available
                    }
                } catch (SecurityException e) {
                     Log.e(TAG, "SecurityException on device.getAddress() in ACTION_FOUND: " + e.getMessage());
                     deviceAddress = "Permission Denied for Address"; // Placeholder if permission denied
                }
                Log.d(TAG, "Classic Bluetooth device found: " + deviceName + " (" + deviceAddress + ")");
            }
        // Handle Bluetooth adapter state changes (ACTION_STATE_CHANGED).
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "Bluetooth off");
                    // If Bluetooth is turned off, attempt to stop any ongoing LE scan.
                    if (mBluetoothAdapter != null) {
                        // Check for BLUETOOTH_SCAN permission before stopping scan.
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                Log.d(TAG, "Attempting to stop LE scan due to Bluetooth turning off.");
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } catch (SecurityException e) {
                                // This catch block might be redundant if checkSelfPermission is effective,
                                // but kept for robustness.
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
                    // Potentially start scan here if needed when Bluetooth turns on.
                    // For now, scan is typically initiated elsewhere (e.g., MainActivity).
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "Turning Bluetooth on...");
                    break;
            }
        }
    }

    /**
     * Callback for Bluetooth LE scan results.
     * This callback is triggered when an LE device is found during a scan
     * initiated by {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)}.
     * It adds the discovered device to the {@code mLeDevices} list if it's not already present
     * and attempts to update the UI in MainActivity if a reference to it is available.
     */
    @SuppressLint("MissingPermission") // Permissions are checked within the callback where device.getName() is called
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> {
                if (device != null) {
                    boolean found = false;
                    // Check if the device is already in the list to avoid duplicates.
                    for (BluetoothDevice d : mLeDevices) {
                        if (d.getAddress().equals(device.getAddress())) {
                            found = true;
                            break;
                        }
                    }
                    // If device is not found in the list, add it.
                    if (!found) {
                        mLeDevices.add(device);
                        String deviceName = null;
                        // Attempt to get device name if BLUETOOTH_CONNECT permission is granted.
                        // This requires context; attempting to get it from MainActivity.reference.
                        Context mainActivityContext = MainActivity.reference != null ? MainActivity.reference.getApplicationContext() : null;
                        if (mainActivityContext != null && ContextCompat.checkSelfPermission(mainActivityContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            try {
                               deviceName = device.getName(); // This call requires BLUETOOTH_CONNECT
                            } catch (SecurityException e) {
                               // Log security exception if getName fails despite permission check (e.g., if context or permission state changed).
                               Log.e(TAG, "SecurityException on device.getName() in callback", e);
                            }
                        } else {
                            // Log warning if permission is not granted or context is null, as device name cannot be retrieved.
                            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for getting device name or context is null in callback.");
                        }
                        Log.i(TAG, "Added LE device: " + (deviceName != null ? deviceName : "Unknown") + " with address " + device.getAddress());

                        // If MainActivity reference is available, update its device list on the UI thread.
                        // This allows the UI to reflect newly discovered devices.
                        if (MainActivity.reference != null) {
                            MainActivity.reference.runOnUiThread(() -> {
                                if (MainActivity.reference != null) { // Double-check reference as it can be nullified
                                    MainActivity.reference.updateDeviceList(device);
                                }
                            });
                        }
                    }
                }
            };
}
