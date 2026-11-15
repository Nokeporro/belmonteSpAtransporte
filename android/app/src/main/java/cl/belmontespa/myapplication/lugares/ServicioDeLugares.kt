package cl.belmontespa.myapplication.lugares

import android.content.Context
import android.content.Intent
import cl.belmontespa.myapplication.model.Resultado

sealed interface ResultadoDeBusqueda {
    data class Elegido(val placeId: String) : ResultadoDeBusqueda

    data object Cancelado : ResultadoDeBusqueda

    data object Fallido : ResultadoDeBusqueda
}

interface ServicioDeLugares {
    val disponible: Boolean

    fun intentDeBusqueda(contexto: Context): Intent

    fun resultadoDeBusqueda(codigo: Int, datos: Intent?): ResultadoDeBusqueda

    suspend fun obtener(placeId: String): Resultado<LugarCrudo>
}
