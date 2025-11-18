package com.mundosvirtuales.telpoapp.domain.model

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun width() = right - left
    fun height() = bottom - top
}
