package repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import model.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    /**
     * RF03: historial de solicitudes del Solicitante autenticado.
     * ACTUALIZADO: fecha_creacion ya no es columna directa, es fhCreacion (FK a
     * FechaHora); se ordena por su campo "data" vía el path anidado.
     */
    List<Viaje> findBySolicitante_IdUsuarioOrderByFhCreacion_DataDesc(Long idSolicitante);

    /**
     * RF04: solicitudes pendientes de asignación, para el panel del Administrador.
     */
    List<Viaje> findByEstado_NombreEstadoOrderByFhCreacion_DataAsc(String nombreEstado);

    /**
     * RF07/HU07: "Viajes del día" del Conductor autenticado, ordenados por hora
     * programada. Filtra por fhAgenda.data (fecha/hora del viaje en sí, no por
     * fhCreacion). ACTUALIZADO: fhAgenda es ahora una FK a FechaHora (ver esa entidad).
     */
    @Query("""
            SELECT v FROM Viaje v
            WHERE v.conductor.idUsuario = :idConductor
            AND v.fhAgenda.data BETWEEN :inicioDia AND :finDia
            AND v.estado.nombreEstado IN ('ASIGNADO', 'EN_CURSO')
            ORDER BY v.fhAgenda.data ASC
            """)
    List<Viaje> findViajesDelDia(
            @Param("idConductor") Long idConductor,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia
    );
}
