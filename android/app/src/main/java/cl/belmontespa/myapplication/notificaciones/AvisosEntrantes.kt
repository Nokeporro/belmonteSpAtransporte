package cl.belmontespa.myapplication.notificaciones

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AvisosEntrantes {
    private val avisos = MutableSharedFlow<AvisoPush>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val flujo: SharedFlow<AvisoPush> = avisos.asSharedFlow()

    fun publicar(aviso: AvisoPush) {
        avisos.tryEmit(aviso)
    }
}
