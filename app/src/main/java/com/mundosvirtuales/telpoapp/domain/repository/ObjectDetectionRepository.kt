package com.mundosvirtuales.telpo.domain.repository

import android.graphics.Bitmap
import com.mundosvirtuales.telpo.data.detector.YoloDetector
import com.mundosvirtuales.telpoapp.domain.model.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ObjectDetectionRepository @Inject constructor(
    private val detector: YoloDetector
) {
    suspend fun detectObjects(bitmap: Bitmap): Result<List<DetectionResult>> = withContext(Dispatchers.Default) {
        try {
            detector.initialize()
            val results = detector.detect(bitmap)
            if (results.isEmpty()) {
                Result.failure(Exception("No se detectaron objetos"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        detector.close()
    }
}
