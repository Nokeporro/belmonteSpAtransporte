package com.belmontespa.transporte.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class DetalleViajePasajeroId implements Serializable {
    private Long idViaje;
    private Long idPasajero;
}
