package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

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
    @Column(name = "hash", length = 50, nullable = false)
    private String hash;
    @Column(name = "monto", nullable = false)
    private BigDecimal monto;
    @Column(name = "ESTADO_PAGO_id", nullable = false)
    private Long estadoPagoId;
    @Column(name = "BANCO_id", nullable = false)
    private Long bancoId;
    @Column(name = "EMPRESA_id_empresa", nullable = false)
    private Long empresaIdEmpresa;
    @Column(name = "MEDIO_PAGO_id", nullable = false)
    private Long medioPagoId;
    @Column(name = "TIPO_RECIBO_nombre", length = 20, nullable = false)
    private String tipoReciboNombre;
    @Column(name = "FECHA_HORA_id", nullable = false)
    private Long fechaHoraId;
}
