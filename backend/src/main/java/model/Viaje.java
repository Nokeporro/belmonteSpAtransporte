package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de traslado corporativo: entidad central del dominio (RF02-RF15;
 * casos de uso 1, 4, 5 y 8).
 *
 * ACTUALIZADA (schema.sql 2026-10-10):
 * - Las marcas de tiempo (creación/agenda/asignación/inicio/fin) ya no son columnas
 *   TIMESTAMP directas: cada una es ahora una FK a FechaHora (ver esa entidad).
 *   fhAgenda reemplaza lo que hasta la versión anterior llamábamos "fechaHoraProgramada".
 * - Tiene FK directa a Conductor (id_conductor), como ya habíamos decidido.
 * - movil y pago son opcionales (nullable) mientras el viaje no está asignado/pagado.
 */
@Entity
@Table(name = "viaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    private Long idViaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Solicitante solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_origen", nullable = false)
    private Ubicacion origen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_destino", nullable = false)
    private Ubicacion destino;

    /**
     * Ciclo de vida del traslado: PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO, CANCELADO
     * (RF05/RF09/RF15).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoSolicitud estado;

    /**
     * Vehículo utilizado; nullable mientras el viaje está PENDIENTE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movil_id_movil")
    private Movil movil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /**
     * Momento en que se registró la solicitud (RF02).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fh_creacion", nullable = false)
    private FechaHora fhCreacion;

    /**
     * Fecha y hora en que el pasajero necesita ser trasladado, indicada por el
     * Solicitante en el formulario (RF02). Usado para filtrar "Viajes del día" (HU07).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fh_agenda", nullable = false)
    private FechaHora fhAgenda;

    /**
     * Momento en que el Administrador asigna (o reasigna) un conductor (RF05).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fh_asignacion")
    private FechaHora fhAsignacion;

    /**
     * Momento en que el Conductor marca el viaje como En Curso (RF09).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fh_inicio")
    private FechaHora fhInicio;

    /**
     * Momento en que el Conductor marca el viaje como Finalizado (RF09).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fh_fin")
    private FechaHora fhFin;

    /**
     * Conductor asignado (RF05/HU05). Nullable: no existe todavía mientras el viaje
     * está en estado PENDIENTE; se llena al asignar (o reasignar) y pasa a ASIGNADO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id_usuario")
    private Conductor conductor;

    /**
     * Opcional; solo aplica a traslados desde/hacia el aeropuerto.
     */
    @Column(name = "numero_vuelo", length = 20)
    private String numeroVuelo;

    /*
     * NOTA: no quedó espacio en el schema.sql actual para fecha_cancelacion (RF15).
     * Dado el patrón FechaHora, lo natural sería agregar una FK más: fh_cancelacion
     * (nullable), simétrica a fh_asignacion/fh_inicio/fh_fin. Falta agregarla al
     * schema.sql del repo y aquí cuando se confirme.
     */
}
