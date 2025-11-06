package com.marsraver.WledFx.wled.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WledConfig {
    private Object rev;
    private long vid;
    private Object id;
    private Object nw;
    private Object ap;
    private Object wifi;
    private Object hw;
    private Object light;
    private Object def;
    @JsonProperty("if")
    private InterfaceConfig iface;
    private Object ol;
    private Object timers;
    private Object ota;
    private Object um;
    
    @Data
    public static class InterfaceConfig {
        private Object sync;
        private Object nodes;
        private LiveConfig live;
        private Object va;
        private Object mqtt;
        private Object hue;
        private Object ntp;
    }
    
    @Data
    public static class LiveConfig {
        private boolean en; // Live enabled
        private boolean mso;
        private boolean rlm;
        private int port; // Port
        private boolean mc; // Multicast
        private DmxConfig dmx; // DMX configuration
        private int timeout;
        private boolean maxbri;
        @JsonProperty("no-gc")
        private boolean noGc;
        private int offset;
    }
    
    @Data
    public static class DmxConfig {
        private int uni; // Universe
        private boolean seqskip; // Skip out-of-sequence packets
        private int e131prio; // E1.31 priority
        private int addr; // DMX start address
        private int dss; // DMX single LED timeout
        private int mode; // DMX mode (4 = Multi RGB)
    }
}

