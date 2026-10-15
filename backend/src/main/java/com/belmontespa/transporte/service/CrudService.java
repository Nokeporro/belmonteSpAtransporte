package com.belmontespa.transporte.service;
import com.belmontespa.transporte.model.Usuario;
import java.lang.reflect.Field;
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
    /** Busca por la clave pública definida por el contrato REST, nunca por el ID interno. */
    public E findByBusinessKey(String key) {
        return repository.findAll().stream()
                .filter(entity -> matchesBusinessKey(entity, key))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean matchesBusinessKey(E entity, String key) {
        String entityName = entity.getClass().getSimpleName();
        String property = switch (entityName) {
            case "Usuario" -> "correo";
            case "Administrador", "Conductor", "Solicitante" -> "idUsuario";
            case "Empresa" -> "rut";
            case "Movil" -> "patente";
            case "Pasajero" -> "dniHash";
            case "Pago" -> "hash";
            case "Viaje" -> "folio";
            default -> null;
        };
        if (property == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            Field field = entity.getClass().getDeclaredField(property);
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value == null) return false;
            if ("idUsuario".equals(property)) {
                Usuario usuario = entityManager.find(Usuario.class, value);
                return usuario != null && key.equalsIgnoreCase(usuario.getCorreo());
            }
            return key.equalsIgnoreCase(value.toString());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudo resolver la clave de negocio de " + entityName, exception);
        }
    }
    public E create(E entity) { return repository.save(entity); }
    public E updateByBusinessKey(String key, E replacement) {
        E current = findByBusinessKey(key);
        Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(current);
        try {
            Field idField = java.util.Arrays.stream(replacement.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(jakarta.persistence.Id.class))
                    .findFirst().orElseThrow();
            idField.setAccessible(true);
            idField.set(replacement, id);
            return update((ID) id, replacement);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo actualizar el recurso", exception);
        }
    }
    public E update(ID id, E entity) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object bodyId = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (bodyId == null || !bodyId.equals(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del cuerpo debe coincidir con el ID de la ruta");
        return repository.save(entity);
    }
}
