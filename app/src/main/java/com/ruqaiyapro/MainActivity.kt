package com.ruqaiyapro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ruqaiyapro.brain.BrainManager
import com.ruqaiyapro.service.FloatingChibiService
import com.ruqaiyapro.service.RuqaiyaHotwordService
import com.ruqaiyapro.tuner.HardwareTuner
import com.ruqaiyapro.ui.theme.RuqaiyaProTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (audioGranted && cameraGranted) {
            startHotwordService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Hardware Tuner for Infinix HOT12 Play (X6816C)
        HardwareTuner.applyInfinixOptimizations(this)
        
        checkAndRequestPermissions()
        checkOverlayPermission()

        setContent {
            RuqaiyaProTheme {
                MainScreen(
                    onStartChibi = { startChibiOverlay() },
                    onStartHotword = { startHotwordService() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startChibiOverlay() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingChibiService::class.java)
            startService(intent)
            Toast.makeText(this, "Ruqaiya Chibi Overlay Started! 😍", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please allow Overlay permission first", Toast.LENGTH_LONG).show()
        }
    }

    private fun startHotwordService() {
        val intent = Intent(this, RuqaiyaHotwordService::class.java)
        startService(intent)
    }
}