package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vehículo de la flota. ACTUALIZADA (schema.sql 2026-10-10):
 * - Ya no tiene FK directa a Conductor (ver DetalleMovilConductor, relación M:N).
 * - gps_serial pasó de texto a número.
 * - Se agregó "capacidad" (cantidad de pasajeros que admite).
 * - MOVIL__UN quedó UNIQUE solo sobre "patente" (antes era compuesto con gps_serial).
 */
@Entity
@Table(name = "movil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movil")
    private Long idMovil;

    @Column(name = "patente", length = 6, nullable = false, unique = true)
    private String patente;

    @Column(name = "gps_serial")
    private Long gpsSerial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_movil_id_estado_mov", nullable = false)
    private EstadoMovil estadoMovil;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
}
