package com.marsraver.WledFx.wled

import com.marsraver.WledFx.wled.model.WledInfo
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

/**
 * Client for controlling WLED devices via Art-Net protocol.
 */
class WledArtNetClient(
    private val wledInfo: WledInfo,
    private val universe: Int,
    private val port: Int,
    private val dmxStartAddress: Int = 0,
) {
    private var socket: DatagramSocket? = null

    constructor(wledInfo: WledInfo, universe: Int, port: Int) : this(wledInfo, universe, port, 0)

    constructor(wledInfo: WledInfo) : this(wledInfo, 0, ARTNET_PORT, 0)

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
        val ledsPerUniverse = LEDS_PER_UNIVERSE
        val totalUniverses = (numLeds + ledsPerUniverse - 1) / ledsPerUniverse

        for (uni in 0 until totalUniverses) {
            val startLed = uni * ledsPerUniverse
            val endLed = minOf(startLed + ledsPerUniverse, numLeds)
            val ledsInUniverse = endLed - startLed
            val universeRgbData = IntArray(ledsInUniverse * 3)
            System.arraycopy(rgbData, startLed * 3, universeRgbData, 0, ledsInUniverse * 3)

            val packet = createArtNetPacket(universeRgbData, ledsInUniverse, universe + uni)
            val datagramPacket = DatagramPacket(packet, packet.size, address, port)
            activeSocket.send(datagramPacket)
        }
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

    private fun createArtNetPacket(rgbData: IntArray, numLeds: Int, universeNum: Int): ByteArray {
        val dmxDataLength = 1 + dmxStartAddress + numLeds * 3
        val packetLength = 18 + dmxDataLength
        val packet = ByteArray(packetLength)
        var offset = 0

        ARTNET_ID.toByteArray().copyInto(packet, offset)
        offset += 8

        packet[offset++] = (OP_ART_DMX and 0xFF).toByte()
        packet[offset++] = ((OP_ART_DMX shr 8) and 0xFF).toByte()

        packet[offset++] = 0
        packet[offset++] = PROTOCOL_VERSION.toByte()
        packet[offset++] = 0
        packet[offset++] = 0

        packet[offset++] = (universeNum and 0xFF).toByte()
        packet[offset++] = ((universeNum shr 8) and 0xFF).toByte()

        packet[offset++] = ((dmxDataLength shr 8) and 0xFF).toByte()
        packet[offset++] = (dmxDataLength and 0xFF).toByte()

        packet[offset++] = 0

        repeat(dmxStartAddress) {
            packet[offset++] = 0
        }

        for (i in 0 until numLeds) {
            packet[offset++] = (rgbData[i * 3 + 2] and 0xFF).toByte()
            packet[offset++] = (rgbData[i * 3] and 0xFF).toByte()
            packet[offset++] = (rgbData[i * 3 + 1] and 0xFF).toByte()
        }

        return packet
    }

    fun getWledInfo(): WledInfo = wledInfo

    fun getPort(): Int = port

    fun getUniverse(): Int = universe

    fun isConnected(): Boolean = socket?.isClosed == false

    companion object {
        private const val ARTNET_ID = "Art-Net\u0000"
        private const val ARTNET_PORT = 6454
        private const val OP_ART_DMX = 0x5000
        private const val PROTOCOL_VERSION = 14
        private const val LEDS_PER_UNIVERSE = 170

        @JvmStatic
        fun getDefaultArtNetPort(): Int = ARTNET_PORT

        @JvmStatic
        fun getMaxLedsPerUniverse(): Int = LEDS_PER_UNIVERSE
    }
}

