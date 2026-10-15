package es.cesguiro.login_jwt.controller.mapper;

import org.springframework.stereotype.Component;

import es.cesguiro.login_jwt.controller.dto.LoginResponse;
import es.cesguiro.login_jwt.domain.model.TokenPair;

@Component
public class AuthMapper {

	public LoginResponse toLoginResponse(TokenPair tokenPair) {
		return new LoginResponse(
				tokenPair.accessToken(),
				"Bearer",
				tokenPair.refreshToken());
	}
}
