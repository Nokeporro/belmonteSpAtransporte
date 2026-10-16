package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "viaje")
public class Viaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_viaje", nullable = false)
	private Long idViaje;

	// Identificador público y clave de ruta en la API. Inmutable.
	@Column(name = "folio", length = 20, nullable = false, unique = true, updatable = false)
	private String folio;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_solicitante", nullable = false)
	private Solicitante solicitante;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_origen", nullable = false)
	private Ubicacion origen;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_destino", nullable = false)
	private Ubicacion destino;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_estado", nullable = false)
	private EstadoSolicitud estado;

	// NULL hasta la asignación.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movil_id_movil")
	private Movil movil;

	// NULL si no hay pago.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pago_id")
	private Pago pago;

	// Mantenido por trigger desde DETALLE_VIAJE_PASAJERO; nunca lo escribe la aplicación.
	@Column(name = "cantidad_pasajeros", nullable = false, insertable = false, updatable = false)
	private Integer cantidadPasajeros;

	// DEFAULT CURRENT_TIMESTAMP de la base de datos. Inmutable.
	@Column(name = "fh_creacion", nullable = false, insertable = false, updatable = false)
	private LocalDateTime fhCreacion;

	@Column(name = "fh_agenda", nullable = false)
	private LocalDateTime fhAgenda;

	@Column(name = "fh_asignacion")
	private LocalDateTime fhAsignacion;

	@Column(name = "fh_inicio")
	private LocalDateTime fhInicio;

	@Column(name = "fh_fin")
	private LocalDateTime fhFin;

	// NULL hasta la asignación (RF05).
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conductor_id_usuario")
	private Conductor conductor;

	@Column(name = "numero_vuelo", length = 20)
	private String numeroVuelo;

	@PrePersist
	private void generarFolio() {
		if (folio == null || folio.isBlank()) {
			folio = "VJ" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
		}
	}

}
