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

	// Nunca texto plano (RNF01).
	@Column(name = "password_hash", length = 200, nullable = false)
	private String passwordHash;

	// NULL: contraseña normal. Con valor: password_hash es temporal hasta ese instante (RF21).
	@Column(name = "password_temporal_expira")
	private LocalDateTime passwordTemporalExpira;

	// DEFAULT CURRENT_TIMESTAMP de la base de datos.
	@Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	// Se reemplaza en cada login (RF13).
	@Column(name = "token_fcm", length = 255)
	private String tokenFcm;

}
