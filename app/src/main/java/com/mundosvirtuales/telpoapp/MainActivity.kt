package com.mundosvirtuales.telpoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mundosvirtuales.telpoapp.ui.CameraScreen
import com.mundosvirtuales.telpoapp.ui.CameraViewModel
import com.mundosvirtuales.telpoapp.ui.CameraViewModelFactory
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor

class MainActivity : ComponentActivity(), CameraDialogParent {

    private lateinit var cameraManager: CameraManager

    private val viewModel: CameraViewModel by viewModels {
        CameraViewModelFactory(cameraManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Crear el Manager
        cameraManager = CameraManager(this)

        // 2. Registrar el Manager al ciclo de vida
        lifecycle.addObserver(cameraManager)

        // 3. Setear el contenido de Compose
        setContent {
            // Aquí iría tu Composable principal
            // (Usando el 'CameraScreen' de tu pregunta anterior)

            // Asegúrate de que tu tema de Material esté aplicado
            // MaterialTheme {
            CameraScreen(viewModel = viewModel)
            // }
        }
    }

    // --- Implementación obligatoria de CameraDialogParent ---

    override fun getUSBMonitor(): USBMonitor? {
        return cameraManager.getUSBMonitor()
    }

    override fun onDialogResult(canceled: Boolean) {
        // Pasa el resultado al ViewModel/Manager
        viewModel.onDialogResult(canceled)
    }
}
