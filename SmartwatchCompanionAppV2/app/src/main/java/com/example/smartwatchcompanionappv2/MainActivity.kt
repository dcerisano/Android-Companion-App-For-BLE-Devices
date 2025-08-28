package com.example.smartwatchcompanionappv2

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue // Added for 'by' delegate
import androidx.compose.runtime.mutableStateOf // Added for mutableStateOf
import androidx.compose.runtime.remember // Added for remember
import androidx.compose.runtime.derivedStateOf // Added for deriving the names list
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.smartwatchcompanionappv2.ui.theme.AndroidCompanionAppForBLEDevicesTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "Notification permission granted")
            } else {
                Log.w("MainActivity", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        reference = this 

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AndroidCompanionAppForBLEDevicesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scannedDevicesSource by remember { mutableStateOf(listOf<BluetoothDevice>()) } 
                    val connectionStatus by remember { mutableStateOf("Idle") }
                    val isScanning by remember { mutableStateOf(false) }
                    val connectedDeviceName by remember { mutableStateOf<String?>(null) }

                    val scannedDeviceDisplayNames by remember {
                        derivedStateOf {
                            scannedDevicesSource.map { device ->
                                try {
                                    device.name ?: "Unknown Device" 
                                } catch (e: SecurityException) {
                                    Log.e("MainActivity", "Missing BLUETOOTH_CONNECT permission for device.name in derivedStateOf", e)
                                    "Permission Denied" 
                                }
                            }
                        }
                    }

                    MainScreen(
                        scannedDevices = scannedDeviceDisplayNames, 
                        connectionStatus = connectionStatus,
                        onConnectClick = { deviceIdentifier -> 
                            Log.d("MainActivity", "onConnectClick with: $deviceIdentifier")
                        },
                        onDisconnectClick = {
                            // TODO: Implement disconnect logic
                        },
                        onStartScanClick = {
                            // TODO: Implement start scan logic
                        },
                        isScanning = isScanning,
                        connectedDeviceName = connectedDeviceName
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reference = null 
    }

    fun updateScanStatus() {
        Log.d("MainActivity", "Instance.updateScanStatus called")
    }

    fun updateDeviceList(device: BluetoothDevice?) {
        if (device != null) {
            val deviceNameToLog = try {
                device.name ?: "Unknown"
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Missing BLUETOOTH_CONNECT permission for device.name in updateDeviceList", e)
                "Permission Denied"
            }
            Log.d("MainActivity", "Instance.updateDeviceList called with device: $deviceNameToLog")
            // Example: Update scannedDevicesSource
            // val currentList = scannedDevicesSource.toMutableList() 
            // if (!currentList.any { it.address == device.address }) {
            //     currentList.add(device)
            //     scannedDevicesSource = currentList 
            // }
        } else {
            Log.d("MainActivity", "Instance.updateDeviceList called with null device to clear list?")
            // scannedDevicesSource = listOf() 
        }
    }

    companion object {
        @JvmField

        var reference: MainActivity? = null

        @JvmField

        var currentDevice: BluetoothDevice? = null

        // @JvmStatic is removed from const vals as it's redundant
        const val SERVICE_UUID = "00001809-0000-1000-8000-00805f9b34fb"
        const val COMMAND_UUID = "00002a37-0000-1000-8000-00805f9b34fb"
        const val CHARACTERISTIC_NOTIFICATION_UPDATE = "00002902-0000-1000-8000-00805f9b34fb"

        @JvmStatic 
        fun updateStatusText() {
            Log.d("MainActivity", "Companion.updateStatusText called")
        }

        @JvmStatic 
        fun updateNotifications() {
            Log.d("MainActivity", "Companion.updateNotifications called")
        }
    }
}
