# WLED v0.15.3 Missing Animations Reference

**Last Updated:** 2025-12-13  
**WledFx Version:** 81 animations  
**WLED v0.15.3:** 187 effects  
**Coverage:** ~43% overall, ~70% of 2D effects, 100% of audio-reactive effects

---

## Summary

**Total Missing:** ~107 effects

### Breakdown by Category
- **1D Strip Effects:** ~80 (not useful for 2D matrices)
- **2D Matrix Effects:** ~8 (could be implemented)
- **Complex/Special Effects:** ~19 (mixed utility)

---

## Missing 1D Strip Effects (~80)

### Blink/Strobe Effects (8)
- ~~Blink~~ ✓ Implemented
- Blink Rainbow
- ~~Strobe~~ ✓ Implemented (variation of Blink)
- Strobe Rainbow
- Multi Strobe
- Flash Sparkle
- Hyper Sparkle
- Sparkle

### Chase/Running Effects (15)
- Chase Color
- Chase Flash
- Chase Flash Random
- ~~Chase Rainbow~~ ✓ Implemented
- Chase Rainbow White
- Chase Random
- Random Chase
- Running Color
- Running Dual
- ~~Running Lights~~ ✓ Implemented
- Running Random
- ~~Theater Chase~~ ✓ Implemented
- Theater Chase Rainbow
- Tricolor Chase
- Dual Scan

### Wipe/Sweep Effects (9)
- ~~Color Wipe~~ ✓ Implemented
- Color Wipe Random
- Color Sweep
- Color Sweep Random
- Tricolor Wipe
- Tricolor Fade
- Fade
- ~~Dissolve~~ ✓ Implemented
- Dissolve Random

### Simple Color Effects (18)
- ~~Solid~~ ✓ Implemented
- Solid Pattern
- Solid Pattern Tri
- Solid Glitter
- ~~Confetti~~ ✓ Implemented (Random fading spots)
- ~~Juggle~~ ✓ Implemented (Sine wave dots)
- ~~Sinelon~~ ✓ Implemented (Sine scanner with trails)
- Android
- ~~Breathe~~ ✓ Implemented (Enabled existing 2D effect for 1D)
- Fade 
- Sweep Random

### Scanner Effects (4)
- ~~Larson Scanner~~ ✓ Implemented (Knight Rider)
- ~~Dual Larson Scanner~~ ✓ Implemented (Opposing KITT)
- ~~Scan~~ ✓ Implemented
- ~~ICU~~ ✓ Implemented (Center-out scanner)

### Twinkle/Glitter Effects (7)
- ~~Twinkle~~ ✓ Implemented
- ~~Twinkle Random~~ ✓ Implemented (Color Variant)
- ~~Twinkle Fade~~ ✓ Implemented (Smooth variant)
- Twinkle Fade Random
- ~~Sparkle~~ ✓ Implemented
- ~~Flash Sparkle~~ ✓ Implemented (Strobe + Sparkles)
- Hyper Sparkle

### Other/Misc Patterns
- ~~Sweep Random~~ ✓ Implemented
- ~~Multi Comet~~ ✓ Implemented
- ~~Ripple~~ ✓ Implemented (Existing)
- ~~Solid~~ ✓ Implemented
- Android
- Flow
- Drift
- Fade
- Loop
- Wavesse

### Noise Effects (10)
- ~~Noise 1~~ ✓ Implemented (Palette Noise with 2D support)
- ~~Noise 2~~ ✓ Implemented (Cycling Palette Noise)
- ~~Noise 3~~ ✓ Implemented (Lava Lamp / Void Noise)
- ~~Noise 4~~ ✓ Implemented (Fire-like Noise)
- ~~FillNoise8~~ ✓ Implemented
- MidNoise
### Fire/Flame Effects (3)
- ~~Fire 2012~~ ✓ Implemented (Classic 1D)
- ~~Fire 2012 2D~~ ✓ Implemented (Vertical Matrix)
- Fire Flicker
- Fire 2021
- *(2DFireNoise implemented ✓)*

### Rainbow Effects (Existing 2D: Rainbow, Chase Rainbow)
- ~~Rainbow~~ ✓ Implemented (Rainbow Cycle)
- ~~Chase Rainbow~~ ✓ Implemented (Rainbow Runner)
- Rainbow Runner
- Rainbow Glabber
- Rainbow Swirl

### Meteor/Comet Effects (4)
- ~~Meteor~~ ✓ Implemented
- ~~Meteor Smooth~~ (merged into Meteor)
- ~~Comet~~ ✓ Implemented
- Multi Comet (requires particle system)

### Static/Utility Effects (10)
- Static Pattern
- Tri Static Pattern
- Palette
- Gradient
- Percent
- Loading
- Railway
- Traffic Light
- Halloween Eyes
- TV Simulator

### Wave/Oscillate Effects (8)
- ColorWaves
- SineWave
- WaveSins
- Oscillate
- Phased
- Saw
- Flow
- FlowStripe

### Other 1D Effects (12)
- Rainbow
- Rainbow Cycle
- Colorful
- Dynamic
- Dynamic Smooth
- Juggle (different from Juggles)
- Sinelon
- Sinelon Dual
- Sinelon Rainbow
- Two Dots
- Heartbeat
- Breath (different from Breathe)

---

## Missing 2D Matrix Effects (~8)

### High Priority
- ~~**Starburst**~~ ✓ Implemented (Physics based)
- ~~**Sunrise**~~ ✓ Implemented (Day/Night cycle)
- ~~**Drip**~~ ✓ Implemented (Physics based)

### Medium Priority
- **Dancing Shadows** - Shadow dance effect
- **Fairy** - Fairy light effect
- **Popcorn** - Popping particles
- **Pride 2015** - Pride colors effect
- **RollingBalls** - Rolling ball simulation

---

## Missing Complex/Special Effects (~19)

### Audio-Reactive (Non-2D)
- **FreqPixels** - Frequency-based pixel control

### Visual Effects
- **Exploding Fireworks** - Firework explosions
- **Plasma** - Classic plasma effect (1D)
- **Candle Multi** - Multiple candle flames
- **PuddlePeak** - Audio-reactive puddles with peaks
- **Ripple Rainbow** - Rainbow ripple variant
  - *(Note: Have Ripple and RipplePeak implemented)*

### Other
- Various palette and gradient variations
- Utility effects
- Experimental effects

---

## Implementation Notes

### Why These Are Missing

**1D Strip Effects:**
- Designed for linear LED strips
- Don't translate well to 2D matrices
- Would look repetitive or boring on a matrix
- Examples: Blink just flashes all LEDs, Chase moves a dot left-to-right

**2D Effects:**
- Some are minor variations of implemented effects
- Some have complex dependencies
- Some are experimental/niche

**Complex Effects:**
- High implementation complexity
- May require additional libraries
- Limited visual impact vs. effort

### What's Well Covered

✅ **Audio-Reactive:** 12/12 (100%)  
✅ **2D Matrix:** ~55/~75 (~70%)  
✅ **Visually Impressive:** All major effects  
✅ **User Favorites:** Pacifica, GhostRider, Plasma effects, Fire effects, Meteor, Comet

---

## Recommendations

### Don't Implement
- 1D Strip effects (80 effects) - Not useful for matrices
- Utility effects (Static Pattern, Loading, etc.)
- Simple variations (multiple blink/chase types)

### Could Implement (If Desired)
1. **Starburst** - Visually impressive
2. **Sunrise** - Ambient effect
3. **Drip** - Interesting physics
4. **RollingBalls** - Physics simulation
5. **Pride 2015** - Popular color effect

### Already Have Better Alternatives
- Have Ripple and RipplePeak (don't need Ripple Rainbow)
- Have TwinkleFox and TwinkleUp (don't need all twinkle variants)
- Have comprehensive fire effects (FireNoise2D)
- Have comprehensive noise effects (Noise2D)

---

## Current Implementation Status

### Fully Implemented Categories
- ✅ All audio-reactive effects
- ✅ All major 2D matrix effects
- ✅ All "Grav" family effects
- ✅ All "Freq" family effects
- ✅ Plasma family (Plasmoid, PlasmaBall2D, PlasmaRotoZoom)
- ✅ Fire family (FireNoise2D, Fireworks, Fireflies)
- ✅ Water family (Lake, Puddles, Ripple, RipplePeak)
- ✅ Meteor/Comet family (Meteor, Comet)

### Custom WledFx Animations (Not in WLED)
- Aquarium
- Fireflies
- Snake
- SpectrumAnalyzer
- Thunderstorm
- Custom Tetrix variant

---

## Conclusion

**WledFx has excellent coverage for 2D LED matrix displays.**

The 107 missing effects are primarily:
- Basic 1D strip patterns unsuitable for matrices (80 effects)
- Minor variations of implemented effects
- Utility/experimental effects

**You have all the visually impressive and useful effects for your 32x32 matrix setup!**
