package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import model.DetalleMovilConductor;
import model.DetalleMovilConductorId;

public interface DetalleMovilConductorRepository
        extends JpaRepository<DetalleMovilConductor, DetalleMovilConductorId> {

    List<DetalleMovilConductor> findByConductor_IdUsuario(Long idConductor);

    List<DetalleMovilConductor> findByMovil_IdMovil(Long idMovil);
}
