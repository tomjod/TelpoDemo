package com.mundosvirtuales.telpo.data.detector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.mundosvirtuales.telpoapp.domain.model.BoundingBox
import com.mundosvirtuales.telpoapp.domain.model.DetectionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class YoloDetector @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ObjectDetector {

    companion object {
        private const val TAG = "YoloDetector"
        private const val MODEL_PATH = "yolo12s.tflite"
        private const val LABELS_PATH = "labels.txt"
        private const val INPUT_SIZE = 640
        private const val PIXEL_SIZE = 3
        private const val IMAGE_MEAN = 0f
        private const val IMAGE_STD = 255f
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.45f
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val gpuDelegate = GpuDelegate()
    private val isInitialized = AtomicBoolean(false)

    suspend fun initialize() {
        if (isInitialized.get()) return
        withContext(Dispatchers.IO) {
            try {
                setupInterpreter()
                loadLabels()
                isInitialized.set(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing detector: ${e.message}", e)
            }
        }
    }

    private fun setupInterpreter() {
        val options = Interpreter.Options().apply {
            addDelegate(gpuDelegate)
            setNumThreads(4)
            setUseNNAPI(true)
        }

        val model = loadModelFile()
        interpreter = Interpreter(model, options)

        Log.d(TAG, "Interpreter initialized successfully")
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels() {
        labels = context.assets.open(LABELS_PATH).bufferedReader().use { reader ->
            reader.readLines()
        }
        Log.d(TAG, "Loaded ${labels.size} labels")
    }

    override fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interpreter = this.interpreter ?: run {
            Log.e(TAG, "Interpreter is null")
            return emptyList()
        }

        try {
            // Preprocesar imagen
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

            // Preparar output buffers
            // YOLO12 output: [1, num_detections, 6] donde 6 = [x, y, w, h, confidence, class_id]
            val outputShape = interpreter.getOutputTensor(0).shape()
            val numDetections = outputShape[1]
            val outputBuffer = Array(1) { Array(numDetections) { FloatArray(6) } }

            // Ejecutar inferencia
            val startTime = System.currentTimeMillis()
            interpreter.run(inputBuffer, outputBuffer)
            val inferenceTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Inference time: ${inferenceTime}ms")

            // Post-procesar resultados
            val detections = mutableListOf<DetectionResult>()
            val scaleX = bitmap.width.toFloat() / INPUT_SIZE
            val scaleY = bitmap.height.toFloat() / INPUT_SIZE

            for (i in 0 until numDetections) {
                val detection = outputBuffer[0][i]
                val confidence = detection[4]

                if (confidence >= CONFIDENCE_THRESHOLD) {
                    val centerX = detection[0]
                    val centerY = detection[1]
                    val width = detection[2]
                    val height = detection[3]
                    val classId = detection[5].toInt()

                    // Convertir de centro-ancho-alto a esquinas
                    val left = (centerX - width / 2) * scaleX
                    val top = (centerY - height / 2) * scaleY
                    val right = (centerX + width / 2) * scaleX
                    val bottom = (centerY + height / 2) * scaleY

                    val label = if (classId in labels.indices) labels[classId] else "Unknown"

                    detections.add(
                        DetectionResult(
                            label = label,
                            confidence = confidence,
                            boundingBox = BoundingBox(left, top, right, bottom)
                        )
                    )
                }
            }

            // Aplicar Non-Maximum Suppression
            return applyNMS(detections)

        } catch (e: Exception) {
            Log.e(TAG, "Error during detection: ${e.message}", e)
            return emptyList()
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]

                // Normalizar RGB
                byteBuffer.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        return byteBuffer
    }

    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<DetectionResult>()

        for (detection in sortedDetections) {
            var shouldSelect = true

            for (selected in selectedDetections) {
                if (detection.label == selected.label) {
                    val iou = calculateIoU(detection.boundingBox, selected.boundingBox)
                    if (iou > IOU_THRESHOLD) {
                        shouldSelect = false
                        break
                    }
                }
            }

            if (shouldSelect) {
                selectedDetections.add(detection)
            }
        }

        Log.d(TAG, "Detected ${selectedDetections.size} objects after NMS")
        return selectedDetections
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val intersectionLeft = maxOf(box1.left, box2.left)
        val intersectionTop = maxOf(box1.top, box2.top)
        val intersectionRight = minOf(box1.right, box2.right)
        val intersectionBottom = minOf(box1.bottom, box2.bottom)

        if (intersectionRight < intersectionLeft || intersectionBottom < intersectionTop) {
            return 0f
        }

        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectionArea

        return intersectionArea / unionArea
    }

    override fun close() {
        try {
            interpreter?.close()
            gpuDelegate.close()
            interpreter = null
            Log.d(TAG, "Detector closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing detector: ${e.message}", e)
        }
    }
}
