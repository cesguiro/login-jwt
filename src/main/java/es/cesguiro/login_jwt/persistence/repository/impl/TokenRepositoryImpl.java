package es.cesguiro.login_jwt.persistence.repository.impl;

import org.springframework.stereotype.Repository;

import es.cesguiro.login_jwt.domain.model.StoredToken;
import es.cesguiro.login_jwt.domain.repository.TokenRepository;
import es.cesguiro.login_jwt.persistence.dao.TokenDao;

@Repository
public class TokenRepositoryImpl implements TokenRepository {

	private final TokenDao tokenDao;

	public TokenRepositoryImpl(TokenDao tokenDao) {
		this.tokenDao = tokenDao;
	}

	@Override
	public void saveRefreshToken(StoredToken refreshToken) {
		tokenDao.insertRefreshToken(refreshToken);
	}
}
