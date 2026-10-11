package model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "conductor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conductor {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_conductor_id_estado", nullable = false)
    private EstadoConductor estadoConductor;
}
