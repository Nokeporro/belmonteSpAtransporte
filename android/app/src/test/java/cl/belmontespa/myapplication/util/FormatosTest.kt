package cl.belmontespa.myapplication.util

import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.ui.components.EstiloChip
import cl.belmontespa.myapplication.ui.components.EtiquetaCalculada
import cl.belmontespa.myapplication.ui.components.estiloChip
import cl.belmontespa.myapplication.ui.components.etiqueta
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Etiquetas y formatos de README (Textos y tema). */
class FormatosTest {

    @Test
    fun fechaYHoraEnListas() {
        assertEquals("24-09 · 14:30", Formatos.fechaHoraLista(LocalDateTime.of(2026, 9, 24, 14, 30)))
        assertEquals("01-01 · 00:05", Formatos.fechaHoraLista(LocalDateTime.of(2027, 1, 1, 0, 5)))
    }

    @Test
    fun fechaYHoraEnDetallesYPush() {
        assertEquals("24-09-2026 · 14:30", Formatos.fechaHoraDetalle(LocalDateTime.of(2026, 9, 24, 14, 30, 45)))
        assertEquals("31-12-2026 · 23:59", Formatos.fechaHoraDetalle(LocalDateTime.of(2026, 12, 31, 23, 59)))
    }

    @Test
    fun fechaSolaYHoraEn24Horas() {
        assertEquals("24-09-2026", Formatos.fecha(LocalDate.of(2026, 9, 24)))
        assertEquals("09:05", Formatos.hora(LocalDateTime.of(2026, 9, 24, 9, 5)))
        assertEquals("17:45", Formatos.hora(LocalDateTime.of(2026, 9, 24, 17, 45)))
    }

    @Test
    fun montoConPuntoDeMilesSinDecimales() {
        assertEquals("$48.000", Formatos.monto(48000))
        assertEquals("$0", Formatos.monto(0))
        assertEquals("$999", Formatos.monto(999))
        assertEquals("$1.000", Formatos.monto(1000))
        assertEquals("$1.234.567", Formatos.monto(1234567))
    }

    @Test
    fun rutConPuntosYGuion() {
        assertEquals("76.543.210-3", Formatos.rut("76543210-3"))
        assertEquals("76.543.210-3", Formatos.rut("76.543.210-3"))
        assertEquals("15.234.567-4", Formatos.rut("152345674"))
        assertEquals("1.234.567-K", Formatos.rut("1234567-k"))
    }

    @Test
    fun telefonoNormalizadoEnGrupos() {
        assertEquals("+56 9 5555 0101", Formatos.telefono("+56955550101"))
        // Uno que no está normalizado se muestra tal cual, sin inventar dígitos.
        assertEquals("955550101", Formatos.telefono("955550101"))
    }

    @Test
    fun codigoDeAbordajeEnGruposDe4() {
        assertEquals("7F3A 9C21 B04E 58D6", Formatos.codigoAbordaje("7F3A9C21B04E58D6"))
    }

    @Test
    fun codigoDeAbordajeNormalizaEspaciosYMayusculas() {
        val entradas = listOf(
            "7F3A 9C21 B04E 58D6",
            "7f3a9c21b04e58d6",
            "  7f3a  9c21 b04e 58d6  ",
        )

        entradas.forEach { entrada ->
            assertEquals(
                "7F3A 9C21 B04E 58D6",
                Formatos.codigoAbordaje(entrada),
            )
        }
    }

    @Test
    fun cadaEstadoTieneElEstiloDeSuChip() {
        val esperado = mapOf(
            EstiloChip.NEUTRO to listOf(EstadoViaje.PENDIENTE.estiloChip, EstadoPago.PENDIENTE.estiloChip, EtiquetaCalculada.POR_ABORDAR.estiloChip),
            EstiloChip.INFORMACION to listOf(EstadoViaje.ASIGNADO.estiloChip),
            EstiloChip.LLENO to listOf(EstadoViaje.EN_CURSO.estiloChip, EstadoConductor.OCUPADO.estiloChip, EstadoMovil.EN_USO.estiloChip),
            EstiloChip.TENUE to listOf(EstadoViaje.FINALIZADO.estiloChip, EtiquetaCalculada.INACTIVO.estiloChip),
            EstiloChip.CORRECTO to listOf(
                EstadoPago.PAGADO.estiloChip,
                EstadoConductor.DISPONIBLE.estiloChip,
                EstadoMovil.DISPONIBLE.estiloChip,
                EtiquetaCalculada.ABORDADO.estiloChip,
            ),
            EstiloChip.PELIGRO to listOf(EstadoViaje.CANCELADO.estiloChip, EstadoPago.RECHAZADO.estiloChip),
            EstiloChip.PELIGRO_BORDE to listOf(EstadoPago.ANULADO.estiloChip, EtiquetaCalculada.VENCIDO.estiloChip),
            EstiloChip.AVISO to listOf(
                EstadoPago.REEMBOLSADO.estiloChip,
                EtiquetaCalculada.ATRASADO.estiloChip,
                EtiquetaCalculada.PENDIENTE_SINCRONIZAR.estiloChip,
                EstadoSolicitante.PENDIENTE.estiloChip,
            ),
        )
        esperado.forEach { (estilo, obtenidos) ->
            obtenidos.forEach { assertEquals(estilo, it) }
        }
    }

    @Test
    fun cadaCodigoTieneUnaEtiquetaDistintaDentroDeSuGrupo() {
        val grupos = listOf(
            EstadoViaje.entries.map { it.etiqueta },
            EstadoPago.entries.map { it.etiqueta },
            EstadoConductor.entries.map { it.etiqueta },
            EstadoMovil.entries.map { it.etiqueta },
            EstadoSolicitante.entries.map { it.etiqueta },
            MedioPago.entries.map { it.etiqueta },
            TipoRecibo.entries.map { it.etiqueta },
            TipoDocumento.entries.map { it.etiqueta },
            Rol.entries.map { it.etiqueta },
            EtiquetaCalculada.entries.map { it.etiqueta },
        )
        grupos.forEach { etiquetas ->
            assertEquals(etiquetas.size, etiquetas.toSet().size)
        }
    }
}
