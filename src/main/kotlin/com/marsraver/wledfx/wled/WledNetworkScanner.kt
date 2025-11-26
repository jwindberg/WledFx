package com.marsraver.wledfx.wled

import com.marsraver.wledfx.wled.model.WledInfo
import java.net.Inet4Address
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WledNetworkScanner {

    @Throws(Exception::class)
    fun discover(): List<WledInfo> {
        val subnet = detectLocalSubnet()
        if (subnet == null) {
            System.err.println("❌ Could not determine local subnet.")
            return emptyList()
        }

        println("📡 Detected subnet: $subnet.x")
        val devices = CopyOnWriteArrayList<WledInfo>()
        val pool: ExecutorService = Executors.newFixedThreadPool(64)

        for (i in 1 until 255) {
            val host = i
            pool.submit {
                val ip = "$subnet.$host"
                try {
                    val client = WledClient(ip)
                    val info = client.getInfo()
                    info.ip = ip
                    devices.add(info)
                    println("✅ Found $info")
                } catch (_: Exception) {
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(25, TimeUnit.SECONDS)
        return Collections.unmodifiableList(devices)
    }

    @Throws(SocketException::class)
    private fun detectLocalSubnet(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue

            for (addr: InterfaceAddress in iface.interfaceAddresses) {
                val inetAddr = addr.address
                if (inetAddr is Inet4Address && !inetAddr.isLoopbackAddress) {
                    val bytes = inetAddr.address
                    return String.format(
                        "%d.%d.%d",
                        bytes[0].toInt() and 0xFF,
                        bytes[1].toInt() and 0xFF,
                        bytes[2].toInt() and 0xFF
                    )
                }
            }
        }
        return null
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("🔍 Discovering WLED devices on local network...")
        val devices = discover()
        println("\n=== ${devices.size} WLED devices found ===")
        devices.forEach { println(it) }
    }
}

