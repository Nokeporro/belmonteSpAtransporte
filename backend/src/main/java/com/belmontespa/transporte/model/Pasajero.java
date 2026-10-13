package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pasajero")
public class Pasajero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pasajero", nullable = false)
    private Long idPasajero;
    @Column(name = "dni_hash", length = 64, nullable = false)
    private String dniHash;
    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;
}
