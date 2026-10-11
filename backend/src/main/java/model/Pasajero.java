package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona transportada; puede no coincidir con el Solicitante (RF02).
 *
 * ACTUALIZADA (schema.sql 2026-10-10): la columna "rut" pasó a "rut_hash" — se
 * almacena un hash del RUT, no el RUT en texto plano (buena práctica de privacidad,
 * en línea con RNF08). El hashing/verificación va en la capa Service, no en la entidad.
 */
@Entity
@Table(name = "pasajero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pasajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pasajero")
    private Long idPasajero;

    @Column(name = "rut_hash", length = 50, nullable = false, unique = true)
    private String rutHash;

    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;
}
