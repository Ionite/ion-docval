package net.ionite.docval.server;

/**
 * Exception thrown when there is an error in the ion-docval client.
 */
public class DocValClientException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct the error with the given message
	 */
	public DocValClientException(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * Construct the error with the given message and base error
	 */
	public DocValClientException(String errorMessage, Throwable exc) {
		super(errorMessage, exc);
	}
}
