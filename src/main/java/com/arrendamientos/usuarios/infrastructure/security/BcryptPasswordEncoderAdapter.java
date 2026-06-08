package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate;

    public BcryptPasswordEncoderAdapter(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String hash(String raw) {
        return delegate.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hashBcrypt) {
        if (raw == null || hashBcrypt == null) {
            return false;
        }
        return delegate.matches(raw, hashBcrypt);
    }
}
