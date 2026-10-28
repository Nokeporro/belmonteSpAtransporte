package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movil")
public class Movil {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movil", nullable = false)
	private Long idMovil;

	@Column(name = "patente", length = 6, nullable = false, unique = true)
	private String patente;

	@Column(name = "gps_serial")
	private Integer gpsSerial;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_movil_id_estado_mov", nullable = false)
	private EstadoMovil estadoMovil;

	@Column(name = "capacidad", nullable = false)
	private Integer capacidad;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@Column(name = "codigo_abordaje", length = 16, nullable = false, unique = true)
	private String codigoAbordaje;

}
