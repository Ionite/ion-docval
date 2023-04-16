package net.ionite.docval.validation.validator;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidatorException;

/**
 * Interface for document validator implementation classes.
 * 
 * @author Ionite
 *
 */
public interface DocumentValidator {

	/**
	 * Reload the underlying validation file
	 * 
	 * @throws ValidatorException
	 */
	public void reload() throws ValidatorException;

	/**
	 * Validate the given XML source against the validation file and return a new
	 * ValidationResult instance
	 * 
	 * @param source The XML source to validate
	 * @return A newly initialized ValidationResult containing the results of the
	 *         validation
	 * @throws ValidatorException Thrown when there is an error performing the
	 *                            validation
	 */
	public ValidationResult validate(byte[] source) throws ValidatorException;

	/**
	 * Validate the given XML source against the validation file, and add the errors
	 * and warnings to the given ValidationResult item.
	 * 
	 * @param source The XML source to validate
	 * @param result Errors and warnings are added to this instance in-place
	 * @return The modified ValidationResult instance
	 * @throws ValidatorException Thrown when there is an error performing the
	 *                            validation
	 */
	public ValidationResult validate(byte[] source, ValidationResult result) throws ValidatorException;
};
