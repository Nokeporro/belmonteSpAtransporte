package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PAGO.tipo_recibo_nombre referencia a esta tabla por "nombre" (no por "id"),
 * así que "nombre" debe mantenerse UNIQUE.
 * (El schema.sql actualizado ya trae PK propia en "id"; el bug que faltaba fue
 * corregido en el repo.)
 */
@Entity
@Table(name = "tipo_recibo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoRecibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 20, nullable = false, unique = true)
    private String nombre;
}
