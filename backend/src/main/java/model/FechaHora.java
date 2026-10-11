package model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NUEVA (schema.sql actualizado 2026-10-10): valor de fecha/hora independiente,
 * referenciado por FK desde Viaje (creación, agenda, asignación, inicio, fin) y Pago.
 * Cada marca de tiempo es su propia fila; no se reutiliza entre registros.
 *
 * NOTA: el schema.sql declara la columna como DATE (Oracle), que a diferencia del DATE
 * ANSI SQL incluye hora/minuto/segundo. Se mapea a LocalDateTime (no LocalDate) para no
 * perder esa precisión — importante para FH_agenda, que es la hora exacta del traslado.
 */
@Entity
@Table(name = "fecha_hora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FechaHora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;
}
