/*
package com.mundosvirtuales.telpo.utils

import android.content.Context
import android.graphics.*
import com.mundosvirtuales.telpo.domain.model.Product
import java.text.SimpleDateFormat
import java.util.*

class LabelFormatter(private val context: Context) {

    companion object {
        private const val PADDING = 20f
        private const val LINE_SPACING = 10f
    }

    fun createLabel(product: Product, width: Int): Bitmap {
        val height = calculateLabelHeight(product)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo blanco
        canvas.drawColor(Color.WHITE)

        var yPosition = PADDING

        // Título - Nombre del producto
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            Paint.setTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            product.name.uppercase(),
            width / 2f,
            yPosition + titlePaint.textSize,
            titlePaint
        )
        yPosition += titlePaint.textSize + LINE_SPACING * 2

        // Línea separadora
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        canvas.drawLine(PADDING, yPosition, width - PADDING, yPosition, linePaint)
        yPosition += LINE_SPACING * 2

        // Peso
        val weightPaint = Paint().apply {
            color = Color.BLACK
            textSize = 64f
            Paint.setTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            product.getFormattedWeight(),
            width / 2f,
            yPosition + weightPaint.textSize,
            weightPaint
        )
        yPosition += weightPaint.textSize + LINE_SPACING * 2

        // Precio si está disponible
        if (product.pricePerKg > 0) {
            val pricePaint = Paint().apply {
                color = Color.BLACK
                textSize = 56f
                Paint.setTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                product.getFormattedPrice(),
                width / 2f,
                yPosition + pricePaint.textSize,
                pricePaint
            )
            yPosition += pricePaint.textSize + LINE_SPACING

            // Precio por kg
            val priceKgPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "($${String.format("%,.0f", product.pricePerKg)}/kg)",
                width / 2f,
                yPosition + priceKgPaint.textSize,
                priceKgPaint
            )
            yPosition += priceKgPaint.textSize + LINE_SPACING * 2
        }

        // Línea separadora
        canvas.drawLine(PADDING, yPosition, width - PADDING, yPosition, linePaint)
        yPosition += LINE_SPACING * 2

        // Fecha y hora
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val datePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(
            "Fecha: ${dateFormat.format(Date(product.timestamp))}",
            PADDING,
            yPosition + datePaint.textSize,
            datePaint
        )
        yPosition += datePaint.textSize + LINE_SPACING

        // Confianza de detección
        canvas.drawText(
            "Confianza: ${String.format("%.1f%%", product.confidence * 100)}",
            PADDING,
            yPosition + datePaint.textSize,
            datePaint
        )

        return bitmap
    }

    private fun calculateLabelHeight(product: Product): Int {
        var height = PADDING * 2
        height += 48f + LINE_SPACING * 2 // Título
        height += LINE_SPACING * 2 // Línea
        height += 64f + LINE_SPACING * 2 // Peso

        if (product.pricePerKg > 0) {
            height += 56f + LINE_SPACING // Precio
            height += 32f + LINE_SPACING * 2 // Precio/kg
        }

        height += LINE_SPACING * 2 // Línea
        height += 28f * 2 + LINE_SPACING * 2 // Fecha y confianza
        height += PADDING

        return height.toInt()
    }

    fun createQRCodeLabel(product: Product, width: Int): Bitmap {
        // Implementar si se necesita código QR o código de barras
        // Requeriría biblioteca adicional como ZXing
        return createLabel(product, width)
    }
}*/
