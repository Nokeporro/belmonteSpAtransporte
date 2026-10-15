package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comuna")
public class Comuna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comuna", nullable = false)
    private Long idComuna;
    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;
    @Column(name = "id_region", nullable = false)
    private Long idRegion;
}
