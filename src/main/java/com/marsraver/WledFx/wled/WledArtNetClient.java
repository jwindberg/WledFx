package com.marsraver.WledFx.wled;

import com.marsraver.WledFx.wled.model.WledInfo;

import java.io.IOException;
import java.net.*;

/**
 * Client for controlling WLED devices via Art-Net protocol.
 * Art-Net is a UDP-based protocol for transmitting DMX512 data over IP networks.
 * 
 * WLED Art-Net Configuration:
 * - Protocol: Art-Net
 * - Port: 6454 (default Art-Net port)
 * - Start Universe: 0
 * - DMX Start Address: 0
 * - DMX Mode: Multi RGB (3 channels per LED for RGB control)
 */
public class WledArtNetClient {
    
    private static final String ARTNET_ID = "Art-Net\0";
    private static final int ARTNET_PORT = 6454;
    private static final short OP_ART_DMX = 0x5000;
    private static final short PROTOCOL_VERSION = 14;
    private static final int LEDS_PER_UNIVERSE = 170; // Maximum LEDs in universe (170 * 3 = 510 channels < 512)
    
    private final WledInfo wledInfo;
    private final int universe;
    private final int port;
    private final int dmxStartAddress;
    private DatagramSocket socket;
    
    /**
     * Creates a new ArtNet client for a WLED device.
     * 
     * @param wledInfo The WledInfo object containing device information
     * @param universe The Art-Net universe to use (typically 0)
     * @param port The UDP port to use (typically 6454 for Art-Net or 5568 for WLED sync)
     */
    public WledArtNetClient(WledInfo wledInfo, int universe, int port, int dmxStartAddress) {
        this.wledInfo = wledInfo;
        this.universe = universe;
        this.port = port;
        this.dmxStartAddress = dmxStartAddress;
    }
    
    /**
     * Creates a new ArtNet client for a WLED device.
     * 
     * @param wledInfo The WledInfo object containing device information
     * @param universe The Art-Net universe to use (typically 0)
     */
    public WledArtNetClient(WledInfo wledInfo, int universe, int port) {
        this(wledInfo, universe, port, 0);
    }
    
    /**
     * Creates a new ArtNet client for a WLED device with default universe 0.
     * 
     * @param wledInfo The WledInfo object containing device information
     */
    public WledArtNetClient(WledInfo wledInfo) {
        this(wledInfo, 0, ARTNET_PORT, 0);
    }
    
    /**
     * Opens the UDP socket for sending Art-Net packets.
     * Must be called before sending any data.
     * 
     * @throws SocketException if the socket could not be opened
     */
    public void connect() throws SocketException {
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket();
        }
    }
    
    /**
     * Closes the UDP socket.
     */
    public void disconnect() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    /**
     * Sends RGB color data to the WLED device.
     * Supports multiple universes if numLeds > LEDS_PER_UNIVERSE
     * 
     * @param rgbData Array of RGB values. For LED at index i:
     *                rgbData[i*3] = red, rgbData[i*3+1] = green, rgbData[i*3+2] = blue
     * @param numLeds Number of LEDs to control (can exceed LEDS_PER_UNIVERSE for multi-universe support)
     * @throws IOException if the packet could not be sent
     */
    public void sendRgb(int[] rgbData, int numLeds) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IllegalStateException("Client not connected. Call connect() first.");
        }
        
        if (rgbData.length < numLeds * 3) {
            throw new IllegalArgumentException("RGB data array too small. Need at least " + (numLeds * 3) + " elements");
        }
        
        InetAddress address = InetAddress.getByName(wledInfo.getIp());
        
        // Split data across multiple universes if needed
        int ledsPerUniverse = LEDS_PER_UNIVERSE;
        int totalUniverses = (numLeds + ledsPerUniverse - 1) / ledsPerUniverse; // Ceiling division
        
        for (int uni = 0; uni < totalUniverses; uni++) {
            int startLed = uni * ledsPerUniverse;
            int endLed = Math.min(startLed + ledsPerUniverse, numLeds);
            int ledsInThisUniverse = endLed - startLed;
            
            // Extract RGB data for this universe
            int[] universeRgbData = new int[ledsInThisUniverse * 3];
            System.arraycopy(rgbData, startLed * 3, universeRgbData, 0, ledsInThisUniverse * 3);
            
            // Create Art-Net packet for this universe
            byte[] packet = createArtNetPacket(universeRgbData, ledsInThisUniverse, universe + uni);
            
            // Send to WLED device
            DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, port);
            socket.send(datagramPacket);
        }
    }
    
    /**
     * Sends an array of Color objects to the WLED device.
     * 
     * @param colors Array of Color objects
     * @param numLeds Number of LEDs to control
     * @throws IOException if the packet could not be sent
     */
    public void sendColors(java.awt.Color[] colors, int numLeds) throws IOException {
        if (colors.length < numLeds) {
            throw new IllegalArgumentException("Color array too small. Need at least " + numLeds + " colors");
        }
        
        int[] rgbData = new int[numLeds * 3];
        for (int i = 0; i < numLeds; i++) {
            rgbData[i * 3] = colors[i].getRed();
            rgbData[i * 3 + 1] = colors[i].getGreen();
            rgbData[i * 3 + 2] = colors[i].getBlue();
        }
        
        sendRgb(rgbData, numLeds);
    }
    
    /**
     * Creates an Art-Net packet from RGB data.
     * 
     * @param rgbData RGB color data
     * @param numLeds Number of LEDs
     * @param universeNum Universe number to use in the packet
     * @return Art-Net packet as byte array
     */
    private byte[] createArtNetPacket(int[] rgbData, int numLeds, int universeNum) {
        // DMX data includes start code (0x00) + optional padding + RGB data
        // For DMX Start Address > 0, we need padding bytes after start code
        int dmxDataLength = 1 + dmxStartAddress + (numLeds * 3); // Start code + padding + RGB data
        int packetLength = 18 + dmxDataLength; // Header (18 bytes) + DMX data
        
        byte[] packet = new byte[packetLength];
        int offset = 0;
        
        // Art-Net ID: "Art-Net\0"
        System.arraycopy(ARTNET_ID.getBytes(), 0, packet, offset, 8);
        offset += 8;
        
        // OpCode (ArtDMX = 0x5000)
        packet[offset++] = (byte) (OP_ART_DMX & 0xFF);
        packet[offset++] = (byte) ((OP_ART_DMX >> 8) & 0xFF);
        
        // Protocol version (14)
        packet[offset++] = 0;
        packet[offset++] = (byte) PROTOCOL_VERSION;
        
        // Sequence (0 = no sequence)
        packet[offset++] = 0;
        
        // Physical (0 = no physical input port)
        packet[offset++] = 0;
        
        // Universe (little-endian)
        packet[offset++] = (byte) (universeNum & 0xFF);
        packet[offset++] = (byte) ((universeNum >> 8) & 0xFF);
        
        // Data length (big-endian) - includes start code
        packet[offset++] = (byte) ((dmxDataLength >> 8) & 0xFF);
        packet[offset++] = (byte) (dmxDataLength & 0xFF);
        
        // DMX start code (0x00)
        packet[offset++] = 0;
        
        // Padding bytes for DMX Start Address offset
        for (int i = 0; i < dmxStartAddress; i++) {
            packet[offset++] = 0;
        }
        
        // DMX data (BRG values for each LED - hardware expects BRG order)
        for (int i = 0; i < numLeds; i++) {
            packet[offset++] = (byte) rgbData[i * 3 + 2]; // Blue
            packet[offset++] = (byte) rgbData[i * 3];     // Red
            packet[offset++] = (byte) rgbData[i * 3 + 1]; // Green
        }
        
        return packet;
    }
    
    /**
     * Gets the WledInfo object.
     * 
     * @return WledInfo object
     */
    public WledInfo getWledInfo() {
        return wledInfo;
    }
    
    /**
     * Gets the default Art-Net port (6454).
     * 
     * @return Default Art-Net port
     */
    public static int getDefaultArtNetPort() {
        return ARTNET_PORT;
    }
    
    /**
     * Gets the configured port for this client.
     * 
     * @return Configured port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the maximum number of LEDs that can be controlled in a single universe.
     * 
     * @return Maximum LEDs per universe
     */
    public static int getMaxLedsPerUniverse() {
        return LEDS_PER_UNIVERSE;
    }
    
    /**
     * Gets the Art-Net universe being used.
     * 
     * @return Universe number
     */
    public int getUniverse() {
        return universe;
    }
    
    /**
     * Checks if the client is connected.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
}

