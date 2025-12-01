package com.marsraver.wledfx

import com.marsraver.wledfx.animation.LedAnimation
import com.marsraver.wledfx.animation.*

/**
 * Registry for creating animation instances and managing animation metadata.
 */
object AnimationRegistry {
    
    private val factories: Map<String, () -> LedAnimation> = mapOf(
        "Akemi" to { AkemiAnimation() },
        "Android" to { AndroidAnimation() },
        "Aurora" to { AuroraAnimation() },
        "Black Hole" to { BlackHoleAnimation() },
        "Blends" to { BlendsAnimation() },
        "Blobs" to { BlobsAnimation() },
        "Blurz" to { BlurzAnimation() },
        "BPM" to { BpmAnimation() },
        "Bouncing Ball" to { BouncingBallAnimation() },
        "Breathe" to { BreatheAnimation() },
        "Candle" to { CandleAnimation() },
        "ChunChun" to { ChunChunAnimation() },
        "Colored Bursts" to { ColoredBurstsAnimation() },
        "Crazy Bees" to { CrazyBeesAnimation() },
        "DNA" to { DnaAnimation() },
        "DNA Spiral" to { DnaSpiralAnimation() },
        "Distortion Waves" to { DistortionWavesAnimation() },
        "DJLight" to { DjLightAnimation() },
        "Drift" to { DriftAnimation() },
        "Drift Rose" to { DriftRoseAnimation() },
        "Frizzles" to { FrizzlesAnimation() },
        "GEQ" to { GeqAnimation() },
        "Starburst" to { StarburstAnimation() },
        "Funky Plank" to { FunkyPlankAnimation() },
        "Game Of Life" to { GameOfLifeAnimation() },
        "Gravcenter" to { GravcenterAnimation() },
        "Gravcntric" to { GravcntricAnimation() },
        "Gravfreq" to { GravfreqAnimation() },
        "Hiphotic" to { HiphoticAnimation() },
        "Julia" to { JuliaAnimation() },
        "Juggles" to { JugglesAnimation() },
        "Lightning" to { LightningAnimation() },
        "Lissajous" to { LissajousAnimation() },
        "Matrix" to { MatrixAnimation() },
        "Matripix" to { MatripixAnimation() },
        "MetaBalls" to { MetaBallsAnimation() },
        "Octopus" to { OctopusAnimation() },
        "PixelWave" to { PixelWaveAnimation() },
        "Plasmoid" to { PlasmoidAnimation() },
        "Polar Lights" to { PolarLightsAnimation() },
        "Puddles" to { PuddlesAnimation() },
        "Pulser" to { PulserAnimation() },
        "Rain" to { RainAnimation() },
        "Rainbow" to { RainbowAnimation() },
        "Ripple" to { RippleAnimation() },
        "Ripple Peak" to { RipplePeakAnimation() },
        "Ripple Rainbow" to { RippleRainbowAnimation() },
        "Rocktaves" to { RocktavesAnimation() },
        "RotoZoomer" to { RotoZoomerAnimation() },
        "Scrolling Text" to { ScrollingTextAnimation() },
        "SinDots" to { SinDotsAnimation() },
        "Snake" to { SnakeAnimation() },
        "Soap" to { SoapAnimation() },
        "Space Ships" to { SpaceShipsAnimation() },
        "Square Swirl" to { SquareSwirlAnimation() },
        "Sun Radiation" to { SunRadiationAnimation() },
        "Swirl" to { SwirlAnimation() },
        "Tartan" to { TartanAnimation() },
        "Tetrix" to { TetrixAnimation() },
        "TwinkleFox" to { TwinkleFoxAnimation() },
        "TwinkleUp" to { TwinkleUpAnimation() },
        "Washing Machine" to { WashingMachineAnimation() },
        "Waverly" to { WaverlyAnimation() },
        "Waving Cell" to { WavingCellAnimation() }
    )

    /**
     * Create an animation instance by name.
     *
     * @param name animation name
     * @return animation instance, or null if name not found
     */
    fun create(name: String): LedAnimation? {
        return factories[name]?.invoke()
    }

    /**
     * Get all available animation names in sorted order.
     *
     * @return sorted list of animation names
     */
    fun getNames(): List<String> {
        return factories.keys.sortedBy { it.lowercase() }
    }

    /**
     * Check if an animation name exists.
     *
     * @param name animation name
     * @return true if the animation exists
     */
    fun exists(name: String): Boolean {
        return factories.containsKey(name)
    }
}

