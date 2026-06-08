package com.arrendamientos.usuarios.domain.port.out;

public interface PasswordEncoderPort {
    String hash(String raw);
    boolean matches(String raw, String hashBcrypt);
}
