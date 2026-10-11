package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Empresa cliente corporativa a la que pertenece un Solicitante (RF01).
 */
@Entity
@Table(name = "empresa", uniqueConstraints = @UniqueConstraint(columnNames = {"razon_social", "rut"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "razon_social", length = 50, nullable = false)
    private String razonSocial;

    @Column(name = "rut", length = 15, nullable = false)
    private String rut;
}
