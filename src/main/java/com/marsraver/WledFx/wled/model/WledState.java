package com.marsraver.WledFx.wled.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WledState {
    private boolean on;
    private int bri;
    private int transition;
    private int ps;
    private int pl;
    private int ledmap;
    @JsonProperty("AudioReactive")
    private AudioReactiveConfig audioReactive;
    private NlConfig nl;
    private UdpnConfig udpn;
    private int lor;
    private int mainseg;
    private List<Segment> seg;
    
    @Data
    public static class AudioReactiveConfig {
        private boolean on;
    }
    
    @Data
    public static class NlConfig {
        private boolean on;
        private int dur;
        private int mode;
        private int tbri;
        private int rem;
    }
    
    @Data
    public static class UdpnConfig {
        private boolean send;
        private boolean recv;
        private int sgrp;
        private int rgrp;
    }
    
    @Data
    public static class Segment {
        private int id;
        private int start;
        private int stop;
        private int startY;
        private int stopY;
        private int len;
        private int grp;
        private int spc;
        private int of;
        private boolean on;
        private boolean frz;
        private int bri;
        private int cct;
        private int set;
        private List<List<Integer>> col;
        private int fx;
        private int sx;
        private int ix;
        private int pal;
        private int c1;
        private int c2;
        private int c3;
        private boolean sel;
        private boolean rev;
        private boolean mi;
        @JsonProperty("rY")
        private boolean rY;
        @JsonProperty("mY")
        private boolean mY;
        private boolean tp;
        private boolean o1;
        private boolean o2;
        private boolean o3;
        private int si;
        private int m12;
    }
}

