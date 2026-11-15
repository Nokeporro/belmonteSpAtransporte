package cl.belmontespa.myapplication.lugares

import android.content.Context
import android.content.Intent
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado

internal class ServicioDeLugaresFalso(override var disponible: Boolean = true) : ServicioDeLugares {
    val lugares = mutableMapOf<String, LugarCrudo>()
    val consultas = mutableListOf<String>()
    var resultado: ResultadoDeBusqueda = ResultadoDeBusqueda.Cancelado
    var fallo: ErrorApp? = null

    override fun intentDeBusqueda(contexto: Context): Intent = error("No se usa en las pruebas JVM.")

    override fun resultadoDeBusqueda(codigo: Int, datos: Intent?): ResultadoDeBusqueda = resultado

    override suspend fun obtener(placeId: String): Resultado<LugarCrudo> {
        consultas += placeId
        fallo?.let { return Resultado.Fallo(it) }
        return lugares[placeId]?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.NoEncontrado)
    }
}
