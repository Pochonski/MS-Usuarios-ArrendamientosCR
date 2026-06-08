package com.arrendamientos.usuarios.domain.port.out;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import io.jsonwebtoken.Claims;

public interface TokenProviderPort {

    String generarAccessToken(String userId, String correo, RolUsuario rol, String jti);

    String generarRefreshToken(String userId, String jti);

    String generarEmailVerificationToken(String userId, String correo);

    Claims parsearAccessToken(String token);

    Claims parsearEmailVerificationToken(String token);
}
