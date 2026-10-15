package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Extensión 1:1 de USUARIO, sin atributos propios. Se crea solo por migración Flyway.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administrador")
public class Administrador {

	@Id
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario")
	private Usuario usuario;

}
