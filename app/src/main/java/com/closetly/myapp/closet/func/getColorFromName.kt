package com.closetly.myapp.closet.func

import androidx.compose.ui.graphics.Color

fun getColorFromName(name: String): Color {
    return when (name.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "blue" -> Color.Blue
        "red" -> Color.Red
        "green" -> Color.Green
        "gray" -> Color.Gray
        "beige" -> Color(0xFFF5F5DC)
        "yellow" -> Color.Yellow
        "orange" -> Color(0xFFFFA500)
        "violet" -> Color(0xFF8F00FF)
        "purple" -> Color(0xFF800080)
        else -> Color.LightGray
    }
}