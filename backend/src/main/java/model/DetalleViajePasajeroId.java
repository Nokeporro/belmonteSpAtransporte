package model;

import java.io.Serializable;
import java.util.Objects;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleViajePasajeroId implements Serializable {

    private Long viaje;
    private Long pasajero;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetalleViajePasajeroId)) return false;
        DetalleViajePasajeroId that = (DetalleViajePasajeroId) o;
        return Objects.equals(viaje, that.viaje) && Objects.equals(pasajero, that.pasajero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viaje, pasajero);
    }
}