package es.cesguiro.login_jwt.domain.model;

public record TokenPair(
		String accessToken,
		String refreshToken) {
}
