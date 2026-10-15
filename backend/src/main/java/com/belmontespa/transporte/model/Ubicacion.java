package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ubicacion")
public class Ubicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion", nullable = false)
    private Long idUbicacion;
    @Column(name = "direccion", length = 200, nullable = false)
    private String direccion;
    @Column(name = "id_comuna", nullable = false)
    private Long idComuna;
    @Column(name = "latitud", nullable = false)
    private BigDecimal latitud;
    @Column(name = "longitud", nullable = false)
    private BigDecimal longitud;
}
