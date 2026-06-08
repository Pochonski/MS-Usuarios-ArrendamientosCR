package com.arrendamientos.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "Usuarios")
public class UsuarioEntity {

    @Id
    @Column(name = "Id", length = 50, nullable = false)
    private String id;

    @Column(name = "Nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "Correo", length = 255, nullable = false)
    private String correo;

    @Column(name = "ContrasenaHash", columnDefinition = "NVARCHAR(MAX)")
    private String contrasenaHash;

    @Column(name = "Rol", length = 20, nullable = false)
    private String rol;

    @Column(name = "Telefono", length = 20)
    private String telefono;

    @Column(name = "Avatar", length = 500)
    private String avatar;

    @Column(name = "GoogleId", length = 255)
    private String googleId;

    @Column(name = "FechaRegistro", nullable = false, columnDefinition = "datetime2")
    private Instant fechaRegistro;

    @Column(name = "UltimoLogin", columnDefinition = "datetime2")
    private Instant ultimoLogin;

    @Column(name = "IntentosFallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "BloqueadoHasta", columnDefinition = "datetime2")
    private Instant bloqueadoHasta;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Instant getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}
