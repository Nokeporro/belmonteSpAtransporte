package com.belmontespa.transporte.repository;
import com.belmontespa.transporte.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {}
