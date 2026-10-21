package cl.belmontespa.myapplication.model

data class Movil(
    val patente: String,
    val capacidad: Int,
    val gpsSerial: Long?,
    val activo: Boolean,
    val estado: EstadoMovil,
    val codigoAbordaje: String,
)

data class NuevoMovil(
    val patente: String,
    val capacidad: Int,
    val gpsSerial: Long?,
)

/** Habilitación vehículo↔conductor, con el estado de ambos (`DetalleMovilConductorDTOResponse`). */
data class Habilitacion(
    val patente: String,
    val capacidad: Int,
    val estadoMovil: EstadoMovil,
    val activoMovil: Boolean,
    val correoConductor: String,
    val nombreConductor: String,
    val estadoConductor: EstadoConductor,
    val activoConductor: Boolean,
)
