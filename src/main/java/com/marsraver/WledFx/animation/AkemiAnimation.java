package com.marsraver.WledFx.animation;

import javax.sound.sampled.*;
import java.util.Arrays;

/**
 * Akemi animation - renders a stylised character with audio-reactive elements and side GEQ bars
 * Based on WLED mode_2DAkemi
 */
public class AkemiAnimation implements LedAnimation {

    private static final int BASE_WIDTH = 32;
    private static final int BASE_HEIGHT = 32;

    // Akemi bitmap (32x32) - values indicate which palette entry to use
    private static final int[] AKEMI_MAP = new int[]{
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,2,2,3,3,3,3,3,3,2,2,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,2,3,3,0,0,0,0,0,0,3,3,2,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,2,3,0,0,0,6,5,5,4,0,0,0,3,2,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,0,6,6,5,5,5,5,4,4,0,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,3,2,0,6,5,5,5,5,5,5,5,5,5,5,4,0,2,3,0,0,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,7,7,5,5,5,5,7,7,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,5,1,1,5,5,5,5,1,1,5,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,7,7,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            1,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,2,
            0,2,2,2,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,2,2,2,0,
            0,0,0,3,2,0,0,0,6,5,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,2,2,0,0,0,
            0,0,0,3,2,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,2,3,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,3,0,3,3,0,0,3,3,0,3,3,0,0,0,0,2,2,0,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,2,0,3,2,0,0,3,2,0,3,2,0,0,0,0,2,3,0,0,0,0,
            0,0,0,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,2,3,0,0,0,0,0,
            0,0,0,0,0,3,2,2,2,2,0,0,0,3,2,0,0,3,2,0,0,0,3,2,2,2,3,0,0,0,0,0,
            0,0,0,0,0,0,3,3,3,0,0,0,0,3,2,0,0,3,2,0,0,0,0,3,3,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    };

    // Configuration parameters (mirroring WLED sliders)
    private int colorSpeed = 128;   // Equivalent to SEGMENT.speed
    private int intensity = 128;    // Controls dance activation threshold

    private int combinedWidth;
    private int combinedHeight;
    private int[][][] pixelColors;

    // Audio data
    private final int[] fftResult = new int[16];
    private TargetDataLine microphone;
    private byte[] audioBuffer;

    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelColors = new int[combinedWidth][combinedHeight][3];

        // Try to start microphone capture
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                audioBuffer = new byte[4096];
                System.out.println("Akemi: Microphone initialized successfully");
            } else {
                System.err.println("Akemi: Microphone not supported");
            }
        } catch (Exception e) {
            System.err.println("Akemi: Failed to initialize microphone: " + e.getMessage());
        }
    }

    private void updateFFT() {
        if (microphone != null && microphone.isOpen()) {
            try {
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    int samplesPerBand = Math.max(1, bytesRead / 2 / 16);
                    for (int band = 0; band < 16; band++) {
                        long sum = 0;
                        int count = 0;
                        int startIdx = band * samplesPerBand * 2;
                        int endIdx = Math.min(startIdx + samplesPerBand * 2, bytesRead);
                        for (int i = startIdx; i < endIdx; i += 2) {
                            int sample = (int) audioBuffer[i] | ((int) audioBuffer[i + 1] << 8);
                            sum += (long) sample * sample;
                            count++;
                        }
                        if (count > 0) {
                            double rms = Math.sqrt(sum / (double) count);
                            fftResult[band] = (int) Math.min(255, rms / 180.0);
                        } else {
                            fftResult[band] = Math.max(0, fftResult[band] - 5);
                        }
                    }
                } else {
                    decayFFT();
                }
            } catch (Exception e) {
                simulateFFT();
            }
        } else {
            simulateFFT();
        }
    }

    private void decayFFT() {
        for (int i = 0; i < fftResult.length; i++) {
            fftResult[i] = Math.max(0, fftResult[i] - 5);
        }
    }

    private void simulateFFT() {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 16; i++) {
            double phase = time / 90.0 + i * 0.55;
            double value = Math.sin(phase) * 50 + 60;
            value += Math.sin(phase * 2.1) * 25;
            value += Math.sin(phase * 0.6 + i) * 15;
            fftResult[i] = (int) Math.max(0, Math.min(180, value));
        }
    }

    private int map(int value, int inMin, int inMax, int outMin, int outMax) {
        if (inMax == inMin) return outMin;
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    private int constrain(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private int[] hsvToRgb(float h, float s, float v) {
        h = h % 360f;
        if (h < 0) h += 360f;
        int hi = (int) (h / 60.0f) % 6;
        float f = (h / 60.0f) - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (hi) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return new int[]{
                Math.round(r * 255),
                Math.round(g * 255),
                Math.round(b * 255)
        };
    }

    private int[] colorWheel(int pos) {
        pos = (pos % 256 + 256) % 256;
        if (pos < 85) {
            return new int[]{pos * 3, 255 - pos * 3, 0};
        } else if (pos < 170) {
            pos -= 85;
            return new int[]{255 - pos * 3, 0, pos * 3};
        } else {
            pos -= 170;
            return new int[]{0, pos * 3, 255 - pos * 3};
        }
    }

    private int[] multiplyColor(int[] rgb, float factor) {
        factor = clamp01(factor);
        return new int[]{
                Math.min(255, Math.round(rgb[0] * factor)),
                Math.min(255, Math.round(rgb[1] * factor)),
                Math.min(255, Math.round(rgb[2] * factor))
        };
    }

    private void setPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.max(0, Math.min(255, rgb[0]));
            pixelColors[x][y][1] = Math.max(0, Math.min(255, rgb[1]));
            pixelColors[x][y][2] = Math.max(0, Math.min(255, rgb[2]));
        }
    }

    @Override
    public boolean update(long now) {
        updateFFT();

        long timeMs = now / 1_000_000L;
        int speedFactor = (colorSpeed >> 2) + 2;
        int counter = (int) ((timeMs * speedFactor) & 0xFFFF);
        counter = counter >> 8;  // 0-255 range

        float lightFactor = 0.15f;
        float normalFactor = 0.4f;

        int[] soundColor = new int[]{255, 165, 0}; // Orange
        int[] armsAndLegsDefault = new int[]{0xFF, 0xE0, 0xA0};
        int[] eyeColor = new int[]{255, 255, 255};

        int[] faceColor = colorWheel(counter & 0xFF);
        int[] armsAndLegsColor = Arrays.copyOf(armsAndLegsDefault, 3);

        float base = fftResult[0] / 255.0f;
        boolean isDancing = intensity > 128 && fftResult[0] > 128;

        if (isDancing) {
            for (int x = 0; x < combinedWidth; x++) {
                setPixelColor(x, 0, new int[]{0, 0, 0});
            }
        }

        for (int y = 0; y < combinedHeight; y++) {
            int akY = Math.min(BASE_HEIGHT - 1, y * BASE_HEIGHT / combinedHeight);
            for (int x = 0; x < combinedWidth; x++) {
                int akX = Math.min(BASE_WIDTH - 1, x * BASE_WIDTH / combinedWidth);
                int ak = AKEMI_MAP[akY * BASE_WIDTH + akX];

                int[] color;
                switch (ak) {
                    case 3:
                        color = multiplyColor(armsAndLegsColor, lightFactor);
                        break;
                    case 2:
                        color = multiplyColor(armsAndLegsColor, normalFactor);
                        break;
                    case 1:
                        color = Arrays.copyOf(armsAndLegsColor, 3);
                        break;
                    case 6:
                        color = multiplyColor(faceColor, lightFactor);
                        break;
                    case 5:
                        color = multiplyColor(faceColor, normalFactor);
                        break;
                    case 4:
                        color = Arrays.copyOf(faceColor, 3);
                        break;
                    case 7:
                        color = Arrays.copyOf(eyeColor, 3);
                        break;
                    case 8:
                        if (base > 0.4f) {
                            float boost = clamp01(base);
                            color = new int[]{
                                    Math.min(255, Math.round(soundColor[0] * boost)),
                                    Math.min(255, Math.round(soundColor[1] * boost)),
                                    Math.min(255, Math.round(soundColor[2] * boost))
                            };
                        } else {
                            color = Arrays.copyOf(armsAndLegsColor, 3);
                        }
                        break;
                    default:
                        color = new int[]{0, 0, 0};
                        break;
                }

                if (isDancing) {
                    int targetY = Math.min(combinedHeight - 1, y + 1);
                    setPixelColor(x, targetY, color);
                } else {
                    setPixelColor(x, y, color);
                }
            }
        }

        int xMax = Math.max(1, combinedWidth / 8);
        int midY = combinedHeight / 2;
        int maxBarHeight = Math.max(1, 17 * combinedHeight / 32);

        for (int x = 0; x < xMax; x++) {
            int band = map(x, 0, Math.max(xMax, 4), 0, 15);
            band = constrain(band, 0, 15);
            int barHeight = map(fftResult[band], 0, 255, 0, maxBarHeight);
            barHeight = Math.min(maxBarHeight, Math.max(0, barHeight));

            int colorIndex = band * 35;
            int[] barColor = hsvToRgb((colorIndex % 256) * (360f / 255f), 1.0f, 1.0f);

            for (int y = 0; y < barHeight; y++) {
                int topY = midY - y;
                if (topY >= 0 && topY < combinedHeight) {
                    setPixelColor(x, topY, barColor);
                    int mirrorX = combinedWidth - 1 - x;
                    setPixelColor(mirrorX, topY, barColor);
                }
            }
        }

        return true;
    }

    @Override
    public int[] getPixelColor(int x, int y) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            return pixelColors[x][y].clone();
        }
        return new int[]{0, 0, 0};
    }

    @Override
    public String getName() {
        return "Akemi";
    }

    public void cleanup() {
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
            System.out.println("Akemi: Microphone closed");
        }
    }
}
