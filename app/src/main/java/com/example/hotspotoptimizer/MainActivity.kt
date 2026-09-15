package com.example.hotspotoptimizer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hotspotoptimizer.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) turnOffBluetooth()
        else Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on – critical for many phones to keep hotspot alive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateStatus()

        binding.btnOptimize.setOnClickListener { maxOptimize() }

        binding.btnGrantDnd.setOnClickListener { requestDndAccess() }
        binding.btnOpenAppSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }
        binding.btnLocationSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        binding.btnBatterySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }
        binding.btnNfcSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
        binding.btnWifiSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        binding.btnDeveloperOptions.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Developer options not enabled", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnDisplaySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun maxOptimize() {
        // 1. Bluetooth OFF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                turnOffBluetooth()
            }
        } else {
            turnOffBluetooth()
        }

        // 2. Do Not Disturb ON (total silence)
        enableDoNotDisturb()

        // 3. Lower brightness hard (saves battery → longer hotspot)
        lowerBrightness()

        // 4. Mute media volume
        muteMediaVolume()

        // 5. Mute notification & system volume too
        muteOtherVolumes()

        // 6. Disable auto-rotate (saves a tiny bit of CPU)
        disableAutoRotate()

        updateStatus()
        Toast.makeText(this, "MAX performance mode applied", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Need Bluetooth permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableDoNotDisturb() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Do Not Disturb permission needed")
                .setMessage("Grant access so the app can mute all notifications.")
                .setPositiveButton("Open settings") { _, _ -> requestDndAccess() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun lowerBrightness() {
        try {
            val lp = window.attributes
            lp.screenBrightness = 0.05f   // almost minimum
            window.attributes = lp
        } catch (_: Exception) {}
    }

    private fun muteMediaVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun muteOtherVolumes() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        } catch (_: Exception) {}
    }

    private fun disableAutoRotate() {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
        } catch (_: Exception) {
            // Needs WRITE_SETTINGS – most phones will just ignore this
        }
    }

    private fun requestDndAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun updateStatus() {
        // Bluetooth
        val btOn = try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        } catch (_: SecurityException) { false }

        binding.tvBluetoothStatus.text = if (btOn) "Bluetooth: ON" else "Bluetooth: OFF"
        binding.tvBluetoothStatus.setTextColor(if (btOn) 0xFFF44336.toInt() else 0xFF4CAF50.toInt())

        // DND
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val hasAccess = nm.isNotificationPolicyAccessGranted
        val dndActive = hasAccess && nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE

        binding.tvDndStatus.text = when {
            !hasAccess -> "Do Not Disturb: No permission"
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

        // NFC
        val nfc = NfcAdapter.getDefaultAdapter(this)
        val nfcOn = nfc?.isEnabled == true
        binding.tvNfcStatus.text = when {
            nfc == null -> "NFC: Not available"
            nfcOn -> "NFC: ON"
            else -> "NFC: OFF"
        }
        binding.tvNfcStatus.setTextColor(
            when {
                nfc == null -> 0xFF9E9E9E.toInt()
                nfcOn -> 0xFFF44336.toInt()
                else -> 0xFF4CAF50.toInt()
            }
        )
    }
}
