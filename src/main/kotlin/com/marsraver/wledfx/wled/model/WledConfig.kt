package com.marsraver.wledfx.wled.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WledConfig(
    var rev: Any? = null,
    var vid: Long = 0,
    var id: Any? = null,
    var nw: Any? = null,
    var ap: Any? = null,
    var wifi: Any? = null,
    var hw: Any? = null,
    var light: Any? = null,
    var def: Any? = null,
    @JsonProperty("if")
    var iface: InterfaceConfig? = null,
    var ol: Any? = null,
    var timers: Any? = null,
    var ota: Any? = null,
    var um: Any? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InterfaceConfig(
        var sync: Any? = null,
        var nodes: Any? = null,
        var live: LiveConfig? = null,
        var va: Any? = null,
        var mqtt: Any? = null,
        var hue: Any? = null,
        var ntp: Any? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LiveConfig(
        var en: Boolean = false,
        var mso: Boolean = false,
        var rlm: Boolean = false,
        var port: Int = 0,
        var mc: Boolean = false,
        var dmx: DmxConfig? = null,
        var timeout: Int = 0,
        var maxbri: Boolean = false,
        @JsonProperty("no-gc")
        var noGc: Boolean = false,
        var offset: Int = 0,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DmxConfig(
        var uni: Int = 0,
        var seqskip: Boolean = false,
        var e131prio: Int = 0,
        var addr: Int = 0,
        var dss: Int = 0,
        var mode: Int = 0,
    )
}

