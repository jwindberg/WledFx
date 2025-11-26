package com.marsraver.wledfx.wled

import com.marsraver.wledfx.wled.model.WledInfo
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

/**
 * Client for controlling WLED devices via DDP (Device-Discovery Protocol).
 * DDP is recommended for WLED as it has better performance and avoids Art-Net's secondary color issues.
 */
class WledDdpClient(
    private val wledInfo: WledInfo,
    private val port: Int = DDP_PORT,
) {
    private var socket: DatagramSocket? = null
    private var debugLogged = false
    private var sequence = 0

    @Throws(SocketException::class)
    fun connect() {
        if (socket == null || socket?.isClosed == true) {
            socket = DatagramSocket()
        }
    }

    fun disconnect() {
        socket?.takeIf { !it.isClosed }?.close()
    }

    @Throws(IOException::class)
    fun sendRgb(rgbData: IntArray, numLeds: Int) {
        val activeSocket = socket ?: throw IllegalStateException("Client not connected. Call connect() first.")
        if (rgbData.size < numLeds * 3) {
            throw IllegalArgumentException("RGB data array too small. Need at least ${numLeds * 3} elements")
        }

        val address = InetAddress.getByName(wledInfo.ip)
        val ledsPerPacket = LEDS_PER_PACKET
        val totalPackets = (numLeds + ledsPerPacket - 1) / ledsPerPacket

        for (packetNum in 0 until totalPackets) {
            val startLed = packetNum * ledsPerPacket
            val endLed = minOf(startLed + ledsPerPacket, numLeds)
            val ledsInPacket = endLed - startLed
            val packetRgbData = IntArray(ledsInPacket * 3)
            System.arraycopy(rgbData, startLed * 3, packetRgbData, 0, ledsInPacket * 3)

            if (!debugLogged && packetNum == 0) {
                debugLogged = true
                val nonZero = mutableListOf<String>()
                for (i in 0 until minOf(ledsInPacket, 20)) {
                    val r = packetRgbData[i * 3]
                    val g = packetRgbData[i * 3 + 1]
                    val b = packetRgbData[i * 3 + 2]
                    if ((r or g or b) != 0) {
                        nonZero += "led=${startLed + i} -> rgb($r,$g,$b)"
                    }
                }
                val name = wledInfo.name ?: wledInfo.ip ?: "unknown"
                println("DDP debug for $name (packet $packetNum, startLed=$startLed): ${if (nonZero.isEmpty()) "<none>" else nonZero.joinToString()}")
            }

            val packet = createDdpPacket(packetRgbData, ledsInPacket, startLed, packetNum == totalPackets - 1)
            val datagramPacket = DatagramPacket(packet, packet.size, address, port)
            activeSocket.send(datagramPacket)
        }

        // Increment sequence number for next frame
        sequence = (sequence + 1) and 0xFF
    }

    @Throws(IOException::class)
    fun sendColors(colors: Array<java.awt.Color>, numLeds: Int) {
        if (colors.size < numLeds) {
            throw IllegalArgumentException("Color array too small. Need at least $numLeds colors")
        }
        val rgbData = IntArray(numLeds * 3)
        for (i in 0 until numLeds) {
            val color = colors[i]
            rgbData[i * 3] = color.red
            rgbData[i * 3 + 1] = color.green
            rgbData[i * 3 + 2] = color.blue
        }
        sendRgb(rgbData, numLeds)
    }

    private fun createDdpPacket(rgbData: IntArray, numLeds: Int, startLed: Int, isLast: Boolean): ByteArray {
        // DDP packet format:
        // Header (10 bytes):
        //   - Flags (1 byte): bit 0 = push (isLast), bit 1 = query, bits 2-3 = reserved, bit 4 = reply, bit 5 = storage, bits 6-7 = timecode
        //   - Sequence (1 byte): sequence number
        //   - Data type (1 byte): 1 = RGB pixel data
        //   - Destination ID (1 byte): 1 = default
        //   - Data offset (4 bytes, big-endian): starting LED index
        //   - Data length (2 bytes, big-endian): number of RGB triplets
        
        val flags: Byte = if (isLast) 0x40.toByte() else 0x00.toByte()  // Push flag for last packet
        val dataOffset = startLed * 3  // DDP uses byte offset, not LED offset
        val dataLength = numLeds * 3   // Number of bytes (RGB triplets)
        
        val packetLength = 10 + dataLength
        val packet = ByteArray(packetLength)
        var offset = 0

        // Header (10 bytes)
        packet[offset++] = flags
        packet[offset++] = sequence.toByte()
        packet[offset++] = 1  // Data type: RGB pixel data
        packet[offset++] = 1  // Destination ID: default
        
        // Data offset (4 bytes, big-endian)
        packet[offset++] = ((dataOffset shr 24) and 0xFF).toByte()
        packet[offset++] = ((dataOffset shr 16) and 0xFF).toByte()
        packet[offset++] = ((dataOffset shr 8) and 0xFF).toByte()
        packet[offset++] = (dataOffset and 0xFF).toByte()
        
        // Data length (2 bytes, big-endian)
        packet[offset++] = ((dataLength shr 8) and 0xFF).toByte()
        packet[offset++] = (dataLength and 0xFF).toByte()

        // RGB data - DDP uses standard RGB order
        // WLED will convert to hardware order internally
        for (i in 0 until numLeds) {
            val r = rgbData[i * 3]
            val g = rgbData[i * 3 + 1]
            val b = rgbData[i * 3 + 2]
            
            // DDP standard is RGB order per LED
            packet[offset++] = (r and 0xFF).toByte()  // R
            packet[offset++] = (g and 0xFF).toByte()  // G
            packet[offset++] = (b and 0xFF).toByte()  // B
        }

        return packet
    }

    fun getWledInfo(): WledInfo = wledInfo

    fun getPort(): Int = port

    fun isConnected(): Boolean = socket?.isClosed == false

    companion object {
        private const val DDP_PORT = 4048
        private const val LEDS_PER_PACKET = 1440  // DDP max packet size is 1450 bytes (10 header + 1440 RGB bytes)

        @JvmStatic
        fun getDefaultDdpPort(): Int = DDP_PORT

        @JvmStatic
        fun getMaxLedsPerPacket(): Int = LEDS_PER_PACKET
    }
}

