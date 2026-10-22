package cl.belmontespa.myapplication.data.mapper

import cl.belmontespa.myapplication.data.local.DatosDemo
import cl.belmontespa.myapplication.data.local.entity.*
import cl.belmontespa.myapplication.util.FechaIso
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class ViajeMapperTest {
    private val semilla = DatosDemo.crear(LocalDateTime.of(2026, 10, 21, 12, 0))
    private fun relacion(folio: String): ViajeConPasajeros = ViajeConPasajeros(
        semilla.viajes.first { it.folio == folio },
        semilla.viajePasajeros.filter { it.folio == folio }.map { detalle ->
            PasajeroDeViaje(detalle, semilla.pasajeros.first { it.dniHash == detalle.dniHash })
        },
    )
    @Test fun convierteLaSemillaSinPerderPasajerosNiCoordenadas() {
        semilla.viajes.forEach { fila ->
            val modelo = relacion(fila.folio).aModelo()
            assertEquals(fila.cantidadPasajeros, modelo.pasajeros.size)
            assertEquals(fila.origenLatitud, modelo.origen.latitud, 0.0)
            assertEquals(fila.destinoLongitud, modelo.destino.longitud, 0.0)
            assertEquals(FechaIso.leer(fila.fhAgenda), modelo.fhAgenda)
            assertEquals(fila.estadoPago, modelo.estadoPago?.name)
        }
    }
    @Test fun conservaLasMarcasDeAbordaje() {
        val relacion = relacion("TR-1042")
        val modelo = relacion.aModelo()
        assertEquals(1, modelo.pasajeros.count { it.fhAbordaje != null })
        assertEquals(relacion.pasajeros.map { it.detalle.dniHash }.toSet(), modelo.pasajeros.map { it.dniHash }.toSet())
    }
    @Test fun admiteLaSolicitudSinPasajeros() { assertTrue(relacion("TR-1055").aModelo().pasajeros.isEmpty()) }
}