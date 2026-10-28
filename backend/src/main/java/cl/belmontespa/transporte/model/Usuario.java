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
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "nombre", length = 120, nullable = false)
	private String nombre;

	@Column(name = "correo", length = 150, nullable = false, unique = true)
	private String correo;

	@Column(name = "telefono", length = 20)
	private String telefono;

	@Column(name = "password_hash", length = 200, nullable = false)
	private String passwordHash;

	@Column(name = "password_temporal_hash", length = 200)
	private String passwordTemporalHash;

	@Column(name = "password_temporal_expira")
	private LocalDateTime passwordTemporalExpira;

	@Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	@Column(name = "token_fcm", length = 255, unique = true)
	private String tokenFcm;

}
