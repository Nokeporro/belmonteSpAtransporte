package cl.belmontespa.myapplication.model


enum class EstadoViaje { PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO, CANCELADO }

enum class EstadoPago { PENDIENTE, PAGADO, RECHAZADO, ANULADO, REEMBOLSADO }

enum class EstadoConductor { DISPONIBLE, OCUPADO }

enum class EstadoMovil { DISPONIBLE, EN_USO }

enum class EstadoSolicitante { PENDIENTE, APROBADO, RECHAZADO }

enum class TipoDocumento { RUT, PASAPORTE }

enum class MedioPago { TRANSFERENCIA, TARJETA_DEBITO, TARJETA_CREDITO, EFECTIVO }

enum class TipoRecibo { BOLETA, FACTURA }
