package es.cesguiro.login_jwt.controller.dto;

public record LoginResponse(
		String accessToken,
		String tokenType,
		String refreshToken) {
}
