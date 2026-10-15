package es.cesguiro.login_jwt.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Credenciales no válidas.");
	}
}
