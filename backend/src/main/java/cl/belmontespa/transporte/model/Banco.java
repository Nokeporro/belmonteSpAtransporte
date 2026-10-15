package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "banco")
public class Banco {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "nombre", length = 50, nullable = false)
	private String nombre;

	@Column(name = "cod_sbif", nullable = false, unique = true)
	private Integer codSbif;

}
