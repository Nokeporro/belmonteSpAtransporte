package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "estado_solicitud")
public class EstadoSolicitud {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_estado", nullable = false)
	private Long idEstado;

	// PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO, CANCELADO.
	@Column(name = "nombre_estado", length = 20, nullable = false, unique = true)
	private String nombreEstado;

}
