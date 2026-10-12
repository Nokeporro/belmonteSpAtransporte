package com.belmontespa.transporte.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.belmontespa.transporte.service.CrudService;
public abstract class CrudController<E, ID> {
    protected final CrudService<E, ID> service;
    protected CrudController(CrudService<E, ID> service) { this.service = service; }
    @GetMapping public List<E> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public E findById(@PathVariable ID id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public E create(@RequestBody E entity) { return service.create(entity); }
    @PutMapping("/{id}") public E update(@PathVariable ID id, @RequestBody E entity) { return service.update(id, entity); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable ID id) { service.delete(id); }
}
