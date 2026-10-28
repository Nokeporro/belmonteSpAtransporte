package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conductor")
public class Conductor {

	@Id
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario")
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_conductor_id_estado", nullable = false)
	private EstadoConductor estadoConductor;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

}
