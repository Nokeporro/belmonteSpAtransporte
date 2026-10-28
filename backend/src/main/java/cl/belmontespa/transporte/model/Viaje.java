package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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

	@Column(name = "folio", length = 20, nullable = false, unique = true, insertable = false, updatable = false)
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movil_id_movil")
	private Movil movil;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pago_id")
	private Pago pago;

	@Column(name = "cantidad_pasajeros", nullable = false, insertable = false, updatable = false)
	private Integer cantidadPasajeros;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conductor_id_usuario")
	private Conductor conductor;

	@Column(name = "numero_vuelo", length = 20)
	private String numeroVuelo;

	@Column(name = "motivo", length = 200)
	private String motivo;

}
