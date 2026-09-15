package com.example.hotspotoptimizer

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val builder = Builder()
            .setSession("Hotspot Optimizer DNS")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("94.140.14.14")   // AdGuard DNS (ad-blocking)
            .addDnsServer("94.140.15.15")   // AdGuard DNS secondary
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        try {
            vpnInterface = builder.establish()
            running = true

            // Minimal loop so the VPN stays alive.
            // We do not inspect packets – only force DNS.
            Thread {
                val input = FileInputStream(vpnInterface?.fileDescriptor)
                val output = FileOutputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteBuffer.allocate(32767)

                while (running) {
                    try {
                        val length = input.read(buffer.array())
                        if (length > 0) {
                            // Just write the packet back (passthrough)
                            output.write(buffer.array(), 0, length)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        running = false
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.hotspotoptimizer.STOP_VPN"
    }
}
