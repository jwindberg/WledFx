package com.marsraver.wledfx.wled.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WledState(
    var on: Boolean = false,
    var bri: Int = 0,
    var transition: Int = 0,
    var ps: Int = 0,
    var pl: Int = 0,
    var ledmap: Int = 0,
    @JsonProperty("AudioReactive")
    var audioReactive: AudioReactiveConfig? = null,
    var nl: NlConfig? = null,
    var udpn: UdpnConfig? = null,
    var lor: Int = 0,
    var mainseg: Int = 0,
    var seg: List<Segment> = emptyList(),
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AudioReactiveConfig(
        var on: Boolean = false,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NlConfig(
        var on: Boolean = false,
        var dur: Int = 0,
        var mode: Int = 0,
        var tbri: Int = 0,
        var rem: Int = 0,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class UdpnConfig(
        var send: Boolean = false,
        var recv: Boolean = false,
        var sgrp: Int = 0,
        var rgrp: Int = 0,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Segment(
        var id: Int = 0,
        var start: Int = 0,
        var stop: Int = 0,
        var startY: Int = 0,
        var stopY: Int = 0,
        var len: Int = 0,
        var grp: Int = 0,
        var spc: Int = 0,
        var of: Int = 0,
        var on: Boolean = false,
        var frz: Boolean = false,
        var bri: Int = 0,
        var cct: Int = 0,
        var set: Int = 0,
        var col: List<List<Int>> = emptyList(),
        var fx: Int = 0,
        var sx: Int = 0,
        var ix: Int = 0,
        var pal: Int = 0,
        var c1: Int = 0,
        var c2: Int = 0,
        var c3: Int = 0,
        var sel: Boolean = false,
        var rev: Boolean = false,
        var mi: Boolean = false,
        @JsonProperty("rY")
        var rY: Boolean = false,
        @JsonProperty("mY")
        var mY: Boolean = false,
        var tp: Boolean = false,
        var o1: Boolean = false,
        var o2: Boolean = false,
        var o3: Boolean = false,
        var si: Int = 0,
        var m12: Int = 0,
    )
}

