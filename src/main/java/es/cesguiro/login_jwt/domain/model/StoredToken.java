package es.cesguiro.login_jwt.domain.model;

import java.time.Instant;

public record StoredToken(Long userId, byte[] tokenHash, Instant issuedAt, Instant expiresAt) {
}
