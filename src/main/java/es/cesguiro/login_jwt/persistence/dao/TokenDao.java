package es.cesguiro.login_jwt.persistence.dao;

import es.cesguiro.login_jwt.domain.model.StoredToken;

public interface TokenDao {

	void insertRefreshToken(StoredToken token);
}
