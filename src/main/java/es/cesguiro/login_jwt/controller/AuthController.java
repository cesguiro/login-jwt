package es.cesguiro.login_jwt.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.cesguiro.login_jwt.auth.SecurityContext;
import es.cesguiro.login_jwt.controller.dto.LoginRequest;
import es.cesguiro.login_jwt.controller.dto.LoginResponse;
import es.cesguiro.login_jwt.controller.dto.UserResponse;
import es.cesguiro.login_jwt.controller.exception.AuthenticationRequiredException;
import es.cesguiro.login_jwt.controller.mapper.AuthMapper;
import es.cesguiro.login_jwt.controller.mapper.UserResponseMapper;
import es.cesguiro.login_jwt.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_jwt.domain.service.AuthService;
import es.cesguiro.login_jwt.domain.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthMapper authMapper;
	private final UserService userService;
	private final UserResponseMapper userMapper;

	public AuthController(
			AuthService authService,
			AuthMapper authMapper,
			UserService userService,
			UserResponseMapper userMapper) {
		this.authService = authService;
		this.authMapper = authMapper;
		this.userService = userService;
		this.userMapper = userMapper;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		return ResponseEntity.ok(authMapper.toLoginResponse(
				authService.login(loginRequest.email(), loginRequest.password())));
	}

	@GetMapping("/me")
	public UserResponse currentUser() {
		Principal principal = SecurityContext.get();
		if (!(principal instanceof AuthenticatedPrincipal authenticatedPrincipal)) {
			throw new AuthenticationRequiredException();
		}

		return userMapper.toUserResponse(userService.findById(authenticatedPrincipal.userId()));
	}
}
