package com.partidoya.platform.auth.security;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Única definición de "token de acceso válido": firma y expiración correctas y usuario activo.
 * La usan tanto el filtro HTTP como el handshake STOMP para que ambas vías apliquen las mismas reglas.
 */
@Component
public class AccessTokenAuthenticator {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;
    private final UserRepository userRepository;

    public AccessTokenAuthenticator(JwtTokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    /** @param authorizationHeader valor completo de la cabecera, por ejemplo {@code "Bearer eyJ..."} */
    public Optional<AuthenticatedUser> authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        try {
            var userId = tokenService.parseUserId(authorizationHeader.substring(BEARER_PREFIX.length()));
            return userRepository.findById(new UserId(userId))
                    .filter(user -> user.isActive())
                    .map(user -> new AuthenticatedUser(user.getId().value(), user.getEmail().value(),
                            user.getRole(), user.getPlan()));
        } catch (RuntimeException invalidToken) {
            return Optional.empty();
        }
    }
}
