package model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ACTUALIZADA (schema.sql 2026-10-10): la columna fecha_hora directa fue reemplazada
 * por una FK a FechaHora, igual que en Viaje.
 */
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hash", nullable = false, unique = true)
    private String hash;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_pago_id", nullable = false)
    private EstadoPago estadoPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id_empresa", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medio_pago_id", nullable = false)
    private MedioPago medioPago;

    /**
     * FK no estándar: referencia TipoRecibo por su columna "nombre" (no por "id"),
     * tal como está definido en el schema.sql.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_recibo_nombre", referencedColumnName = "nombre", nullable = false)
    private TipoRecibo tipoRecibo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fecha_hora_id", nullable = false)
    private FechaHora fechaHora;
}
