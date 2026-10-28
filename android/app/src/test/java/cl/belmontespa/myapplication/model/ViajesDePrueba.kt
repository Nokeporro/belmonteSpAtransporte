package cl.belmontespa.myapplication.model

import java.time.LocalDateTime

internal fun viajeDePrueba(
    fhAgenda: LocalDateTime,
    folio: String = "TR-1051",
    estado: EstadoViaje = EstadoViaje.PENDIENTE,
    cantidadPasajeros: Int = 1,
    nombreSolicitante: String = "Marcela Soto",
    correoConductor: String? = null,
    patenteMovil: String? = null,
    pasajeros: List<PasajeroViaje> = emptyList(),
): Viaje {
    val lugar = Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana de Santiago", -33.4261, -70.611)
    return Viaje(
        folio = folio,
        estado = estado,
        correoSolicitante = "solicitante@ejemplo.cl",
        nombreSolicitante = nombreSolicitante,
        telefonoSolicitante = null,
        razonSocial = "Empresa",
        origen = lugar,
        destino = lugar,
        fhCreacion = fhAgenda.minusDays(1),
        fhAgenda = fhAgenda,
        fhAsignacion = null,
        fhInicio = null,
        fhFin = null,
        numeroVuelo = null,
        cantidadPasajeros = cantidadPasajeros,
        correoConductor = correoConductor,
        nombreConductor = correoConductor?.let { "Conductor" },
        patenteMovil = patenteMovil,
        hashPago = null,
        estadoPago = null,
        motivo = null,
        pasajeros = pasajeros,
    )
}

internal fun pasajeroDePrueba(nombre: String, fhAbordaje: LocalDateTime? = null) =
    PasajeroViaje(dniHash = "hash-$nombre", nombre = nombre, fhAbordaje = fhAbordaje)
