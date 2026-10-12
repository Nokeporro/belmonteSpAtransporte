package com.belmontespa.transporte.service;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.server.ResponseStatusException;
public abstract class CrudService<E, ID> {
    protected final JpaRepository<E, ID> repository;
    @PersistenceContext private EntityManager entityManager;
    protected CrudService(JpaRepository<E, ID> repository) { this.repository = repository; }
    public List<E> findAll() { return repository.findAll(); }
    public E findById(ID id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public E create(E entity) { return repository.save(entity); }
    public E update(ID id, E entity) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object bodyId = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (bodyId == null || !bodyId.equals(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del cuerpo debe coincidir con el ID de la ruta");
        return repository.save(entity);
    }
    public void delete(ID id) { if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
