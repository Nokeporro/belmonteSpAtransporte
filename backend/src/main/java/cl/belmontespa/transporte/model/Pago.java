package cl.belmontespa.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pago")
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	// Generado por el backend al crear el pago.
	@Column(name = "hash", length = 50, nullable = false, unique = true)
	private String hash;

	@Column(name = "monto", nullable = false)
	private Integer monto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estado_pago_id", nullable = false)
	private EstadoPago estadoPago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "banco_id", nullable = false)
	private Banco banco;

	// Empresa que paga.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id_empresa", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "medio_pago_id", nullable = false)
	private MedioPago medioPago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tipo_recibo_id", nullable = false)
	private TipoRecibo tipoRecibo;

	// NULL hasta que el pago pasa a PAGADO (P2).
	@Column(name = "fecha_pago")
	private LocalDateTime fechaPago;

	// NULL salvo que el pago se haya reembolsado (P4).
	@Column(name = "monto_reembolsado")
	private Integer montoReembolsado;

}
