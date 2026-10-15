package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conductor")
public class Conductor {
    @Id
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;
    @Column(name = "ESTADO_CONDUCTOR_id_estado", nullable = false)
    private Long estadoConductorIdEstado;
}
