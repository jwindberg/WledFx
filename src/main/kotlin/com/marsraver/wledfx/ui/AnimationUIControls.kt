package com.marsraver.wledfx.ui

import javafx.scene.control.*
import javafx.scene.paint.Color

/**
 * Container for all UI controls related to animation configuration.
 * Encapsulates the UI state and provides methods to update controls.
 */
class AnimationUIControls {
    val colorPicker: ColorPicker
    val colorLabel: Label
    val paletteComboBox: ComboBox<String>
    val paletteLabel: Label
    val brightnessSlider: Slider
    val brightnessLabel: Label
    val textInputLabel: Label
    val textInputField: TextField
    val textSpeedLabel: Label
    val textSpeedSlider: Slider
    val candleMultiCheckBox: CheckBox
    val twinkleCheckBox: CheckBox

    constructor(
        colorPicker: ColorPicker,
        colorLabel: Label,
        paletteComboBox: ComboBox<String>,
        paletteLabel: Label,
        brightnessSlider: Slider,
        brightnessLabel: Label,
        textInputLabel: Label,
        textInputField: TextField,
        textSpeedLabel: Label,
        textSpeedSlider: Slider,
        candleMultiCheckBox: CheckBox,
        twinkleCheckBox: CheckBox
    ) {
        this.colorPicker = colorPicker
        this.colorLabel = colorLabel
        this.paletteComboBox = paletteComboBox
        this.paletteLabel = paletteLabel
        this.brightnessSlider = brightnessSlider
        this.brightnessLabel = brightnessLabel
        this.textInputLabel = textInputLabel
        this.textInputField = textInputField
        this.textSpeedLabel = textSpeedLabel
        this.textSpeedSlider = textSpeedSlider
        this.candleMultiCheckBox = candleMultiCheckBox
        this.twinkleCheckBox = twinkleCheckBox
    }

    /**
     * Update color picker to show a specific RGB color.
     */
    fun setColor(r: Int, g: Int, b: Int) {
        colorPicker.value = Color.rgb(r, g, b)
    }

    /**
     * Get the current color from the color picker as RGB.
     */
    fun getColor(): Triple<Int, Int, Int> {
        val color = colorPicker.value
        return Triple(
            (color.red * 255).toInt().coerceIn(0, 255),
            (color.green * 255).toInt().coerceIn(0, 255),
            (color.blue * 255).toInt().coerceIn(0, 255)
        )
    }

    /**
     * Update palette combo box to show a specific palette.
     */
    fun setPalette(paletteName: String) {
        paletteComboBox.value = paletteName
    }

    /**
     * Get the current palette name from the combo box.
     */
    fun getPalette(): String? {
        return paletteComboBox.value
    }

    /**
     * Show/hide text input controls.
     */
    fun setTextInputVisible(visible: Boolean) {
        textInputLabel.isVisible = visible
        textInputLabel.isManaged = visible
        textInputField.isVisible = visible
        textInputField.isManaged = visible
    }

    /**
     * Show/hide text speed controls.
     */
    fun setTextSpeedVisible(visible: Boolean) {
        textSpeedLabel.isVisible = visible
        textSpeedLabel.isManaged = visible
        textSpeedSlider.isVisible = visible
        textSpeedSlider.isManaged = visible
    }

    /**
     * Show/hide candle multi checkbox.
     */
    fun setCandleMultiVisible(visible: Boolean) {
        candleMultiCheckBox.isVisible = visible
        candleMultiCheckBox.isManaged = visible
    }

    /**
     * Show/hide twinkle checkbox.
     */
    fun setTwinkleVisible(visible: Boolean) {
        twinkleCheckBox.isVisible = visible
        twinkleCheckBox.isManaged = visible
    }

    /**
     * Enable/disable color controls.
     */
    fun setColorEnabled(enabled: Boolean) {
        colorPicker.isDisable = !enabled
        colorLabel.isDisable = !enabled
    }

    /**
     * Enable/disable palette controls.
     */
    fun setPaletteEnabled(enabled: Boolean) {
        paletteComboBox.isDisable = !enabled
        paletteLabel.isDisable = !enabled
    }
}

