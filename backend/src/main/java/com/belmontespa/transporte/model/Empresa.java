package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa", nullable = false)
    private Long idEmpresa;
    @Column(name = "razon_social", length = 50, nullable = false)
    private String razonSocial;
    @Column(name = "rut", length = 15, nullable = false)
    private String rut;
}
