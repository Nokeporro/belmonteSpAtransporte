package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.Usuario

class LoginViewModel: ViewModel() {

    // USUARIO CORRECTO PARA VALIDAR LOGIN
    val usuarioCorrecto = Usuario(
        "admin",
        "123"
    )

    // ESTADOS DE LA PANTALLA
    var mail by mutableStateOf("")
    var pass by mutableStateOf("")

    fun cambiarMail(nuevoMail: String) {
        mail = nuevoMail
    }

    fun cambiarPass(nuevaPass: String) {
        pass = nuevaPass
    }

    fun validarLogin(): Boolean {
        var validador = false

        if (mail == usuarioCorrecto.mail &&
            pass == usuarioCorrecto.pass) {
            validador = true
        }

        return validador
    }
}