package com.belmontespa.transporte.service;

import com.belmontespa.transporte.model.Usuario;
import com.belmontespa.transporte.repository.AdministradorRepository;
import com.belmontespa.transporte.repository.ConductorRepository;
import com.belmontespa.transporte.repository.SolicitanteRepository;
import com.belmontespa.transporte.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TransporteUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final AdministradorRepository administradores;
    private final ConductorRepository conductores;
    private final SolicitanteRepository solicitantes;

    public TransporteUserDetailsService(UsuarioRepository usuarios,
            AdministradorRepository administradores,
            ConductorRepository conductores,
            SolicitanteRepository solicitantes) {
        this.usuarios = usuarios;
        this.administradores = administradores;
        this.conductores = conductores;
        this.solicitantes = solicitantes;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        Long id = usuario.getIdUsuario();
        String rol;
        if (administradores.existsByIdUsuario(id)) rol = "ADMINISTRADOR";
        else if (conductores.existsByIdUsuario(id)) rol = "CONDUCTOR";
        else if (solicitantes.existsByIdUsuario(id)) rol = "SOLICITANTE";
        else throw new UsernameNotFoundException("La cuenta no tiene un rol activo");

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities("ROLE_" + rol)
                .build();
    }
}
