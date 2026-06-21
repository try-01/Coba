package com.example.samsungremote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun NumberButtons(onCommand: (String) -> Unit) {
    Column {
        Row {
            Button(onClick = { onCommand("KEY_1") }) { Text("1") }
            Button(onClick = { onCommand("KEY_2") }) { Text("2") }
            Button(onClick = { onCommand("KEY_3") }) { Text("3") }
        }
        Row {
            Button(onClick = { onCommand("KEY_4") }) { Text("4") }
            Button(onClick = { onCommand("KEY_5") }) { Text("5") }
            Button(onClick = { onCommand("KEY_6") }) { Text("6") }
        }
        Row {
            Button(onClick = { onCommand("KEY_7") }) { Text("7") }
            Button(onClick = { onCommand("KEY_8") }) { Text("8") }
            Button(onClick = { onCommand("KEY_9") }) { Text("9") }
        }
        Row {
            Button(onClick = { onCommand("KEY_0") }) { Text("0") }
        }
    }
}