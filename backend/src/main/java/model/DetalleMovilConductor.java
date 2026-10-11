package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NUEVA (schema.sql actualizado 2026-10-10): asociativa M:N entre Movil y Conductor —
 * un vehículo puede ser usado por más de un conductor a lo largo del tiempo, y un
 * conductor puede usar más de un vehículo. Reemplaza la FK directa que antes tenía
 * Movil.conductor_id_usuario (relación 1:N).
 */
@Entity
@Table(name = "detalle_movil_conductor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleMovilConductor {

    @EmbeddedId
    private DetalleMovilConductorId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("movil")
    @JoinColumn(name = "id_movil", nullable = false)
    private Movil movil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("conductor")
    @JoinColumn(name = "id_usuario", nullable = false)
    private Conductor conductor;
}
