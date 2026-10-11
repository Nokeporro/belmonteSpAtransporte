package model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comuna", uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "id_region"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comuna")
    private Long idComuna;

    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_region", nullable = false)
    private Region region;
}