package model;
import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Direcciones georreferenciadas reutilizables como origen o destino de un viaje (RF02/RF10).
 * Capturadas mediante autocompletado de Google Places (coordenadas exactas, no texto libre).
 *
 * NOTA: "direccion" se mantiene UNIQUE por sí sola (no en conjunto con comuna/lat/lon),
 * tal como se definió en el modelamiento del problema del README: es la llave candidata
 * que permite reutilizar una misma UBICACION entre distintas solicitudes que comparten
 * la misma dirección exacta (ej. el mismo aeropuerto).
 */
@Entity
@Table(name = "ubicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Long idUbicacion;

    @Column(name = "direccion", length = 200, nullable = false, unique = true)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_comuna", nullable = false)
    private Comuna comuna;

    @Column(name = "latitud", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitud;
}