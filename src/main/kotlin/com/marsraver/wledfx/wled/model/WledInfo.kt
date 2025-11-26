package com.marsraver.wledfx.wled.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WledInfo(
    var ip: String? = null,
    var ver: String? = null,
    var vid: Long = 0,
    var name: String? = null,
    var udpport: Int = 0,
    var live: Boolean = false,
    var lm: String? = null,
    var lip: String? = null,
    var ws: Int = 0,
    var fxcount: Int = 0,
    var palcount: Int = 0,
    var arch: String? = null,
    var core: String? = null,
    var lwip: Int = 0,
    var freeheap: Long = 0,
    var uptime: Long = 0,
    var opt: Int = 0,
    var brand: String? = null,
    var product: String? = null,
    var mac: String? = null,
    var cn: String? = null,
    var release: String? = null,
    var str: Boolean = false,
    var simplifiedui: Boolean = false,
    var liveseg: Int = 0,
    var cpalcount: Int = 0,
    var maps: List<MapInfo> = emptyList(),
    var clock: Int = 0,
    var flash: Int = 0,
    var time: String? = null,
    var u: Map<String, Any?> = emptyMap(),
    @JsonProperty("leds")
    var leds: LedsInfo? = null,
    @JsonProperty("wifi")
    var wifi: WifiInfo? = null,
    @JsonProperty("fs")
    var fs: FsInfo? = null,
    var ndc: Int = 0,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LedsInfo(
        var cct: Boolean = false,
        var count: Int = 0,
        var fps: Int = 0,
        var rgbw: Boolean = false,
        var wv: Boolean = false,
        var pwr: Int = 0,
        var maxpwr: Int = 0,
        var maxseg: Int = 0,
        var lc: Int = 0,
        var seglc: List<Int> = emptyList(),
        var bootps: Int = 0,
        var matrix: MatrixInfo? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MatrixInfo(
        var w: Int = 0,
        var h: Int = 0,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MapInfo(
        var id: Int = 0,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WifiInfo(
        var bssid: String? = null,
        var signal: Int = 0,
        var channel: Int = 0,
        var rssi: Int = 0,
        var ap: Boolean = false,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FsInfo(
        var u: Long = 0,
        var t: Long = 0,
        var pmt: Long = 0,
    )
}

