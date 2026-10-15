package es.cesguiro.login_jwt.domain.service;

import es.cesguiro.login_jwt.domain.model.User;

public interface UserService {

	User findById(Long userId);
}
