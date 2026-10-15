package es.cesguiro.login_jwt.auth;

import java.security.Principal;

public final class SecurityContext {

	private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

	private SecurityContext() {
	}

	public static void set(Principal principal) {
		CURRENT.set(principal);
	}

	public static Principal get() {
		return CURRENT.get();
	}

	public static void clear() {
		CURRENT.remove();
	}
}
