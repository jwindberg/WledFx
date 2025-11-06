package com.marsraver.WledFx.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

/**
 * Utility for exposing microphone data as coroutine-friendly flows.
 * Falls back to simulated audio when no microphone is available.
 */
object AudioPipeline {

    private const val DEFAULT_SAMPLE_RATE = 44_100f
    private const val DEFAULT_BUFFER_SIZE = 4096

    fun rmsFlow(
        sampleRate: Float = DEFAULT_SAMPLE_RATE,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        simulateWhenUnavailable: Boolean = true,
    ): Flow<AudioLevel> =
        pcmFlow(sampleRate, bufferSize, simulateWhenUnavailable)
            .map { frame ->
                val rms = computeRms(frame.samples)
                AudioLevel(rms, levelFromRms(rms))
            }
            .flowOn(Dispatchers.Default)
            .conflate()

    fun spectrumFlow(
        bands: Int = 16,
        sampleRate: Float = DEFAULT_SAMPLE_RATE,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        simulateWhenUnavailable: Boolean = true,
    ): Flow<AudioSpectrum> =
        pcmFlow(sampleRate, bufferSize, simulateWhenUnavailable)
            .map { frame ->
                AudioSpectrum(computeBands(frame.samples, bands))
            }
            .flowOn(Dispatchers.Default)
            .conflate()

    private fun pcmFlow(
        sampleRate: Float,
        bufferSize: Int,
        simulateWhenUnavailable: Boolean,
    ): Flow<PcmFrame> = callbackFlow {
        val format = AudioFormat(sampleRate, 16, 1, true, true)
        val info = javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format)

        if (AudioSystem.isLineSupported(info)) {
            val line = AudioSystem.getLine(info) as TargetDataLine
            val actualBufferSize = maxOf(bufferSize, line.bufferSize)
            line.open(format, actualBufferSize)
            line.start()
            val byteBuffer = ByteArray(bufferSize)
            val shortBuffer = ShortArray(bufferSize / 2)

            val reader = launch {
                while (isActive) {
                    val read = withContext(Dispatchers.IO) {
                        line.read(byteBuffer, 0, byteBuffer.size)
                    }
                    if (read > 0) {
                        val sampleCount = read / 2
                        convertBytesToShorts(byteBuffer, sampleCount, shortBuffer)
                        val frameSamples = shortBuffer.copyOf(sampleCount)
                        trySend(PcmFrame(frameSamples, System.nanoTime()))
                    }
                }
            }

            awaitClose {
                reader.cancel()
                line.stop()
                line.close()
            }
        } else if (simulateWhenUnavailable) {
            val simulator = launch {
                val sampleCount = bufferSize / 2
                var time = 0.0
                val increment1 = 2 * PI * 110 / sampleRate
                val increment2 = 2 * PI * 220 / sampleRate
                val increment3 = 2 * PI * 440 / sampleRate
                while (isActive) {
                    val samples = ShortArray(sampleCount)
                    for (i in 0 until sampleCount) {
                        val value = sin(time) * 0.6 +
                            sin(time * 0.5) * 0.3 +
                            sin(time * 1.5) * 0.2
                        samples[i] = (value * Short.MAX_VALUE * 0.7).roundToInt().toShort()
                        time += increment1
                    }
                    trySend(PcmFrame(samples, System.nanoTime()))
                    delay(16)
                }
            }
            awaitClose { simulator.cancel() }
        } else {
            close()
        }
    }.buffer(Channel.CONFLATED)

    private fun convertBytesToShorts(
        bytes: ByteArray,
        sampleCount: Int,
        target: ShortArray,
    ) {
        var byteIndex = 0
        for (i in 0 until sampleCount) {
            val msb = bytes[byteIndex++].toInt()
            val lsb = bytes[byteIndex++].toInt() and 0xFF
            target[i] = ((msb shl 8) or lsb).toShort()
        }
    }

    private fun computeRms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (sample in samples) {
            val normalized = sample / Short.MAX_VALUE.toDouble()
            sum += normalized * normalized
        }
        return sqrt(sum / samples.size)
    }

    private fun levelFromRms(rms: Double): Int =
        (rms * 255.0 * 2.5).roundToInt().coerceIn(0, 255)

    private fun computeBands(samples: ShortArray, bands: Int): IntArray {
        val result = IntArray(bands)
        if (samples.isEmpty()) return result
        val samplesPerBand = (samples.size / bands).coerceAtLeast(1)
        for (band in 0 until bands) {
            val start = band * samplesPerBand
            val end = min(samples.size, start + samplesPerBand)
            var sum = 0.0
            var count = 0
            for (i in start until end) {
                val normalized = samples[i] / Short.MAX_VALUE.toDouble()
                sum += normalized * normalized
                count++
            }
            val rms = if (count > 0) sqrt(sum / count) else 0.0
            result[band] = (rms * 255.0 * 3.2).roundToInt().coerceIn(0, 255)
        }
        return result
    }
}

data class PcmFrame(
    val samples: ShortArray,
    val timestampNanos: Long,
)

data class AudioLevel(
    val rms: Double,
    val level: Int,
)

data class AudioSpectrum(
    val bands: IntArray,
)

