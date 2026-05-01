package com.transferrate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.transferrate.app.ui.RatesScreen
import com.transferrate.app.ui.theme.TransferRateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge is the Android 14+ default; opt in explicitly so
        // future framework defaults don't change our layout.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TransferRateTheme { RatesScreen() }
        }
    }
}
