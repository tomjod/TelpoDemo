package com.mundosvirtuales.telpoapp.domain.model

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val weight: Float, // en gramos
    val confidence: Float,
    val pricePerKg: Float = 0f, // precio por kg en CLP
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
) {
    fun getTotalPrice(): Float {
        return (weight / 1000f) * pricePerKg
    }

    @SuppressLint("DefaultLocale")
    fun getFormattedWeight(): String {
        return if (weight >= 1000) {
            String.format("%.2f kg", weight / 1000)
        } else {
            String.format("%.0f g", weight)
        }
    }

    @SuppressLint("DefaultLocale")
    fun getFormattedPrice(): String {
        return String.format("$%,.0f", getTotalPrice())
    }
}
