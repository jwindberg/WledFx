package com.marsraver.wledfx.wled

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.marsraver.wledfx.wled.model.WledConfig
import com.marsraver.wledfx.wled.model.WledInfo
import com.marsraver.wledfx.wled.model.WledState
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Client for interacting with a WLED device via REST API.
 */
class WledClient(private val ipAddress: String) {

    @Throws(Exception::class)
    fun getInfo(): WledInfo {
        val url = URI("http://$ipAddress/json/info").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2000  // Increased from 400ms to 2 seconds
        conn.readTimeout = 3000     // Increased from 800ms to 3 seconds
        conn.requestMethod = "GET"

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("Failed to get info: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val info = mapper.readValue(input, WledInfo::class.java)
            conn.disconnect()
            return info
        }
    }

    @Throws(Exception::class)
    fun getConfig(): WledConfig {
        val url = URI("http://$ipAddress/json/cfg").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2000  // Increased from 400ms to 2 seconds
        conn.readTimeout = 3000     // Increased from 800ms to 3 seconds
        conn.requestMethod = "GET"

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("Failed to get config: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val config = mapper.readValue(input, WledConfig::class.java)
            conn.disconnect()
            return config
        }
    }

    @Throws(Exception::class)
    fun getConfigJson(): JsonNode {
        val url = URI("http://$ipAddress/json/cfg").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2000  // Increased from 400ms to 2 seconds
        conn.readTimeout = 3000     // Increased from 800ms to 3 seconds
        conn.requestMethod = "GET"

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("Failed to get config json: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val node = mapper.readTree(input)
            conn.disconnect()
            return node
        }
    }

    @Throws(Exception::class)
    fun getState(): WledState {
        val url = URI("http://$ipAddress/json/state").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2000  // Increased from 400ms to 2 seconds
        conn.readTimeout = 3000     // Increased from 800ms to 3 seconds
        conn.requestMethod = "GET"

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("Failed to get state: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val state = mapper.readValue(input, WledState::class.java)
            conn.disconnect()
            return state
        }
    }

    @Throws(Exception::class)
    fun getEffects(): JsonNode {
        val url = URI("http://$ipAddress/json/eff").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2000
        conn.readTimeout = 3000
        conn.requestMethod = "GET"

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("Failed to get effects: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val node = mapper.readTree(input)
            conn.disconnect()
            return node
        }
    }

    fun getIpAddress(): String = ipAddress

    companion object {
        private val mapper = jacksonObjectMapper()
    }
}

