package com.marsraver.WledFx.wled.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WledInfo {
    // IP address (added when discovered, not in JSON response)
    private String ip;
    
    // General information
    private String ver; // Version name
    private long vid; // Build ID
    private String name; // Friendly name of the light
    private int udpport; // UDP port for realtime packets and WLED broadcast
    private boolean live; // Receiving realtime data via UDP or E1.31
    private String lm; // Realtime data source
    private String lip; // Realtime data source IP address
    private int ws; // Number of connected WebSockets clients
    private int fxcount; // Number of effects included
    private int palcount; // Number of palettes configured
    private String arch; // Platform name
    private String core; // Version of the underlying SDK
    private int lwip; // Version of LwIP
    private long freeheap; // Bytes of heap memory available
    private long uptime; // Time since last boot/reset in seconds
    private int opt; // Used for debugging purposes
    private String brand; // Producer/vendor of the light
    private String product; // Product name
    private String mac; // Hardware MAC address
    
    // Additional fields from actual device
    private String cn; // Device codename
    private String release; // Release name
    private boolean str; // Streaming enabled
    private boolean simplifiedui; // Simplified UI enabled
    private int liveseg; // Live segment ID
    private int cpalcount; // Custom palette count
    private List<MapInfo> maps; // List of mappings
    private int clock; // CPU clock speed in MHz
    private int flash; // Flash size in MB
    private String time; // Current time
    private Map<String, Object> u; // UI data
    
    // LED setup information
    @JsonProperty("leds")
    private LedsInfo leds;
    
    // WiFi information
    @JsonProperty("wifi")
    private WifiInfo wifi;
    
    // Filesystem information
    @JsonProperty("fs")
    private FsInfo fs;
    
    // Node discovery count
    private int ndc; // Number of other WLED devices discovered on the network
    
    @Data
    public static class LedsInfo {
        private boolean cct; // Supports color temperature control
        private int count; // Total LED count
        private int fps; // Current frames per second
        private boolean rgbw; // LEDs are 4-channel (RGB + White)
        private boolean wv; // White channel slider should be displayed
        private int pwr; // Current LED power usage in milliamps
        private int maxpwr; // Maximum power budget in milliamps
        private int maxseg; // Maximum number of segments supported
        private int lc; // Logical AND of all active segment's virtual light capabilities
        private List<Integer> seglc; // Per-segment virtual light capabilities
        private int bootps; // Boot preset
        private MatrixInfo matrix; // Matrix configuration
    }
    
    @Data
    public static class MatrixInfo {
        private int w; // Matrix width
        private int h; // Matrix height
    }
    
    @Data
    public static class MapInfo {
        private int id; // Mapping ID
    }
    
    @Data
    public static class WifiInfo {
        private String bssid; // BSSID of the connected network
        private int signal; // Relative signal quality
        private int channel; // Current WiFi channel
        private int rssi; // Received signal strength indicator
        private boolean ap; // Access point mode
    }
    
    @Data
    public static class FsInfo {
        private long u; // Used filesystem space in kilobytes
        private long t; // Total filesystem size in kilobytes
        private long pmt; // Last modification timestamp of the presets.json file
    }
}
