package com.belmontespa.transporte.controller;

import com.belmontespa.transporte.dto.request.UsuarioDTORequest;
import com.belmontespa.transporte.dto.response.UsuarioDTOResponse;
import com.belmontespa.transporte.model.Usuario;
import com.belmontespa.transporte.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDTOResponse> findAll() {
        return service.findAll().stream().map(UsuarioController::toResponse).toList();
    }

    @GetMapping("/{correo}")
    public UsuarioDTOResponse findByCorreo(@PathVariable String correo) {
        return toResponse(service.findByBusinessKey(correo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTOResponse create(@Valid @RequestBody UsuarioDTORequest request) {
        Usuario usuario = toEntity(request);
        return toResponse(service.create(usuario));
    }

    @PutMapping("/{correo}")
    public UsuarioDTOResponse update(@PathVariable String correo, @Valid @RequestBody UsuarioDTORequest request) {
        return toResponse(service.updateByBusinessKey(correo, toEntity(request)));
    }

    private static Usuario toEntity(UsuarioDTORequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setTelefono(request.telefono());
        usuario.setPasswordHash(request.password());
        usuario.setTokenFcm(request.tokenFcm());
        return usuario;
    }

    private static UsuarioDTOResponse toResponse(Usuario usuario) {
        return new UsuarioDTOResponse(usuario.getNombre(), usuario.getCorreo(), usuario.getTelefono(),
                usuario.getFechaRegistro(), usuario.getTokenFcm());
    }
}
