package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@IdClass(DetalleViajePasajeroId.class)
@Entity
@Table(name = "detalle_viaje_pasajero")
public class DetalleViajePasajero {
    @Id
    @Column(name = "id_viaje", nullable = false)
    private Long idViaje;
    @Id
    @Column(name = "id_pasajero")
    private Long idPasajero;
}
