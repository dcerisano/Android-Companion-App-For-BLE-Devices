package com.example.smartwatchcompanionappv2

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.smartwatchcompanionappv2.ui.theme.AndroidCompanionAppForBLEDevicesTheme
import androidx.activity.enableEdgeToEdge // <-- Add this import

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
        enableEdgeToEdge() // <-- Add this line
        // Test comment by AI Assistant - checking write operation
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // AI Assistant: Comment before setContent
        setContent {
            AndroidCompanionAppForBLEDevicesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android from MainActivity.kt")
                }
            }
        }
        // AI Assistant: Comment at the end of onCreate
    }

    companion object {
        // Another test comment by AI Assistant inside companion object
        @JvmField
        var currentDevice: BluetoothDevice? = null

        
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// AI Assistant: Comment before GreetingPreview
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidCompanionAppForBLEDevicesTheme {
        Greeting("Android Preview")
    }
}
