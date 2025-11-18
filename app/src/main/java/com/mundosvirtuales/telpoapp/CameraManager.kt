package com.mundosvirtuales.telpoapp

// CameraManager.kt
import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mundosvirtuales.telpoapp.ui.CameraState
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.widget.CameraViewInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Re-usa tus constantes
private const val PREVIEW_WIDTH = 640
private const val PREVIEW_HEIGHT = 480
private const val PREVIEW_MODE = 1
private const val USE_SURFACE_ENCODER = false
private const val TAG = "CameraManager"


/**
 * Esta clase maneja toda la lógica de USBMonitor y UVCCameraHandler.
 * Es un observador del ciclo de vida para registrarse y liberarse automáticamente.
 */
class CameraManager(private val context: Context) : DefaultLifecycleObserver {

    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private var mUSBMonitor: USBMonitor? = null
    private var mCameraHandler: UVCCameraHandler? = null
    private var mUVCCameraView: CameraViewInterface? = null

    // --- Estado Interno Explotado como Flow ---
    private val _uiState = MutableStateFlow(CameraState())
    val uiState = _uiState.asStateFlow()

    // --- 1. Inicialización y Ciclo de Vida ---

    override fun onCreate(owner: LifecycleOwner) {
        if (isDebuggable) Log.v(TAG, "onCreate:")
        mUSBMonitor = USBMonitor(context, mOnDeviceConnectListener)
        // El mCameraHandler se crea CUANDO la vista esté lista
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isDebuggable) Log.v(TAG, "onStart:")
        mUSBMonitor?.register()
        mUVCCameraView?.onResume()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (isDebuggable) Log.v(TAG, "onStop:")
        mCameraHandler?.close()
        mUVCCameraView?.onPause()
        _uiState.update { it.copy() } // Actualiza estado
        mUSBMonitor?.unregister()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (isDebuggable) Log.v(TAG, "onDestroy:")
        mCameraHandler?.release()
        mCameraHandler = null
        mUSBMonitor?.destroy()
        mUSBMonitor = null
    }

    /**
     * Paso clave: La UI de Compose nos pasa la vista cuando está lista.
     */
    fun setCameraView(view: CameraViewInterface) {
        if (isDebuggable) Log.v(TAG, "setCameraView (CameraViewInterface) Seteada")
        mUVCCameraView = view
        mUVCCameraView?.setAspectRatio((PREVIEW_WIDTH / PREVIEW_HEIGHT.toFloat()).toDouble())

        // Ahora que tenemos la vista, creamos el Handler
        mCameraHandler = UVCCameraHandler.createHandler(
            context as Activity?, mUVCCameraView,
            if (USE_SURFACE_ENCODER) 0 else 1,
            PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE
        )
    }

    // --- 2. Lógica de Control (API Pública) ---

    fun setCameraViewOnStart(usbMonitor: USBMonitor) {
        if (usbMonitor.isRegistered && usbMonitor.deviceList.isNotEmpty()) {

        }
    }
    fun getUSBMonitor(): USBMonitor? = mUSBMonitor

    fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            _uiState.update { it.copy() }
        }
    }

    fun toggleCamera(isChecked: Boolean) {
        if (isChecked && mCameraHandler?.isOpened == false) {
            CameraDialog.showDialog(context as MainActivity) // Requiere que la Activity sea el contexto
        } else {
            mCameraHandler?.close()
            _uiState.update { it.copy() }
        }
    }

    fun toggleRecording() {
        if (mCameraHandler?.isOpened != true) return
        // Aquí iría la lógica de permisos (checkPermission...)

        if (mCameraHandler?.isRecording == false) {
            mCameraHandler?.startRecording()
            _uiState.update { it.copy(isRecording = true,) }
        } else {
            mCameraHandler?.stopRecording()
            _uiState.update { it.copy() }
        }
    }

    fun captureStill() {
        if (mCameraHandler?.isOpened == true) {
            // Aquí iría la lógica de permisos (checkPermission...)
            mCameraHandler?.captureStill()
        }
    }

    // ... (Métodos para Brillo, Contraste, Reset) ...
    // fun setBrightness(value: Int) { mCameraHandler?.setValue(...) }
    // fun getBrightness(): Int { ... }

    // --- 3. Listeners Internos (Lógica de la librería) ---

    private fun startPreview() {
        val st = mUVCCameraView?.surfaceTexture ?: return
        try {
            mCameraHandler?.startPreview(Surface(st))
            _uiState.update { it.copy(
                captureButtonVisible = true,
                toolsLayoutVisible = true
            )}
            updateItems()
        } catch (e: Exception) {
            _uiState.update { it.copy() }
        }
    }

    private fun updateItems() {
        // Actualiza el estado con lo que soporta la cámara
        val isActive = mCameraHandler?.isOpened == true
        _uiState.update {
            it.copy(
                toolsLayoutVisible = isActive,
                supportsBrightness = mCameraHandler?.checkSupportFlag(UVCCamera.PU_BRIGHTNESS.toLong()) == true,
                supportsContrast = mCameraHandler?.checkSupportFlag(UVCCamera.PU_CONTRAST.toLong()) == true
            )
        }
    }

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) { /* ... */ }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            try {
                mCameraHandler?.open(ctrlBlock)
                startPreview()
                updateItems()
                _uiState.update { it.copy(isCameraOn = true,) } // ¡Importante!
            } catch (e: Exception) {
                _uiState.update { it.copy() }
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            mCameraHandler?.close()
            _uiState.update { it.copy() }
            updateItems()
        }

        override fun onDettach(device: UsbDevice) { /* ... */ }

        override fun onCancel(device: UsbDevice) {
            _uiState.update { it.copy() }
        }
    }
}
