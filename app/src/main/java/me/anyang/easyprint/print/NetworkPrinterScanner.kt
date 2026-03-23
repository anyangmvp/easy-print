package me.anyang.easyprint.print

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkPrinterScanner(private val context: Context) {

    data class DiscoveredPrinter(
        val ip: String,
        val port: Int,
        val name: String,
        val type: String
    )

    private val printerPort = 9100 to "HP JetDirect/Raw"

    suspend fun scanNetwork(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        val printers = mutableListOf<DiscoveredPrinter>()
        val localSubnet = getLocalSubnet() ?: run {
            return@withContext scanKnownSubnets()
        }

        val (port, protocol) = printerPort
        for (host in 1..254) {
            val ip = "$localSubnet.$host"
            if (isPrinterReachable(ip, port)) {
                val printerName = detectPrinterName(ip, port)
                printers.add(
                    DiscoveredPrinter(
                        ip = ip,
                        port = port,
                        name = printerName,
                        type = protocol
                    )
                )
            }
        }

        if (printers.isEmpty()) {
            return@withContext scanKnownSubnets()
        }

        printers
    }

    private suspend fun scanKnownSubnets(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        val printers = mutableListOf<DiscoveredPrinter>()
        val subnets = listOf("192.168.1", "192.168.0", "192.168.2", "192.168.3", "10.0.0")

        val (port, protocol) = printerPort
        for (subnet in subnets) {
            for (host in 1..50) {
                val ip = "$subnet.$host"
                if (isPrinterReachable(ip, port)) {
                    val printerName = detectPrinterName(ip, port)
                    printers.add(
                        DiscoveredPrinter(
                            ip = ip,
                            port = port,
                            name = printerName,
                            type = protocol
                        )
                    )
                }
            }
        }

        printers
    }

    private fun isPrinterReachable(ip: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 300)
                true
            }
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun detectPrinterName(ip: String, port: Int): String {
        return when (port) {
            9100 -> "HP/Samsung Printer ($ip)"
            9228, 8290 -> "HP Printer ($ip)"
            631 -> "IPP Printer ($ip)"
            515 -> "LPD Printer ($ip)"
            else -> "Network Printer ($ip)"
        }
    }

    suspend fun checkPrinterAtIp(ipAddress: String): DiscoveredPrinter? = withContext(Dispatchers.IO) {
        val (port, protocol) = printerPort
        if (isPrinterReachable(ipAddress, port)) {
            return@withContext DiscoveredPrinter(
                ip = ipAddress,
                port = port,
                name = detectPrinterName(ipAddress, port),
                type = protocol
            )
        }
        null
    }

    private fun getLocalSubnet(): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            if (ipAddress != 0) {
                return String.format(
                    "%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}