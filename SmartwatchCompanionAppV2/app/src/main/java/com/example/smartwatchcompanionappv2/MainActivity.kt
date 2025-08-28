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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                    val scannedDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) } 
                    val connectionStatus by remember { mutableStateOf("Idle") }
                    val isScanning by remember { mutableStateOf(false) }
                    val connectedDeviceName by remember { mutableStateOf<String?>(null) }

                    // Line 61: Type mismatch error occurs here if MainScreen expects List<String>
                    MainScreen(
                        scannedDevices = scannedDevices, 
                        connectionStatus = connectionStatus,
                        onConnectClick = { deviceId ->
                            // TODO: Implement connect logic
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
            Log.d("MainActivity", "Instance.updateDeviceList called with device: ${device.name ?: "Unknown"}")
        } else {
            Log.d("MainActivity", "Instance.updateDeviceList called with null device")
        }
    }

    companion object {
        @JvmField
        @JvmStatic 
        var reference: MainActivity? = null

        @JvmField
        @JvmStatic // Added @JvmStatic for Java static access
        var currentDevice: BluetoothDevice? = null

        // @JvmStatic is correctly NOT on const vals
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
