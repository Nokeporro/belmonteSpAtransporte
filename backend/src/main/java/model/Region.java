package model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "region")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_region")
    private Long idRegion;

    @Column(name = "nombre", length = 60, nullable = false, unique = true)
    private String nombre;
}
