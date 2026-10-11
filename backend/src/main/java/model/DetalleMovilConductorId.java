package model;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave compuesta (id_movil, id_usuario) de DetalleMovilConductor.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMovilConductorId implements Serializable {

    private Long movil;
    private Long conductor;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetalleMovilConductorId)) return false;
        DetalleMovilConductorId that = (DetalleMovilConductorId) o;
        return Objects.equals(movil, that.movil) && Objects.equals(conductor, that.conductor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movil, conductor);
    }
}
