package net.ionite.docval.config;

/**
 * Thrown when there is an error reading the configuration file
 * 
 * @author Ionite
 *
 */
public class ConfigurationError extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a ConfigurationError with the given error message
	 * 
	 * @param errorMessage String containing the error message
	 */
	public ConfigurationError(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * Construct a ConfigurationError with the given error message and underlying
	 * exception
	 * 
	 * @param errorMessage String containing the error message
	 * @param exc          Throwable containing an underlying exception
	 */
	public ConfigurationError(String errorMessage, Throwable exc) {
		super(errorMessage, exc);
	}
}
