package cl.belmontespa.myapplication.repository

import cl.belmontespa.myapplication.model.UsuarioEntity
import cl.belmontespa.myapplication.model.ViajeEntity

suspend fun SoporteLocal.usuario(correo: String): UsuarioEntity = checkNotNull(buscarUsuario(correo)) { "No existe $correo" }

suspend fun SoporteLocal.viaje(folio: String): ViajeEntity = checkNotNull(buscarViaje(folio)) { "No existe $folio" }
