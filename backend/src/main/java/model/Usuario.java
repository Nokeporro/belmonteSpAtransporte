package model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;


    @Column(name = "correo", length = 150, nullable = false, unique = true)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    /**
     * Hash de la contraseña (BCrypt); nunca se almacena en texto plano.
     */
    @Column(name = "password_hash", length = 200, nullable = false)
    private String passwordHash;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /**
     * Token de notificaciones push (Firebase Cloud Messaging), registrado/actualizado al
     * iniciar sesión (RF13). Nullable: puede no existir aún si el usuario nunca abrió la app
     * o rechazó el permiso de notificaciones.
     */
    @Column(name = "token_fcm", length = 255)
    private String tokenFcm;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}