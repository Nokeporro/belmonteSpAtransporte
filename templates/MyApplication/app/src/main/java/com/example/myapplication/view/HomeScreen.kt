package com.example.myapplication.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen() {
    // () -> PROPIEDADES DEL COMPONENTE
    // {} -> CONTENIDO DEL COMPONENTE
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Bienvenid@ a Android"
        )
        Text(
            text = "Hola Fernando"
        )
        Text(
            text = "Hola Fernando"
        )
    }
}
