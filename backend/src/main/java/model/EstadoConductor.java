package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de estados propios del Conductor (ej. activo/inactivo de turno).
 * Nota: la "disponibilidad" para asignación (HU05: "sin viaje en curso") se calcula
 * en tiempo de consulta contra Viaje, no se guarda aquí; este catálogo es para el
 * estado administrativo de la cuenta del conductor, no para disponibilidad en vivo.
 */
@Entity
@Table(name = "estado_conductor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoConductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idEstado;

    @Column(name = "estado", length = 20, unique = true)
    private String estado;
}
