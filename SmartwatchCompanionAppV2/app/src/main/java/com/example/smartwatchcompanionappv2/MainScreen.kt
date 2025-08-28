package com.example.smartwatchcompanionappv2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartwatchcompanionappv2.ui.theme.AndroidCompanionAppForBLEDevicesTheme

@OptIn(ExperimentalMaterial3Api::class) // For TopAppBar and Scaffold
@Composable
fun MainScreen(
    scannedDevices: List<String>,
    connectionStatus: String,
    onConnectClick: (String) -> Unit,
    onDisconnectClick: () -> Unit,
    onStartScanClick: () -> Unit,
    isScanning: Boolean,
    connectedDeviceName: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Device Companion") }
                // You can add navigation icons or action items here if needed
            )
        }
    ) { innerPadding -> // This innerPadding contains the insets
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold to respect system bars
                .padding(16.dp), // Add your own content padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status: $connectionStatus",
                style = MaterialTheme.typography.titleMedium
            )

            if (connectedDeviceName != null) {
                Text("Connected to: $connectedDeviceName")
                Button(onClick = onDisconnectClick) {
                    Text("Disconnect")
                }
            } else {
                Button(onClick = onStartScanClick, enabled = !isScanning) {
                    Text(if (isScanning) "Scanning..." else "Start Scan")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (scannedDevices.isNotEmpty()) {
                Text(
                    text = "Available Devices:",
                    style = MaterialTheme.typography.titleSmall
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(scannedDevices) { device ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(device)
                                Button(
                                    onClick = { onConnectClick(device) },
                                    enabled = connectedDeviceName == null // Disable connect if already connected
                                ) {
                                    Text("Connect")
                                }
                            }
                        }
                    }
                }
            } else {
                if (!isScanning && connectedDeviceName == null) {
                    Text("No devices found. Try scanning.")
                }
            }
        }
    }
}

// Preview functions for different states of MainScreen
@Preview(showBackground = true, name = "MainScreen Disconnected")
@Composable
fun MainScreenPreviewDisconnected() {
    AndroidCompanionAppForBLEDevicesTheme { // Use your app's theme for previews
        MainScreen(
            scannedDevices = listOf("TestDevice Alpha", "TestDevice Beta"),
            connectionStatus = "Disconnected. Tap 'Start Scan'.",
            onConnectClick = {},
            onDisconnectClick = {},
            onStartScanClick = {},
            isScanning = false,
            connectedDeviceName = null
        )
    }
}

@Preview(showBackground = true, name = "MainScreen Scanning")
@Composable
fun MainScreenPreviewScanning() {
    AndroidCompanionAppForBLEDevicesTheme {
        MainScreen(
            scannedDevices = emptyList(),
            connectionStatus = "Scanning for devices...",
            onConnectClick = {},
            onDisconnectClick = {},
            onStartScanClick = {},
            isScanning = true,
            connectedDeviceName = null
        )
    }
}

@Preview(showBackground = true, name = "MainScreen Connected")
@Composable
fun MainScreenPreviewConnected() {
    AndroidCompanionAppForBLEDevicesTheme {
        MainScreen(
            scannedDevices = listOf("TestDevice Alpha", "TestDevice Beta"),
            connectionStatus = "Connected",
            onConnectClick = {},
            onDisconnectClick = {},
            onStartScanClick = {},
            isScanning = false,
            connectedDeviceName = "TestDevice Beta"
        )
    }
}