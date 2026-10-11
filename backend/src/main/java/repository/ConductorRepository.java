package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.belmontespa.transporte.model.Conductor;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {

    /**
     * HU05: "solo se listan conductores disponibles, sin viaje en curso". Disponibilidad
     * calculada en tiempo de consulta (no es un campo manual): un conductor está
     * disponible si ningún Viaje suyo está actualmente en estado EN_CURSO.
     */
    @Query("""
            SELECT c FROM Conductor c
            WHERE c.idUsuario NOT IN (
                SELECT v.conductor.idUsuario FROM Viaje v
                WHERE v.conductor IS NOT NULL
                AND v.estado.nombreEstado = 'EN_CURSO'
            )
            """)
    List<Conductor> findDisponibles();
}
