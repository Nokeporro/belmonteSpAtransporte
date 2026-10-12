package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "estado_movil")
public class EstadoMovil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_mov", nullable = false)
    private Long idEstadoMov;
    @Column(name = "nombre", length = 20, nullable = false)
    private String nombre;
}
