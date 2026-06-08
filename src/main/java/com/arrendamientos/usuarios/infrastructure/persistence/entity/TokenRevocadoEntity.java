package com.arrendamientos.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "TokensRevocados")
public class TokenRevocadoEntity {

    @Id
    @Column(name = "TokenId", length = 255, nullable = false)
    private String tokenId;

    @Column(name = "RevocadoEl", nullable = false, columnDefinition = "datetime2")
    private Instant revocadoEl;

    @Column(name = "Expiracion", columnDefinition = "datetime2")
    private Instant expiracion;

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public Instant getRevocadoEl() { return revocadoEl; }
    public void setRevocadoEl(Instant revocadoEl) { this.revocadoEl = revocadoEl; }

    public Instant getExpiracion() { return expiracion; }
    public void setExpiracion(Instant expiracion) { this.expiracion = expiracion; }
}
