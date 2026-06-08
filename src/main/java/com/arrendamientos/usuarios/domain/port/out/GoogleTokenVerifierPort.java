package com.arrendamientos.usuarios.domain.port.out;

import com.arrendamientos.usuarios.domain.model.GoogleUserInfo;

public interface GoogleTokenVerifierPort {
    GoogleUserInfo verificar(String idToken, String nonce, String hostedDomain);
}
