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

	@Column(name = "fh_abordaje")
	private LocalDateTime fhAbordaje;

}
