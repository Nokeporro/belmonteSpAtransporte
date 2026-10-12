package com.belmontespa.transporte.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administrador")
public class Administrador {
    @Id
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;
}
