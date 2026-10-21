package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Extensión 1:1 de USUARIO (RF01).
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitante")
public class Solicitante {

	@Id
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario")
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id_empresa", nullable = false)
	private Empresa empresa;

	// Pendiente de revisión, Aprobado o Rechazado (RF26).
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_solicitante_id", nullable = false)
	private EstadoSolicitante estadoSolicitante;

}
