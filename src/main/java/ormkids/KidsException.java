package ormkids;

public class KidsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KidsException() {
		super();
	}

	public KidsException(String message, Throwable cause) {
		super(message, cause);
	}

	public KidsException(String message) {
		super(message);
	}

	public KidsException(Throwable cause) {
		super(cause);
	}

}
