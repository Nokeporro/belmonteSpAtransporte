package cl.belmontespa.myapplication.notificaciones

import android.content.Context
import cl.belmontespa.myapplication.R

object PresentadorDeAvisos {
    fun presentar(contexto: Context, aviso: AvisoPush) {
        AvisosAndroid.mostrar(contexto, aviso,
            aviso.titulo ?: contexto.getString(R.string.app_name), aviso.mensaje.orEmpty())
    }
}
