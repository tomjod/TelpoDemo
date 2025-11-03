package com.mundosvirtuales.telpoapp.ui

data class CameraState(
    val isCameraOn: Boolean = false,
    val isRecording: Boolean = false,
    val showBrightnessControl: Boolean = false,
    val showContrastControl: Boolean = false,
    val brightnessValue: Int = 0,
    val contrastValue: Int = 0,
    val supportsBrightness: Boolean = false,
    val supportsContrast: Boolean = false,
    val captureButtonVisible: Boolean = false,
    val toolsLayoutVisible: Boolean = false
)
