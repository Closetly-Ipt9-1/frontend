package com.m306.closetly.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Closetly",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Welcome to your wardrobe",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(onClick = { }) {
            Text("My Closet")
        }

        Button(onClick = { }) {
            Text("Explore Outfits")
        }
    }
}