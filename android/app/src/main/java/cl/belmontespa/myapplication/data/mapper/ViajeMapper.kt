package cl.belmontespa.myapplication.data.mapper

import cl.belmontespa.myapplication.data.local.entity.ViajeConPasajeros
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.PasajeroViaje
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.util.FechaIso
import java.text.Collator
import java.util.Locale

// La conversión no modifica Room ni inventa datos ausentes.
fun ViajeConPasajeros.aModelo(): Viaje {
    val fila = viaje

    // Las relaciones de Room no garantizan el orden de los pasajeros.
    val orden = Collator.getInstance(Locale.forLanguageTag("es-CL")).apply {
        strength = Collator.PRIMARY
    }

    return Viaje(
        folio = fila.folio,
        estado = EstadoViaje.valueOf(fila.estado),
        correoSolicitante = fila.correoSolicitante,
        nombreSolicitante = fila.nombreSolicitante,
        telefonoSolicitante = fila.telefonoSolicitante,
        razonSocial = fila.razonSocial,
        origen = Ubicacion(
            direccion = fila.origenDireccion,
            comuna = fila.origenComuna,
            region = fila.origenRegion,
            latitud = fila.origenLatitud,
            longitud = fila.origenLongitud,
        ),
        destino = Ubicacion(
            direccion = fila.destinoDireccion,
            comuna = fila.destinoComuna,
            region = fila.destinoRegion,
            latitud = fila.destinoLatitud,
            longitud = fila.destinoLongitud,
        ),
        fhCreacion = FechaIso.leer(fila.fhCreacion),
        fhAgenda = FechaIso.leer(fila.fhAgenda),
        fhAsignacion = fila.fhAsignacion?.let(FechaIso::leer),
        fhInicio = fila.fhInicio?.let(FechaIso::leer),
        fhFin = fila.fhFin?.let(FechaIso::leer),
        numeroVuelo = fila.numeroVuelo,
        cantidadPasajeros = fila.cantidadPasajeros,
        correoConductor = fila.correoConductor,
        nombreConductor = fila.nombreConductor,
        patenteMovil = fila.patenteMovil,
        hashPago = fila.hashPago,
        estadoPago = fila.estadoPago?.let(EstadoPago::valueOf),
        motivo = fila.motivo,
        pasajeros = pasajeros
            .map {
                PasajeroViaje(
                    dniHash = it.detalle.dniHash,
                    nombre = it.pasajero.nombre,
                    fhAbordaje = it.detalle.fhAbordaje?.let(FechaIso::leer),
                )
            }
            .sortedWith(compareBy(orden) { it.nombre }),
    )
}
