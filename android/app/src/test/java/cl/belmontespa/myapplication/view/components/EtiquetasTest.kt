package cl.belmontespa.myapplication.view.components

import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import org.junit.Assert.assertEquals
import org.junit.Test

class EtiquetasTest {

    @Test
    fun conductorInactivoReemplazaSuEstado() {
        EstadoConductor.entries.forEach { estado ->
            assertEquals(
                R.string.etiqueta_inactivo,
                estado.etiqueta(activo = false),
            )
            assertEquals(
                EstiloChip.TENUE,
                estado.estiloChip(activo = false),
            )
        }
    }

    @Test
    fun movilInactivoReemplazaSuEstado() {
        EstadoMovil.entries.forEach { estado ->
            assertEquals(
                R.string.etiqueta_inactivo,
                estado.etiqueta(activo = false),
            )
            assertEquals(
                EstiloChip.TENUE,
                estado.estiloChip(activo = false),
            )
        }
    }

    @Test
    fun activosConservanSusEstados() {
        assertEquals(
            R.string.estado_conductor_ocupado,
            EstadoConductor.OCUPADO.etiqueta(activo = true),
        )
        assertEquals(
            EstiloChip.LLENO,
            EstadoConductor.OCUPADO.estiloChip(activo = true),
        )
        assertEquals(
            R.string.estado_movil_disponible,
            EstadoMovil.DISPONIBLE.etiqueta(activo = true),
        )
        assertEquals(
            EstiloChip.CORRECTO,
            EstadoMovil.DISPONIBLE.estiloChip(activo = true),
        )
    }
}
