package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {
    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Utilities", "Other")

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            "Food" -> Icons.Default.Fastfood
            "Transport" -> Icons.Default.Commute
            "Shopping" -> Icons.Default.LocalGroceryStore
            "Entertainment" -> Icons.Default.Movie
            "Utilities" -> Icons.Default.Receipt
            else -> Icons.Default.RequestQuote
        }
    }

    fun getCategoryColor(category: String): Color {
        return when (category) {
            "Food" -> Color(0xFFE57373)
            "Transport" -> Color(0xFF64B5F6)
            "Shopping" -> Color(0xFF81C784)
            "Entertainment" -> Color(0xFFBA68C8)
            "Utilities" -> Color(0xFFFFB74D)
            else -> Color(0xFF90A4AE)
        }
    }
}
