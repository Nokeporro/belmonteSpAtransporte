package model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "estado_solicitud")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idEstado;

    @Column(name = "nombre_estado", length = 20, nullable = false, unique = true)
    private String nombreEstado;
}
