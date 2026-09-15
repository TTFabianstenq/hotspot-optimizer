package com.example.hotspotoptimizer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hotspotoptimizer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            turnOffBluetooth()
            runAutomaticOptimizations()
        } else {
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            runAutomaticOptimizations()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on while app is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateStatus()

        binding.btnOptimize.setOnClickListener {
            startOptimize()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun startOptimize() {
        // Request Bluetooth permission if needed, then run everything
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        turnOffBluetooth()
        runAutomaticOptimizations()
    }

    private fun runAutomaticOptimizations() {
        enableDoNotDisturb()
        lowerBrightness()
        muteAllVolumes()
        updateStatus()
        Toast.makeText(this, "Automatic optimizations applied", Toast.LENGTH_SHORT).show()
    }

    private fun turnOffBluetooth() {
        try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            if (adapter != null && adapter.isEnabled) {
                @Suppress("DEPRECATION")
                adapter.disable()
            }
        } catch (_: SecurityException) {
            // permission missing - ignore
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun enableDoNotDisturb() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            }
            // If not granted, it simply does nothing (no settings page opened)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun lowerBrightness() {
        try {
            val lp = window.attributes
            lp.screenBrightness = 0.05f
            window.attributes = lp
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun muteAllVolumes() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun updateStatus() {
        // Bluetooth
        val btOn = try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
        binding.tvBluetoothStatus.text = if (btOn) "Bluetooth: ON" else "Bluetooth: OFF"
        binding.tvBluetoothStatus.setTextColor(if (btOn) 0xFFF44336.toInt() else 0xFF4CAF50.toInt())

        // DND
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val hasAccess = try {
            nm.isNotificationPolicyAccessGranted
        } catch (_: Exception) {
            false
        }
        val dndActive = hasAccess && nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE

        binding.tvDndStatus.text = when {
            !hasAccess -> "Do Not Disturb: needs permission (one-time)"
            dndActive -> "Do Not Disturb: ON"
            else -> "Do Not Disturb: OFF"
        }
        binding.tvDndStatus.setTextColor(
            when {
                !hasAccess -> 0xFFFF9800.toInt()
                dndActive -> 0xFF4CAF50.toInt()
                else -> 0xFFF44336.toInt()
            }
        )
    }
}
