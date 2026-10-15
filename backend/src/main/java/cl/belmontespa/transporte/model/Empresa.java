package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa")
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_empresa", nullable = false)
	private Long idEmpresa;

	@Column(name = "razon_social", length = 50, nullable = false)
	private String razonSocial;

	@Column(name = "rut", length = 15, nullable = false, unique = true)
	private String rut;

}
