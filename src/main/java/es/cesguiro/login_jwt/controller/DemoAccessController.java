package es.cesguiro.login_jwt.controller;

import java.security.Principal;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.cesguiro.login_jwt.auth.SecurityContext;
import es.cesguiro.login_jwt.controller.exception.AuthenticationRequiredException;
import es.cesguiro.login_jwt.controller.exception.InsufficientRoleException;
import es.cesguiro.login_jwt.domain.model.AuthenticatedPrincipal;

@RestController
@RequestMapping("/demo")
public class DemoAccessController {

	@GetMapping("/public")
	public DemoMessageResponse publicRoute() {
		return new DemoMessageResponse("Ruta pública.");
	}

	@GetMapping("/profile")
	public PrincipalResponse authenticatedRoute() {
		AuthenticatedPrincipal authenticatedPrincipal = requireAuthenticatedPrincipal();
		return new PrincipalResponse(authenticatedPrincipal.userId(), authenticatedPrincipal.roles());
	}

	@GetMapping("/admin")
	public DemoMessageResponse adminRoute() {
		AuthenticatedPrincipal authenticatedPrincipal = requireAuthenticatedPrincipal();
		if (!authenticatedPrincipal.roles().contains("ROLE_ADMIN")) {
			throw new InsufficientRoleException();
		}

		return new DemoMessageResponse("Acceso de administrador.");
	}

	private AuthenticatedPrincipal requireAuthenticatedPrincipal() {
		Principal principal = SecurityContext.get();
		if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
			return authenticatedPrincipal;
		}
		throw new AuthenticationRequiredException();
	}

	public record DemoMessageResponse(String message) {
	}

	public record PrincipalResponse(Long userId, Set<String> roles) {
	}
}
