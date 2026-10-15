package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movil")
public class Movil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movil", nullable = false)
    private Long idMovil;
    @Column(name = "patente", length = 6, nullable = false)
    private String patente;
    @Column(name = "gps_serial")
    private Long gpsSerial;
    @Column(name = "ESTADO_MOVIL_id_estado_mov", nullable = false)
    private Long estadoMovilIdEstadoMov;
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
}
