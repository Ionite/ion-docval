package net.ionite.docval.validation;

/**
 * Exception that is thrown when validating documents fails due to an error, and
 * the validator or validator manager cannot return a validation result.
 * 
 * For example, this exception is thrown when a validation file could not be
 * loaded.
 * 
 * Note that this exception does not mean that the document is not valid.
 */
public class ValidatorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a ValidatorException with the given error message
	 * 
	 * @param errorMessage String containing the error message
	 */
	public ValidatorException(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * Construct a ConfigurationError with the given error message and underlying
	 * exception
	 * 
	 * @param errorMessage String containing the error message
	 * @param exc          Throwable containing an underlying exception
	 */
	public ValidatorException(String errorMessage, Throwable exc) {
		super(errorMessage, exc);
	}
}
