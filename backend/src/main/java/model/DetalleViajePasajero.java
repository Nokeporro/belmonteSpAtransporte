package model;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "detalle_viaje_pasajero")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleViajePasajero {

    @EmbeddedId
    private DetalleViajePasajeroId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("viaje")
    @JoinColumn(name = "id_viaje", nullable = false)
    private Viaje viaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("pasajero")
    @JoinColumn(name = "id_pasajero", nullable = false)
    private Pasajero pasajero;
}
