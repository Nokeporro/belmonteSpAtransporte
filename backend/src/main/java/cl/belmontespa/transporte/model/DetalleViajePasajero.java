package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Pasajeros de un viaje (M:N). Cada inserción actualiza VIAJE.cantidad_pasajeros por trigger.
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(DetalleViajePasajeroId.class)
@Table(name = "detalle_viaje_pasajero")
public class DetalleViajePasajero {

	@Id
	@Column(name = "id_viaje", nullable = false)
	private Long idViaje;

	@Id
	@Column(name = "id_pasajero", nullable = false)
	private Long idPasajero;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_viaje", insertable = false, updatable = false)
	private Viaje viaje;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pasajero", insertable = false, updatable = false)
	private Pasajero pasajero;

}
