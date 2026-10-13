package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "viaje")
public class Viaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje", nullable = false)
    private Long idViaje;
    @Column(name = "folio", length = 20, nullable = false, updatable = false)
    private String folio;
    @Column(name = "cantidad_pasajeros", nullable = false)
    private Integer cantidadPasajeros = 0;
    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;
    @Column(name = "id_origen", nullable = false)
    private Long idOrigen;
    @Column(name = "id_destino", nullable = false)
    private Long idDestino;
    @Column(name = "id_estado", nullable = false)
    private Long idEstado;
    @Column(name = "MOVIL_id_movil")
    private Long movilIdMovil;
    @Column(name = "PAGO_id")
    private Long pagoId;
    @Column(name = "FH_creacion", nullable = false)
    private Long fhCreacion;
    @Column(name = "FH_agenda", nullable = false)
    private Long fhAgenda;
    @Column(name = "FH_asignacion")
    private Long fhAsignacion;
    @Column(name = "FH_inicio")
    private Long fhInicio;
    @Column(name = "FH_fin")
    private Long fhFin;
    @Column(name = "CONDUCTOR_id_usuario")
    private Long conductorIdUsuario;
    @Column(name = "numero_vuelo", length = 20)
    private String numeroVuelo;

    @PrePersist
    private void generarCamposPublicos() {
        if (folio == null || folio.isBlank()) {
            folio = "VJ" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        }
        if (cantidadPasajeros == null) cantidadPasajeros = 0;
    }
}
