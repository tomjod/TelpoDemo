package com.mundosvirtuales.telpo.data.detector

import android.graphics.Bitmap
import com.mundosvirtuales.telpoapp.domain.model.DetectionResult

interface ObjectDetector {
    fun detect(bitmap: Bitmap): List<DetectionResult>
    fun close()
}
