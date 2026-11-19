package com.gateway.jwt.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "USUARIOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "correo_elec", nullable = false, unique = true)
    private String email;

    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    /**
     * Indica si el usuario tiene permisos de administrador.
     * true = admin, false = usuario normal
     */
    @Column(name = "permiso_admin", nullable = false)
    private Boolean permisoAdmin;

}
