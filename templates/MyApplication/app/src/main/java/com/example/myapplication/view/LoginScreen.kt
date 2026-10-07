package com.example.myapplication.view

import androidx.collection.longIntMapOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Text(
            text = "Iniciar Sesión"
        )
        OutlinedTextField(
            value = loginViewModel.mail,
            onValueChange = {
                loginViewModel.cambiarMail(it)
            },
            label = {
                Text("Correo Electrónico")
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = loginViewModel.pass,
            onValueChange = {
                loginViewModel.cambiarPass(it)
            },
            label = {
                Text("Contraseña")
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation
            = PasswordVisualTransformation()
        )
        Button(
            onClick = {
                val validador = loginViewModel.validarLogin()

                if(validador)
                    onLoginSuccess()
                else
                    println("CREDENCIALES NO VÁLIDAS")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Presióname"
            )
        }

    }
}