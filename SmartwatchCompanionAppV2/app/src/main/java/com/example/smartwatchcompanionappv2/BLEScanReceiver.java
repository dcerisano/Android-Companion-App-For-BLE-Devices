package com.example.smartwatchcompanionappv2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
// import android.os.Bundle; // Unused import (Warning:(8, 1))
import android.util.Log;

// import java.util.ArrayList; // Unused import (Warning:(11, 1))

// import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
// import no.nordicsemi.android.support.v18.scanner.ScanRecord;
// import no.nordicsemi.android.support.v18.scanner.ScanResult;

/* Receives broadcasts that indicate a device has been found by the background BLE scan
when a device is found it then starts a foreground service to handle the communication to the BLE
device.  */
public class BLEScanReceiver extends BroadcastReceiver {
    private static final String TAG = "BLEReceiver";

    // This action will need to be redefined if you still use a custom scan result action
    // public static final String ACTION_SCANNER_FOUND_DEVICE = "com.smartwatchCompanion.bleReceiver.ACTION_SCANNER_FOUND_DEVICE";
    // Using Android's built-in action for found devices (if applicable with your new scanning method)
    public static final String ACTION_SCANNER_FOUND_DEVICE = BluetoothDevice.ACTION_FOUND;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Broadcast Receiver Triggered " + intent.toString());

        String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "Received intent with null action");
            return;
        }

        switch (action) {

            // Look whether we find our device
            case ACTION_SCANNER_FOUND_DEVICE: {
                // The way scan results are extracted will change significantly
                // if you switch to Android's built-in BLE scanner or another library.
                // The code below using Nordic's ScanResult is commented out.
                // You will need to re-implement device discovery and filtering.

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    Log.i(TAG, "Found: " + device.getAddress()
                            + " device name: " + device.getName());
                    // Add your logic to filter for the correct device and start the service
                    // For example, by checking device.getName() or device.getAddress()

                    // Example:
                    // if (device.getName() != null && device.getName().equals("YourSmartwatchName")) {
                    //    if (!BLEService.isRunning) {
                    //        MainActivity.currentDevice = device;
                    //        context.startForegroundService(new Intent(context, BLEService.class));
                    //    }
                    // }
                }


                /*
                Bundle extras = intent.getExtras();

                if (extras != null) {
                    // The Nordic specific EXTRA_LIST_SCAN_RESULT will not be present
                    // Object o = extras.get(BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT);
                    // if (o instanceof ArrayList) {
                        // ArrayList<ScanResult> scanResults = (ArrayList<ScanResult>) o;
                        // Log.v(TAG, "There are " + scanResults.size() + " results");

                        // for (ScanResult result : scanResults) {
                        //    if (result.getScanRecord() == null) {
                        //        Log.d(TAG, "getScanRecord is null");
                        //        continue;
                        //    }

                        //    BluetoothDevice device = result.getDevice();
                        //    ScanRecord scanRecord = result.getScanRecord();
                        //    String scanName = scanRecord.getDeviceName();
                        //    String deviceName = device.getName();
                        //    int rssi = result.getRssi();
// //                            mHeader.setText("Single device found: " + device.getName() + " RSSI: " + result.getRssi() + "dBm");
                        //    Log.i(TAG, "Found: " + device.getAddress()
                        //            + " scan name: " + scanName
                        //            + " device name: " + deviceName
                        //            + " RSSI: " + result.getRssi() + "dBm");

                        //    try {
                        //        if (!BLEService.isRunning) {
// //                                        BLEScanner.stopScan(MainActivity.reference);
                        //            MainActivity.currentDevice = result.getDevice();
                        //            context.startForegroundService(new Intent(context, BLEService.class));
                        //        }
                        //    } catch (IllegalStateException e) {
                        //        Log.e(TAG, "Could not register service");
                        //    }

                        // }
                    // } else {
                    //    // Received something, but not a list of scan results...
                    //    Log.d(TAG, "   no ArrayList but " + o);
                    // }
                } else {
                    Log.d(TAG, "no extras");
                }
                */

                break;
            }

            case BluetoothAdapter.ACTION_STATE_CHANGED: {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "BLE off");
                        // Need to take some action or app will fail...
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "BLE turning off");
//                        stopScan();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BLE on");
//                        startScan();    // restart scanning (provided the activity wants this to happen)
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BLE turning on");
                        break;
                }
                break;
            }
            default:
                // should not happen
                Log.d(TAG, "Received unexpected action " + intent.getAction());

        }
    }
}
