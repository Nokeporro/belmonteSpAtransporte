package cl.belmontespa.myapplication.model

import java.time.LocalDateTime

/** Viaje ficticio para las pruebas JVM; cada prueba cambia solo lo que le importa. */
internal fun viajeDePrueba(
    fhAgenda: LocalDateTime,
    folio: String = "TR-1051",
    estado: EstadoViaje = EstadoViaje.PENDIENTE,
    cantidadPasajeros: Int = 1,
    nombreSolicitante: String = "Marcela Soto",
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
        correoConductor = null,
        nombreConductor = null,
        patenteMovil = null,
        hashPago = null,
        estadoPago = null,
        motivo = null,
        pasajeros = emptyList(),
    )
}
