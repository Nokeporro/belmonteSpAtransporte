package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "rut_hash", length = 50, nullable = false)
    private String rutHash;
    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;
}
