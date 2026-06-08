package com.arrendamientos.usuarios.infrastructure.persistence.adapter;

import com.arrendamientos.usuarios.domain.exception.DomainException;
import com.arrendamientos.usuarios.domain.port.out.SequenceGeneratorPort;
import com.arrendamientos.usuarios.infrastructure.persistence.repository.SequenceJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SequenceGeneratorAdapter implements SequenceGeneratorPort {

    private static final String USUARIO_ID = "UsuarioId";

    private final SequenceJpaRepository jpa;

    public SequenceGeneratorAdapter(SequenceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public String siguienteUsuarioId() {
        jpa.incrementar(USUARIO_ID);
        int next = jpa.findById(USUARIO_ID)
                .map(s -> s.getCurrentValue())
                .orElseThrow(() -> new DomainException(
                        "Sequence UsuarioId not found in Sequences table. Run V2 migration to initialize.",
                        HttpStatus.INTERNAL_SERVER_ERROR));
        return "usr-" + String.format("%03d", next);
    }
}
