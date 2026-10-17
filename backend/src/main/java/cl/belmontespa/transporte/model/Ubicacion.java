package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ubicacion", uniqueConstraints = @UniqueConstraint(
		columnNames = { "direccion", "id_comuna", "latitud", "longitud" }))
public class Ubicacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_ubicacion", nullable = false)
	private Long idUbicacion;

	@Column(name = "direccion", length = 200, nullable = false)
	private String direccion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_comuna", nullable = false)
	private Comuna comuna;

	@Column(name = "latitud", precision = 9, scale = 6, nullable = false)
	private BigDecimal latitud;

	@Column(name = "longitud", precision = 9, scale = 6, nullable = false)
	private BigDecimal longitud;

}
