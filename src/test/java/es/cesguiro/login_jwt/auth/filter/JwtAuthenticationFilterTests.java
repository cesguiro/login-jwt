package es.cesguiro.login_jwt.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import es.cesguiro.login_jwt.auth.SecurityContext;
import es.cesguiro.login_jwt.domain.model.AuthenticatedPrincipal;
import es.cesguiro.login_jwt.domain.model.TokenPair;
import es.cesguiro.login_jwt.domain.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class JwtAuthenticationFilterTests {

	private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(new StubAuthService());

	@Test
	void extractsTokenFromBearerAuthorizationHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertEquals(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")), observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	@Test
	void extractsTokenWhenBearerSchemeHasDifferentCase() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "bEaReR jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertEquals(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")), observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	@Test
	void leavesSecurityContextEmptyWhenAuthorizationHeaderIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	@Test
	void leavesSecurityContextEmptyWhenAuthorizationHeaderIsMalformed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	@Test
	void leavesSecurityContextEmptyWhenBearerTokenIsEmptyOrContainsWhitespace() throws Exception {
		MockHttpServletRequest emptyTokenRequest = new MockHttpServletRequest();
		emptyTokenRequest.addHeader("Authorization", "Bearer   ");
		MockHttpServletRequest whitespaceTokenRequest = new MockHttpServletRequest();
		whitespaceTokenRequest.addHeader("Authorization", "Bearer token with-spaces");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(emptyTokenRequest, response, filterChain);
		assertNull(observedPrincipal.get());
		jwtAuthenticationFilter.doFilter(whitespaceTokenRequest, response, filterChain);

		assertNull(observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	@Test
	void clearsSecurityContextWhenFilterChainThrows() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer jwt-token-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = (filteredRequest, filteredResponse) -> {
			assertEquals(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")), SecurityContext.get());
			throw new ServletException("Simulated downstream failure");
		};

		assertThrows(ServletException.class, () -> jwtAuthenticationFilter.doFilter(request, response, filterChain));
		assertNull(SecurityContext.get());
	}

	@Test
	void clearsStaleSecurityContextBeforeProcessingRequestWithoutToken() throws Exception {
		SecurityContext.set(new AuthenticatedPrincipal(7L, Set.of("ROLE_ADMIN")));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<Principal> observedPrincipal = new AtomicReference<>();
		FilterChain filterChain = (filteredRequest, filteredResponse) ->
				observedPrincipal.set(SecurityContext.get());

		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		assertNull(observedPrincipal.get());
		assertNull(SecurityContext.get());
	}

	private static class StubAuthService implements AuthService {

		@Override
		public TokenPair login(String email, String password) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<AuthenticatedPrincipal> authenticate(String accessToken) {
			return "jwt-token-123".equals(accessToken)
					? Optional.of(new AuthenticatedPrincipal(42L, Set.of("ROLE_USER")))
					: Optional.empty();
		}
	}
}
