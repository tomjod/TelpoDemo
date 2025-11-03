package com.mundosvirtuales.telpoapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mundosvirtuales.telpoapp.R
import com.serenegiant.widget.CameraViewInterface


/**
 * Composable principal que hostea toda la pantalla.
 * Recibe el ViewModel que contiene toda la lógica y el estado.
 */
@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    // Observa el CameraState del ViewModel.
    // Cada vez que el 'state' cambia, esta pantalla se recompone.
    val state by viewModel.state.collectAsState()

    // Box permite superponer elementos (UI de controles encima del Preview)
    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. Vista de la Cámara ---
        // El 'AndroidView' que renderiza la cámara de la librería serenegiant
        CameraPreview(
            viewModel = viewModel,
            onLongPress = { viewModel.onCaptureStill() } // Evento de pulsación larga
        )

        // --- 2. Controles de la UI ---
        // La capa de botones y sliders
        CameraOverlay(
            state = state,
            onCameraToggle = { viewModel.onToggleCamera(it) },
            onRecordToggle = { viewModel.onToggleRecording() },
            onBrightnessClick = { viewModel.onBrightnessClick() },
            onContrastClick = { viewModel.onContrastClick() },
            onResetClick = { viewModel.onResetClick() },
            onBrightnessChange = { viewModel.onBrightnessChange(it) },
            onContrastChange = { viewModel.onContrastChange(it) }
        )
    }
}

/**
 * Composable que envuelve la vista XML de la cámara (CameraViewInterface).
 * Este es el "puente" entre Compose y el sistema de Vistas clásico.
 */
@Composable
fun CameraPreview(
    viewModel: CameraViewModel,
    onLongPress: () -> Unit
) {
    AndroidView(
        // factory: Se llama UNA VEZ para crear la vista
        factory = { context ->
            // Inflamos un layout XML simple que SÓLO contiene la vista de la cámara
            val view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.camera_view, null)

            // Encontramos la vista por su ID
            view
        },
        // update: Se llama cada vez que el Composable se recompone
        update = { cameraView ->
            // Este es el momento de pasar la vista al ViewModel (que se la dará al Manager)
            viewModel.setCameraView(cameraView as CameraViewInterface)
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { // Añade el detector de gestos
                detectTapGestures(onLongPress = { onLongPress() })
            }
    )
}

/**
 * Dibuja todos los controles (botones, sliders) sobre la cámara.
 * Es un Composable "tonto" que solo reacciona al 'state'.
 */
@Composable
fun CameraOverlay(
    state: CameraState,
    onCameraToggle: (Boolean) -> Unit,
    onRecordToggle: () -> Unit,
    onBrightnessClick: () -> Unit,
    onContrastClick: () -> Unit,
    onResetClick: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Controles Superiores (Switch y Grabar) ---
        TopControls(
            isCameraOn = state.isCameraOn,
            isRecording = state.isRecording,
            captureButtonVisible = state.captureButtonVisible,
            onCameraToggle = onCameraToggle,
            onRecordToggle = onRecordToggle
        )

        // Este Spacer empuja los controles inferiores al fondo
        Spacer(modifier = Modifier.weight(1f))

        // --- Controles Inferiores (Sliders y Botones) ---
        // Solo se muestran si la cámara está encendida
        if (state.toolsLayoutVisible) {
            BottomControls(
                state = state,
                onBrightnessClick = onBrightnessClick,
                onContrastClick = onContrastClick,
                onResetClick = onResetClick,
                onBrightnessChange = onBrightnessChange,
                onContrastChange = onContrastChange
            )
        }
    }
}

/**
 * Composable para los controles de la parte superior (Switch, Grabar)
 */
@Composable
fun TopControls(
    isCameraOn: Boolean,
    isRecording: Boolean,
    captureButtonVisible: Boolean,
    onCameraToggle: (Boolean) -> Unit,
    onRecordToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Switch de On/Off de la cámara
        Switch(
            checked = isCameraOn,
            onCheckedChange = onCameraToggle
        )

        // Botón de Grabar (círculo)
        if (captureButtonVisible) {
            IconButton(
                onClick = onRecordToggle,
                modifier = Modifier.size(48.dp)
            ) {
                // Un círculo rojo o gris para indicar el estado de grabación
                Surface(
                    shape = MaterialTheme.shapes.small, // Puedes usar CircleShape
                    color = if (isRecording) Color.Red else Color.Gray
                ) {
                    Box(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

/**
 * Composable para los controles de la parte inferior (Botones y Sliders)
 */
@Composable
fun BottomControls(
    state: CameraState,
    onBrightnessClick: () -> Unit,
    onContrastClick: () -> Unit,
    onResetClick: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fila de botones de control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (state.supportsBrightness) {
                Button(onClick = onBrightnessClick) {
                    Text("Brightness")
                }
            }

            if (state.supportsContrast) {
                Button(onClick = onContrastClick) {
                    Text("Contrast")
                }
            }
        }

        // Slider de Brillo (condicional)
        if (state.showBrightnessControl) {
            SettingSlider(
                title = "Brightness",
                value = state.brightnessValue,
                onValueChange = onBrightnessChange
            )
        }

        // Slider de Contraste (condicional)
        if (state.showContrastControl) {
            SettingSlider(
                title = "Contrast",
                value = state.contrastValue,
                onValueChange = onContrastChange
            )
        }
    }
}

/**
 * Un Composable reutilizable para un slider de configuración (Brillo, Contraste)
 */
@Composable
fun SettingSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title)
            Slider(
                value = value.toFloat(), // El Slider usa Float
                onValueChange = { onValueChange(it.toInt()) }, // Convertimos de vuelta a Int
                valueRange = 0f..255f // Rango de 0 a 255 (típico para esto)
            )
        }
    }
}

/**
 * Simula el layout de CameraScreen (un Box) con un fondo negro
 * donde iría el AndroidView de la cámara.
 */
@Composable
private fun PreviewScreenContainer(
    state: CameraState
) {
    // Es importante envolver en un Tema para que los colores
    // y tipografías (MaterialTheme) se apliquen correctamente.
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black) // Placeholder para el <AndroidView>
        ) {
            CameraOverlay(
                state = state,
                // En un preview, los eventos son lambdas vacías
                onCameraToggle = {},
                onRecordToggle = {},
                onBrightnessClick = {},
                onContrastClick = {},
                onResetClick = {},
                onBrightnessChange = {},
                onContrastChange = {}
            )
        }
    }
}

/**
 * Preview para el estado "Camera Off".
 * Solo debería mostrar el Switch de encendido.
 */
@Preview(name = "Camera Off", showBackground = true)
@Composable
private fun CameraOffPreview() {
    PreviewScreenContainer(
        state = CameraState(isCameraOn = false)
    )
}

/**
 * Preview para el estado "Camera On".
 * Muestra el botón de grabar y los botones de herramientas.
 */
@Preview(name = "Camera On (Not Recording)", showBackground = true)
@Composable
private fun CameraOnPreview() {
    PreviewScreenContainer(
        state = CameraState(
            isCameraOn = true,
            isRecording = false,
            captureButtonVisible = true,
            toolsLayoutVisible = true,
            supportsBrightness = true,
            supportsContrast = true
        )
    )
}

/**
 * Preview para el estado "Camera On" y "Recording".
 * Muestra el botón de grabar en color rojo.
 */
@Preview(name = "Camera On (Recording)", showBackground = true)
@Composable
private fun CameraRecordingPreview() {
    PreviewScreenContainer(
        state = CameraState(
            isCameraOn = true,
            isRecording = true, // <-- La diferencia
            captureButtonVisible = true,
            toolsLayoutVisible = true,
            supportsBrightness = true,
            supportsContrast = true
        )
    )
}

/**
 * Preview para el estado "Sliders Visible".
 * Muestra el slider de Brillo.
 */
@Preview(name = "Sliders Visible (Brightness)", showBackground = true)
@Composable
private fun CameraSlidersPreview() {
    PreviewScreenContainer(
        state = CameraState(
            isCameraOn = true,
            isRecording = false,
            captureButtonVisible = true,
            toolsLayoutVisible = true,
            supportsBrightness = true,
            supportsContrast = true,
            showBrightnessControl = true, // <-- Muestra el slider
            showContrastControl = false,
            brightnessValue = 128 // Mitad del slider
        )
    )
}
