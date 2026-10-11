package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de estados de un Movil (ej. Disponible, En mantención, Fuera de servicio).
 */
@Entity
@Table(name = "estado_movil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoMovil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_mov")
    private Long idEstadoMov;

    @Column(name = "nombre", length = 20, nullable = false, unique = true)
    private String nombre;
}
