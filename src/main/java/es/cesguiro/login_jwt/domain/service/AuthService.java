package es.cesguiro.login_jwt.domain.service;

import java.util.Optional;

import es.cesguiro.login_jwt.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_jwt.domain.model.TokenPair;

public interface AuthService {

	TokenPair login(String email, String password);

	Optional<AuthenticatedPrincipal> authenticate(String accessToken);
}
