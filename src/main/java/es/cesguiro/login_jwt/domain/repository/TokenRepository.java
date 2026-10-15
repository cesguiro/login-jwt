package es.cesguiro.login_jwt.domain.repository;

import es.cesguiro.login_jwt.domain.model.StoredToken;

public interface TokenRepository {

	void saveRefreshToken(StoredToken refreshToken);
}
