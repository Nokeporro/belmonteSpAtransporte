package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "region")
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_region", nullable = false)
	private Long idRegion;

	@Column(name = "nombre", length = 60, nullable = false, unique = true)
	private String nombre;

}
