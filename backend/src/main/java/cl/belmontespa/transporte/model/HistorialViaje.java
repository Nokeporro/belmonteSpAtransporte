package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// Solo lectura: lo llenan los triggers de VIAJE, nunca la aplicación.
@Getter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "historial_viaje")
public class HistorialViaje {

	@Id
	@Column(name = "id_historial", nullable = false)
	private Long idHistorial;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_viaje", nullable = false)
	private Viaje viaje;

	// Campo de VIAJE que cambió.
	@Column(name = "campo_modificado", length = 30, nullable = false)
	private String campoModificado;

	// Valor nuevo, como texto.
	@Column(name = "valor", length = 200, nullable = false)
	private String valor;

	@Column(name = "fecha_cambio", nullable = false)
	private LocalDateTime fechaCambio;

}
