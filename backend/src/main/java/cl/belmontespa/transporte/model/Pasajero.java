package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pasajero")
public class Pasajero {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pasajero", nullable = false)
	private Long idPasajero;

	@Column(name = "dni_hash", length = 50, nullable = false, unique = true)
	private String dniHash;

	@Column(name = "nombre", length = 120, nullable = false)
	private String nombre;

}
