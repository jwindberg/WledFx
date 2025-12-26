package com.marsraver.wledfx

import com.marsraver.wledfx.animation.LedAnimation
import com.marsraver.wledfx.animation.*
import com.marsraver.wledfx.animation.NoneAnimation
import com.marsraver.wledfx.animation.SpinningGlobeAnimation

/**
 * Registry for creating animation instances and managing animation metadata.
 */
object AnimationRegistry {
    
    private val factories: Map<String, () -> LedAnimation> = mapOf(
        "None" to { NoneAnimation() },
        "Akemi" to { AkemiAnimation() },
        "Android" to { AndroidAnimation() },
        "Aquarium" to { AquariumAnimation() },
        "Aurora" to { AuroraAnimation() },
        "Black Hole" to { BlackHoleAnimation() },
        "Blends" to { BlendsAnimation() },
        "Blink" to { BlinkAnimation() },
        "Blobs" to { BlobsAnimation() },
        "Blurz" to { BlurzAnimation() },
        "BPM" to { BpmAnimation() },
        "Bouncing Ball" to { BouncingBallAnimation() },
        "Breathe" to { BreatheAnimation() },
        "Candle" to { CandleAnimation() },
        "Chase Rainbow" to { ChaseRainbowAnimation() },
        "Color Wipe" to { ColorWipeAnimation() },
        "ChunChun" to { ChunChunAnimation() },
        "Colored Bursts" to { ColoredBurstsAnimation() },
        "Confetti" to { ConfettiAnimation() },
        "Comet" to { CometAnimation() },
        "Crazy Bees" to { CrazyBeesAnimation() },
        "DNA" to { DnaAnimation() },
        "DNA Spiral" to { DnaSpiralAnimation() },
        "Distortion Waves" to { DistortionWavesAnimation() },
        "Dissolve" to { DissolveAnimation() },
        "DJLight" to { DjLightAnimation() },
        "Drift" to { DriftAnimation() },
        "Drift Rose" to { DriftRoseAnimation() },
        "Fireworks" to { FireworksAnimation() },
        "Fireflies" to { FirefliesAnimation() },
        "Fire 2012" to { Fire2012Animation() },
        "Fire 2012 2D" to { Fire2012_2DAnimation() },
        "FireNoise2D" to { FireNoise2DAnimation() },
        "Noise 1" to { Noise1Animation() },
        "Noise 2" to { Noise2Animation() },
        "Noise 3" to { Noise3Animation() },
        "Noise 4" to { Noise4Animation() },
        "Fill Noise 8" to { FillNoise8Animation() },
        "Frizzles" to { FrizzlesAnimation() },
        "Funky Plank" to { FunkyPlankAnimation() },
        "FreqMap" to { FreqMapAnimation() },
        "FreqMatrix" to { FreqMatrixAnimation() },
        "FreqWave" to { FreqWaveAnimation() },
        "GEQ" to { GeqAnimation() },
        "Game Of Life" to { GameOfLifeAnimation() },
        "GhostRider" to { GhostRiderAnimation() },
        "GravCenter" to { GravCenterAnimation() },
        "GravCentric" to { GravCentricAnimation() },
        "GravFreq" to { GravFreqAnimation() },
        "GravMeter" to { GravMeterAnimation() },
        "Hiphotic" to { HiphoticAnimation() },
        "Julia" to { JuliaAnimation() },
        "Juggle" to { JuggleAnimation() },
        "Juggles" to { JugglesAnimation() },
        "Larson Scanner" to { LarsonScannerAnimation() },
        "Dual Larson Scanner" to { DualLarsonScannerAnimation() },
        "ICU" to { ICUAnimation() },
        "Lake" to { LakeAnimation() },
        "Lightning" to { LightningAnimation() },
        "Lissajous" to { LissajousAnimation() },
        "Matrix" to { MatrixAnimation() },
        "Matripix" to { MatripixAnimation() },
        "MetaBalls" to { MetaBallsAnimation() },
        "Meteor" to { MeteorAnimation() },
        "Noise2D" to { Noise2DAnimation() },
        "Octopus" to { OctopusAnimation() },
        "Pacifica" to { PacificaAnimation() },
        "PixelWave" to { PixelWaveAnimation() },
        "Plasmoid" to { PlasmoidAnimation() },
        "PlasmaBall2D" to { PlasmaBall2DAnimation() },
        "PlasmaRotoZoom" to { PlasmaRotoZoomAnimation() },
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
        "Running Lights" to { RunningLightsAnimation() },
        "Scan" to { ScanAnimation() },
        "Sweep Random" to { SweepRandomAnimation() },
        "Multi Comet" to { MultiCometAnimation() },
        "Noise 2D" to { Noise2DAnimation() },
        "Scrolling Text" to { ScrollingTextAnimation() },
        "SinDots" to { SinDotsAnimation() },
        "Snake" to { SnakeAnimation() },
        "Snow" to { SnowAnimation() },
        "Soap" to { SoapAnimation() },
        "Solid" to { SolidAnimation() },
        "Space Ships" to { SpaceShipsAnimation() },
        "Drip" to { DripAnimation() },
        "Popcorn" to { PopcornAnimation() },
        "Spinning Globe" to { SpinningGlobeAnimation() },
        "Starburst" to { StarburstAnimation() },
        "Sparkle" to { SparkleAnimation() },
        "Spectrogram" to { SpectrogramAnimation() },
        "Sinelon" to { SinelonAnimation() },
        "Spectrum Analyzer" to { SpectrumAnalyzerAnimation() },
        "Square Swirl" to { SquareSwirlAnimation() },
        "Strobe" to { StrobeAnimation() },
        "Sun Radiation" to { SunRadiationAnimation() },
        "Sunrise" to { SunriseAnimation() },
        "Swirl" to { SwirlAnimation() },
        "Tartan" to { TartanAnimation() },
        "Tetrix" to { TetrixAnimation() },
        "Theater Chase" to { TheaterChaseAnimation() },
        "Thunderstorm" to { ThunderstormAnimation() },
        "Twinkle" to { TwinkleAnimation() },
        "Twinkle Random" to { TwinkleRandomAnimation() },
        "Twinkle Fade" to { TwinkleFadeAnimation() },
        "Flash Sparkle" to { FlashSparkleAnimation() },
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

