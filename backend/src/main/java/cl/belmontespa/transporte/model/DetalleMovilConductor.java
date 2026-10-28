package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(DetalleMovilConductorId.class)
@Table(name = "detalle_movil_conductor")
public class DetalleMovilConductor {

	@Id
	@Column(name = "id_movil", nullable = false)
	private Long idMovil;

	@Id
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_movil", insertable = false, updatable = false)
	private Movil movil;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Conductor conductor;

}
