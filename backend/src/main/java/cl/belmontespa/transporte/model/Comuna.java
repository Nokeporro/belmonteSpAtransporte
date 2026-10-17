package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comuna", uniqueConstraints = @UniqueConstraint(columnNames = { "nombre", "id_region" }))
public class Comuna {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_comuna", nullable = false)
	private Long idComuna;

	@Column(name = "nombre", length = 80, nullable = false)
	private String nombre;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_region", nullable = false)
	private Region region;

}
