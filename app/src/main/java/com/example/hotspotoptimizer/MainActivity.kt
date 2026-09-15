package com.example.hotspotoptimizer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.VpnService
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
        if (granted) turnOffBluetooth()
        runAutomaticOptimizations()
    }

    private val requestVpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startDnsVpn()
        } else {
            Toast.makeText(this, "VPN permission denied - DNS adblock not active", Toast.LENGTH_SHORT).show()
        }
        runAutomaticOptimizations()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateStatus()

        binding.btnOptimize.setOnClickListener {
            startOptimize()
        }

        binding.btnStop.setOnClickListener {
            stopOptimizations()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun startOptimize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                prepareVpn()
                return
            }
        }
        turnOffBluetooth()
        prepareVpn()
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            requestVpnPermission.launch(intent)
        } else {
            startDnsVpn()
            runAutomaticOptimizations()
        }
    }

    private fun startDnsVpn() {
        val intent = Intent(this, DnsVpnService::class.java)
        startService(intent)
        Toast.makeText(this, "Ad-blocking DNS started", Toast.LENGTH_SHORT).show()
    }

    private fun runAutomaticOptimizations() {
        enableDoNotDisturb()
        lowerBrightness()
        muteAllVolumes()
        updateStatus()
        Toast.makeText(this, "Optimizations applied", Toast.LENGTH_SHORT).show()
    }

    private fun stopOptimizations() {
        // Stop VPN
        try {
            val stopIntent = Intent(this, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            startService(stopIntent)
        } catch (_: Exception) {}

        // Restore volumes (set media to ~60%)
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusic * 0.6).toInt(), 0)

            val maxNotif = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (maxNotif * 0.5).toInt(), 0)

            val maxSystem = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, (maxSystem * 0.5).toInt(), 0)

            val maxRing = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, (maxRing * 0.5).toInt(), 0)
        } catch (_: Exception) {}

        // Restore brightness
        try {
            val lp = window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = lp
        } catch (_: Exception) {}

        // Turn off Do Not Disturb
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: Exception) {}

        // Turn Bluetooth back on
        try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            if (adapter != null && !adapter.isEnabled) {
                @Suppress("DEPRECATION")
                adapter.enable()
            }
        } catch (_: Exception) {}

        updateStatus()
        Toast.makeText(this, "Optimizations stopped - audio restored", Toast.LENGTH_SHORT).show()
    }

    private fun turnOffBluetooth() {
        try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            if (adapter != null && adapter.isEnabled) {
                @Suppress("DEPRECATION")
                adapter.disable()
            }
        } catch (_: Exception) {}
    }

    private fun enableDoNotDisturb() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } catch (_: Exception) {}
    }

    private fun lowerBrightness() {
        try {
            val lp = window.attributes
            lp.screenBrightness = 0.05f
            window.attributes = lp
        } catch (_: Exception) {}
    }

    private fun muteAllVolumes() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        } catch (_: Exception) {}
    }

    private fun updateStatus() {
        val btOn = try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        } catch (_: Exception) { false }

        binding.tvBluetoothStatus.text = if (btOn) "Bluetooth: ON" else "Bluetooth: OFF"
        binding.tvBluetoothStatus.setTextColor(if (btOn) 0xFFF44336.toInt() else 0xFF4CAF50.toInt())

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val hasAccess = try { nm.isNotificationPolicyAccessGranted } catch (_: Exception) { false }
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
