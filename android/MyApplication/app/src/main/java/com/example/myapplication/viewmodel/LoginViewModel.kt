package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Usuario

class LoginViewModel : ViewModel() {

    private val usuarioValido = Usuario(
        mail = "admin",
        pass = "123"
    )

    // ESTADOS DE PANTALLA
    var mail by mutableStateOf("")
        private set

    var pass by mutableStateOf("")
        private set

    fun cambiarMail(nuevoMail: String) {
        mail = nuevoMail
    }

    fun cambiarPass(nuevaPass: String) {
        pass = nuevaPass
    }

    fun validarLogin(): Boolean {
        var validador = false

        if(mail == usuarioValido.mail && pass == usuarioValido.pass) {
            validador = true
        }

        return validador
    }
}