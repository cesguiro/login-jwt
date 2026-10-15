package es.cesguiro.login_jwt.auth.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.cesguiro.login_jwt.auth.SecurityContext;
import es.cesguiro.login_jwt.domain.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_SCHEME = "Bearer";
	private final AuthService authService;

	public JwtAuthenticationFilter(AuthService authService) {
		this.authService = authService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			SecurityContext.clear();
			String accessToken = extractAccessToken(request.getHeader("Authorization"));
			if (accessToken != null) {
				authService.authenticate(accessToken).ifPresent(SecurityContext::set);
			}
			filterChain.doFilter(request, response);
		} finally {
			SecurityContext.clear();
		}
	}

	private String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null) {
			return null;
		}

		int schemeEnd = authorizationHeader.indexOf(' ');
		if (schemeEnd < 0 || !BEARER_SCHEME.equalsIgnoreCase(authorizationHeader.substring(0, schemeEnd))) {
			return null;
		}

		String accessToken = authorizationHeader.substring(schemeEnd).stripLeading();
		if (accessToken.isEmpty() || accessToken.chars().anyMatch(Character::isWhitespace)) {
			return null;
		}

		return accessToken;
	}
}
