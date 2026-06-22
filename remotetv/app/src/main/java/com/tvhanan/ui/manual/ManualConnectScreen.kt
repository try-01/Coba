package com.tvhanan.ui.manual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tvhanan.domain.model.TvDevice
import java.net.InetAddress

@Composable
fun ManualConnectScreen(
    onConnect: (TvDevice) -> Unit,
    onBack: () -> Unit
) {
    var ipAddress by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var ipError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Koneksi Manual",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = ipAddress,
            onValueChange = {
                ipAddress = it
                ipError = null
            },
            label = { Text("IP Address TV") },
            placeholder = { Text("192.168.1.100") },
            isError = ipError != null,
            supportingText = ipError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = macAddress,
            onValueChange = { macAddress = it },
            label = { Text("MAC Address (opsional)") },
            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "MAC diperlukan untuk Wake-on-LAN",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedButton(
            onClick = {
                if (validateIp(ipAddress)) {
                    val cleanMac = macAddress.trim().ifEmpty { null }
                    onConnect(
                        TvDevice(
                            ipAddress = ipAddress.trim(),
                            macAddress = cleanMac
                        )
                    )
                } else {
                    ipError = "Format IP tidak valid"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = ipAddress.isNotBlank()
        ) {
            Text("Hubungkan")
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kembali")
        }
    }
}

private fun validateIp(ip: String): Boolean {
    return try {
        InetAddress.getByName(ip.trim())
        ip.count { it == '.' } == 3
    } catch (_: Exception) {
        false
    }
}
