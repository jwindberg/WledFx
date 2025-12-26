package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.*
import kotlin.random.Random

/**
 * Spinning Globe Animation
 * Simulates a rotating 3D earth using ray-casting/spherical projection and Perlin noise for terrain.
 */
class SpinningGlobeAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // Rotation state
    private var rotation: Double = 0.0
    private var cloudRotation: Double = 0.0
    
    // Constants
    private val radiusPercentage = 0.85 // Globe size relative to min dimension
    
    // Colors
    private val deepOcean = RgbColor(0, 5, 30)
    private val shallowOcean = RgbColor(0, 40, 100)
    private val landLow = RgbColor(30, 100, 20)
    private val landHigh = RgbColor(100, 80, 40)
    private val cloudColor = RgbColor(255, 255, 255)
    private val atmosphereColor = RgbColor(100, 150, 255)

    override fun getName(): String = "Spinning Globe"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true
    override fun supportsPalette(): Boolean = false
    override fun supportsColor(): Boolean = false
    
    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        rotation = 0.0
        cloudRotation = 0.0
        paramSpeed = 64 // Slower by default
    }

    override fun update(now: Long): Boolean {
        // Init buffer if needed (size change)
        if (!::pixelColors.isInitialized || pixelColors.size != width || pixelColors[0].size != height) {
             pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        }
        
        // Update rotation
        val speedFactor = paramSpeed / 128.0
        rotation += 0.02 * speedFactor
        cloudRotation += 0.025 * speedFactor // Clouds move slightly faster
        
        renderGlobe()
        return true
    }

    private fun renderGlobe() {
        val cx = width / 2.0 - 0.5
        val cy = height / 2.0 - 0.5
        val minDim = min(width, height)
        val globeRadius = (minDim / 2.0) * radiusPercentage
        
        // Stars background (static seed per frame? or noise?)
        // Let's rely on simple noise or just clear to black if inside loop
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Ray trace sphere
                val dx = x - cx
                val dy = y - cy
                val distSq = dx*dx + dy*dy
                
                if (distSq > globeRadius * globeRadius) {
                    // Background stars
                    // Use a hash of x,y to determine star presence
                    val hash = ((x * 12345 + y * 67890) xor 0x55555555)
                    if ((hash % 100) == 0) {
                         pixelColors[x][y] = RgbColor(100, 100, 100) // Dim star
                    } else {
                         pixelColors[x][y] = RgbColor.BLACK
                    }
                    continue
                }
                
                val z = sqrt(globeRadius * globeRadius - distSq)
                
                // Normal vector (normalized)
                val nx = dx / globeRadius
                val ny = dy / globeRadius
                val nz = z / globeRadius
                
                // 3D Point on surface for noise sampling
                // Rotate around Y axis (vertical axis on screen is Y, but we want to rotate around "Up" which is usually Y in 3D...
                // Wait, if we view from Z, Y is Up. Earth spins around Y axis.
                // So we rotate X and Z.
                
                val earthX = nx * cos(rotation) - nz * sin(rotation)
                val earthZ = nx * sin(rotation) + nz * cos(rotation)
                val earthY = ny
                
                // Sample Terrain Noise
                val noiseScale = 2.0
                // We use +100 to offset coordinate space
                val n = MathUtils.perlinNoise(earthX * noiseScale + 100.0, earthY * noiseScale, earthZ * noiseScale) 
                
                // Evaluate Terrain Color
                var color = if (n < 0.05) {
                     // Ocean (using noise range -1 to 1)
                     if (n < -0.3) deepOcean else shallowOcean
                } else {
                     // Land
                     if (n < 0.4) landLow else landHigh
                }
                
                // Polar Ice Caps?
                // If |ny| > 0.9 (near poles)
                if (abs(ny) > 0.9) {
                    // Blend to white
                    val iceFactor = ((abs(ny) - 0.9) * 10.0).coerceIn(0.0, 1.0)
                    color = ColorUtils.blend(color, RgbColor.WHITE, (iceFactor * 255).toInt())
                }
                
                // 2. Clouds
                val cloudX = nx * cos(cloudRotation) - nz * sin(cloudRotation)
                val cloudZ = nx * sin(cloudRotation) + nz * cos(cloudRotation)
                val cloudNoise = MathUtils.perlinNoise(cloudX * 2.5 + 500.0, earthY * 2.5, cloudZ * 2.5)
                
                if (cloudNoise > 0.2) {
                    val intensity = ((cloudNoise - 0.2) * 2.0).coerceIn(0.0, 1.0)
                    color = ColorUtils.blend(color, cloudColor, (intensity * 180).toInt())
                }
                
                // 3. Lighting (Diffuse + Ambient)
                // Light from top-left-front: (-1, -1, 1) normalized
                // Light vector L = normalized(-0.5, -0.5, 1.0) -> (-0.4, -0.4, 0.8) approx
                val dot = (nx * -0.4 + ny * -0.4 + nz * 0.82)
                val lightIntensity = (dot * 0.8 + 0.2).coerceIn(0.1, 1.0) // 0.2 ambient
                
                color = ColorUtils.scaleBrightness(color, lightIntensity)
                
                // Specular highlight on ocean
                if (n < 0.05 && dot > 0.0) {
                    // Reflection vector R = 2*(N.L)*N - L ... 
                    // Too complex, use blinn-phong half vector or just threshold near alignment
                    // If normal is pointing somewhat at light (dot > 0.9)
                    if (dot > 0.95) {
                        val spec = ((dot - 0.95) * 20.0).coerceIn(0.0, 1.0)
                        color = ColorUtils.blend(color, RgbColor.WHITE, (spec * 150).toInt())
                    }
                }
                
                // 4. Atmosphere Rim (Fresnel)
                // 1 - nz is factor (1 at edge, 0 at center)
                // Make it subtle
                val fresnel = (1.0 - nz).pow(4.0)
                if (fresnel > 0.0) {
                    color = ColorUtils.blend(color, atmosphereColor, (fresnel * 200).toInt().coerceIn(0, 255))
                }

                pixelColors[x][y] = color
            }
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
}
