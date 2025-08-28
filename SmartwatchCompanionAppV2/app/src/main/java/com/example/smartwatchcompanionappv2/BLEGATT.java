package com.example.smartwatchcompanionappv2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.UUID;

public class BLEGATT extends BluetoothGattCallback {
    private static final String TAG = "BLEGATT";
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private final Context context;

    public BLEGATT(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            this.bluetoothAdapter = null; // Should ideally handle this error case
            Log.e(TAG, "BluetoothManager not available");
        }
    }

    public boolean connect(final String address) {
        Log.d(TAG, "Attempting to connect to: " + address);
        if (this.bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted. Cannot connect.");
            return false;
        }

        final BluetoothDevice device = this.bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }
        MainActivity.currentDevice = device;
        bluetoothGatt = device.connectGatt(context, false, this);
        Log.d(TAG, "Trying to create a new connection.");
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT not connected.");
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted. Cannot disconnect.");
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted. Cannot close GATT.");
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Connected to GATT server.");
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted. Cannot discover services.");
                return;
            }
            Log.i(TAG, "Attempting to start service discovery:" + (bluetoothGatt != null ? bluetoothGatt.discoverServices() : "gatt is null and discoverServices not called"));
            MainActivity.updateStatusText();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected from GATT server.");
            MainActivity.updateStatusText();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "onServicesDiscovered received: GATT_SUCCESS");
            BluetoothGattService service = gatt.getService(UUID.fromString(MainActivity.SERVICE_UUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(MainActivity.COMMAND_UUID));
                if (characteristic != null) {
                    Log.i(TAG, "Found service and command characteristic");
                    BluetoothGattCharacteristic notificationChar = service.getCharacteristic(UUID.fromString(MainActivity.CHARACTERISTIC_NOTIFICATION_UPDATE));
                    if (notificationChar != null) {
                        setCharacteristicNotification(notificationChar, true);
                    } else {
                        Log.w(TAG, "Notification Characteristic " + MainActivity.CHARACTERISTIC_NOTIFICATION_UPDATE + " not found.");
                    }
                } else {
                     Log.w(TAG, "Command Characteristic " + MainActivity.COMMAND_UUID + " not found in service " + MainActivity.SERVICE_UUID);
                }
            } else {
                 Log.w(TAG, "Service " + MainActivity.SERVICE_UUID + " not found");
            }
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized, cannot set characteristic notification.");
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted. Cannot set characteristic notification or write descriptor.");
            return;
        }
        
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (MainActivity.CHARACTERISTIC_NOTIFICATION_UPDATE.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")); // Standard CCCD UUID
            if (descriptor != null) {
                descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            } else {
                Log.w(TAG, "Descriptor not found for characteristic: " + characteristic.getUuid());
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "onCharacteristicRead for " + characteristic.getUuid().toString() + " Data: " + new String(characteristic.getValue()));
        } else {
            Log.w(TAG, "onCharacteristicRead error for " + characteristic.getUuid().toString() + " status: " + status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "onCharacteristicChanged for " + characteristic.getUuid().toString() + ". New data: " + new String(characteristic.getValue()));
        if (MainActivity.CHARACTERISTIC_NOTIFICATION_UPDATE.equals(characteristic.getUuid().toString())) {
            // MainActivity.updateNotifications(); // Potentially call this if needed
        }
    }
}
