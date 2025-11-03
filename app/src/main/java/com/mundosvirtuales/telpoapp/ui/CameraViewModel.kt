package com.mundosvirtuales.telpoapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mundosvirtuales.telpoapp.CameraManager
import com.serenegiant.widget.CameraViewInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraManager: CameraManager
) : ViewModel() {

    // --- Estado de la UI ---
    // Tomamos el estado base del CameraManager
    private val cameraState = cameraManager.uiState

    // Y le añadimos estados puros de la UI (control de sliders)
    private val _showBrightnessControl = MutableStateFlow(false)
    private val _showContrastControl = MutableStateFlow(false)

    // Combinamos todos los flujos en un solo "CameraState" para la UI
    val state: StateFlow<CameraState> = combine(
        cameraState,
        _showBrightnessControl,
        _showContrastControl
    ) { cameraState, showBrightness, showContrast ->
        cameraState.copy(
            showBrightnessControl = showBrightness,
            showContrastControl = showContrast
            // Aquí también podrías poblar brightnessValue, etc.
            // brightnessValue = if (showBrightness) cameraManager.getBrightness() else 0
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CameraState())


    private var hideSettingsJob: Job? = null

    // --- Eventos de la UI ---

    fun setCameraView(view: CameraViewInterface) {
        cameraManager.setCameraView(view)
    }

    fun onToggleCamera(isChecked: Boolean) {
        cameraManager.toggleCamera(isChecked)
    }

    fun onToggleRecording() {
        cameraManager.toggleRecording()
    }

    fun onCaptureStill() {
        cameraManager.captureStill()
    }

    fun onBrightnessClick() {
        _showBrightnessControl.value = true
        _showContrastControl.value = false
        autoHideSettings()
    }

    fun onContrastClick() {
        _showBrightnessControl.value = false
        _showContrastControl.value = true
        autoHideSettings()
    }

    fun onResetClick() {
        // cameraManager.resetValue(...)
        hideSettings()
    }

    fun onBrightnessChange(value: Int) {
        // cameraManager.setBrightness(value)
        autoHideSettings() // Resetea el timer
    }

    fun onContrastChange(value: Int) {
        // cameraManager.setContrast(value)
        autoHideSettings() // Resetea el timer
    }

    private fun autoHideSettings() {
        hideSettingsJob?.cancel() // Cancela el timer anterior
        hideSettingsJob = viewModelScope.launch {
            delay(2500.toLong())
            hideSettings()
        }
    }

    private fun hideSettings() {
        _showBrightnessControl.value = false
        _showContrastControl.value = false
    }

    fun onDialogResult(canceled: Boolean) {
        cameraManager.onDialogResult(canceled)
    }
}

// Factory para inyectar el CameraManager en el ViewModel
class CameraViewModelFactory(private val cameraManager: CameraManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(cameraManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
