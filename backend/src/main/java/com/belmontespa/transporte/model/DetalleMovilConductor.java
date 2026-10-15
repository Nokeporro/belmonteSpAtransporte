package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@IdClass(DetalleMovilConductorId.class)
@Entity
@Table(name = "detalle_movil_conductor")
public class DetalleMovilConductor {
    @Id
    @Column(name = "id_movil", nullable = false)
    private Long idMovil;
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;
}
