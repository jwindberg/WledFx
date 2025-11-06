package com.marsraver.WledFx.wled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsraver.WledFx.wled.model.WledConfig;
import com.marsraver.WledFx.wled.model.WledInfo;
import com.marsraver.WledFx.wled.model.WledState;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Client for interacting with a WLED device via REST API.
 */
public class WledClient {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final String ipAddress;
    
    /**
     * Creates a new WLED client for the specified device.
     * 
     * @param ipAddress The IP address of the WLED device
     */
    public WledClient(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Gets device information from /json/info endpoint.
     * 
     * @return WledInfo object with device information
     * @throws Exception if the request fails
     */
    public WledInfo getInfo() throws Exception {
        URL url = new URL("http://" + ipAddress + "/json/info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(400);
        conn.setReadTimeout(800);
        conn.setRequestMethod("GET");
        
        WledInfo info;
        if (conn.getResponseCode() == 200) {
            try (InputStream in = conn.getInputStream()) {
                info = MAPPER.readValue(in, WledInfo.class);
            }
        } else {
            throw new Exception("Failed to get info: HTTP " + conn.getResponseCode());
        }
        
        conn.disconnect();
        return info;
    }
    
    /**
     * Gets device configuration from /json/cfg endpoint.
     * 
     * @return WledConfig object with device configuration
     * @throws Exception if the request fails
     */
    public WledConfig getConfig() throws Exception {
        URL url = new URL("http://" + ipAddress + "/json/cfg");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(400);
        conn.setReadTimeout(800);
        conn.setRequestMethod("GET");
        
        WledConfig config;
        if (conn.getResponseCode() == 200) {
            try (InputStream in = conn.getInputStream()) {
                config = MAPPER.readValue(in, WledConfig.class);
            }
        } else {
            throw new Exception("Failed to get config: HTTP " + conn.getResponseCode());
        }
        
        conn.disconnect();
        return config;
    }
    
    /**
     * Gets device state from /json/state endpoint.
     * 
     * @return WledState object with device state
     * @throws Exception if the request fails
     */
    public WledState getState() throws Exception {
        URL url = new URL("http://" + ipAddress + "/json/state");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(400);
        conn.setReadTimeout(800);
        conn.setRequestMethod("GET");
        
        WledState state;
        if (conn.getResponseCode() == 200) {
            try (InputStream in = conn.getInputStream()) {
                state = MAPPER.readValue(in, WledState.class);
            }
        } else {
            throw new Exception("Failed to get state: HTTP " + conn.getResponseCode());
        }
        
        conn.disconnect();
        return state;
    }
    
    /**
     * Gets the IP address of the WLED device.
     * 
     * @return IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }
}

