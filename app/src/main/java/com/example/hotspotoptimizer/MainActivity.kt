package com.example.hotspotoptimizer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        if (granted) {
            turnOffBluetooth()
        } else {
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on while app is open – helps some devices keep hotspot more stable
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateStatus()

        binding.btnOptimize.setOnClickListener {
            optimize()
        }

        binding.btnOpenAppSettings.setOnClickListener {
            // Opens the system Apps list so user can force-stop apps themselves
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }

        binding.btnGrantDnd.setOnClickListener {
            requestDndAccess()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun optimize() {
        // 1. Turn Bluetooth off
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

        // 2. Enable Do Not Disturb if we have access
        enableDoNotDisturb()

        updateStatus()
        Toast.makeText(this, "Optimization applied", Toast.LENGTH_SHORT).show()
    }

    private fun turnOffBluetooth() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null && adapter.isEnabled) {
                @Suppress("DEPRECATION")
                adapter.disable()
                Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Need Bluetooth permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableDoNotDisturb() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (notificationManager.isNotificationPolicyAccessGranted) {
            // Set to total silence (alarms still allowed on most devices)
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
        } else {
            // Ask user to grant access
            MaterialAlertDialogBuilder(this)
                .setTitle("Do Not Disturb permission needed")
                .setMessage("To mute notifications you must grant access in system settings.")
                .setPositiveButton("Open settings") { _, _ ->
                    requestDndAccess()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun requestDndAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun updateStatus() {
        // Bluetooth status
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btOn = try {
            bluetoothManager.adapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
        binding.tvBluetoothStatus.text = if (btOn) "Bluetooth: ON" else "Bluetooth: OFF"
        binding.tvBluetoothStatus.setTextColor(
            if (btOn) 0xFFF44336.toInt() else 0xFF4CAF50.toInt()
        )

        // DND status
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val hasAccess = notificationManager.isNotificationPolicyAccessGranted
        val filter = notificationManager.currentInterruptionFilter
        val dndActive = hasAccess && filter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE

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
    }
}
