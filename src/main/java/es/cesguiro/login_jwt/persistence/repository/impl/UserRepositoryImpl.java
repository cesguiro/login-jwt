package es.cesguiro.login_jwt.persistence.repository.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import es.cesguiro.login_jwt.domain.model.User;
import es.cesguiro.login_jwt.domain.repository.UserRepository;
import es.cesguiro.login_jwt.persistence.dao.RoleDao;
import es.cesguiro.login_jwt.persistence.dao.UserDao;

@Repository
public class UserRepositoryImpl implements UserRepository {

	private final UserDao userDao;
	private final RoleDao roleDao;

	public UserRepositoryImpl(UserDao userDao, RoleDao roleDao) {
		this.userDao = userDao;
		this.roleDao = roleDao;
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userDao.findByEmail(email)
				.map(user -> user.withRoles(roleDao.findNamesByUserId(user.id())));
	}

	@Override
	public Optional<User> findById(Long id) {
		return userDao.findById(id)
				.map(user -> user.withRoles(roleDao.findNamesByUserId(user.id())));
	}
}
