package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitante")
public class Solicitante {
    @Id
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;
    @Column(name = "EMPRESA_id_empresa", nullable = false)
    private Long idEmpresa;
}
