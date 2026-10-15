package es.cesguiro.login_jwt.persistence.dao;

import java.util.Optional;

import es.cesguiro.login_jwt.domain.model.User;

public interface UserDao {

	Optional<User> findByEmail(String email);

	Optional<User> findById(Long id);
}
