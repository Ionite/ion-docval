package net.ionite.docval.validation.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidatorException;

/**
 * This class implements the DocumentValidor interface for XML Schema (XSD)
 * validation.
 */
public class XSDValidator implements DocumentValidator {
	private String _filename;
	private InputStream _stream;
	private Validator _validator = null;
	private Logger logger;

	/**
	 * Construct an XSD Validator with the given XML Schema definition (XSD) file.
	 * 
	 * @param filename The file containing the XML schema definition
	 * @throws ValidatorException if there is an error reasing the schema
	 */
	public XSDValidator(String filename) throws ValidatorException {
		_filename = filename;
		logger = LoggerFactory.getLogger(this.getClass().getName());
		reload();
	}

	/**
	 * Construct an XSD Validator with the XML Schema definition provided in the
	 * given input stream.
	 * 
	 * Note that the 'reload' method is disabled when this constructor is used.
	 * 
	 * @param stream the InputStream containing the XML schema definition
	 * @throws ValidatorException if there is an error reasing the schema
	 */
	public XSDValidator(InputStream stream) throws ValidatorException {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		_stream = stream;
		reload();
	}

	/**
	 * Reload the XSD file this validator was initialized with.
	 * 
	 * @throws ValidatorException If there is an error reloading the file, or if
	 *                            this validator was initialized with an input
	 *                            stream.
	 */
	public void reload() throws ValidatorException {
		if (_filename == null && _validator != null) {
			throw new ValidatorException("Can't reload an XSD validator based on a stream");
		}
		if (_filename != null) {
			if (_validator == null) {
				logger.debug("Loading {}", _filename);
			} else {
				logger.info("Reloading {}", _filename);
			}
		} else {
			logger.debug("Loading XSD validator from stream");
		}
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			StreamSource source;
			if (_filename != null) {
				source = new StreamSource(_filename);
			} else if (_stream != null) {
				source = new StreamSource(_stream);
			} else {
				throw new ValidatorException("Must have either stream or filename to load XSDValidator");
			}
			Schema schema = schemaFactory.newSchema(source);
			_validator = schema.newValidator();
			_validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			_validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		} catch (SAXException saxError) {
			logger.error("Reloading file {} failed: ", _filename, saxError);
			throw new ValidatorException("Error reading XSD file " + _filename, saxError);
		}
	}

	/**
	 * Validate the given XML document, and return a new ValidationResult structure
	 * containing the validation results.
	 * 
	 * @param source Byte-array containing the XML document to validate
	 */
	public ValidationResult validate(byte[] source) throws ValidatorException {
		ValidationResult result = new ValidationResult();
		return validate(source, result);
	}

	/**
	 * Validate the given XML document, and add the validation results to the given
	 * ValidationResult structure
	 * 
	 * @param source Byte-array containing the XML document to validate
	 * @param result The structure to add the validation results to
	 */
	public ValidationResult validate(byte[] source, ValidationResult result) throws ValidatorException {
		try {
			StreamSource ssource = new StreamSource(new ByteArrayInputStream(source));
			_validator.validate(ssource);
		} catch (SAXParseException saxParseError) {
			result.addError(saxParseError.getLocalizedMessage(), null, saxParseError.getLineNumber(),
					saxParseError.getColumnNumber(), "XML Schema");
		} catch (SAXException saxError) {
			result.addError("XML error: " + saxError.toString(), "Unknown", null, null, "XML Schema");
		} catch (IOException ioe) {
			logger.error("Validation against XSD failed", ioe);
			throw new ValidatorException("Input/output error while validating document", ioe);
		}
		logger.debug("Validated document against XSD {}: {} errors, {} warnings", _filename, result.errorCount(),
				result.warningCount());
		return result;
	}
}
