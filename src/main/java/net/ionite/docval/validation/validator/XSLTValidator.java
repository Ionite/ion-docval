package net.ionite.docval.validation.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.xml.IgnoreErrorHandler;
import net.sf.saxon.trans.XPathException;

/**
 * This class implements the DocumentValidor interface for Schematron- XSLT
 * validation files. These are XSLT files that transform an input document to a
 * document following the SVRL schema, and are generally distributed as the
 * 'compiled' version of schematron files.
 * 
 * Using these files is much faster than using .SCH files with the SCHValidator
 * directly.
 */
public class XSLTValidator implements DocumentValidator {
	/**
	 * The XSLT file created from a schematron definition, if this validator is
	 * initialized with a file. Null otherwise.
	 */
	protected String _filename;
	/**
	 * The InputStream containing the XSLT created from a schematron definition, if
	 * this validator is initialized with an inputstream. Null otherwise.
	 */
	protected InputStream _stream;
	private Transformer transformer = null;
	/** General Logger instance */
	protected Logger logger;

	/**
	 * Handle SVRL output and put and failed assertion information into the
	 * ValidationResult instance
	 */
	private class SVRLHandler extends DefaultHandler {
		private ValidationResult _result;

		private boolean _inFailedAssert;
		private String _flag;
		private String _location;
		private String _test;
		private StringBuilder _currentValue = new StringBuilder();

		private HashMap<String, String> _nsPrefixes = new HashMap<String, String>();

		public SVRLHandler(ValidationResult result) {
			_result = result;
			_inFailedAssert = false;
		}

		@Override
		public void characters(char ch[], int start, int length) {
			if (_inFailedAssert) {
				_currentValue.append(ch, start, length);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (uri.equals("http://purl.oclc.org/dsdl/svrl") && localName.equals("ns-prefix-in-attribute-values")) {
				// Store them 'reversed', we want to map URI to prefix later (see applyPrefixes)
				_nsPrefixes.put(attributes.getValue("uri"), attributes.getValue("prefix"));
			}
			if (qName.equals("svrl:failed-assert")) {
				_flag = attributes.getValue("flag");
				_location = applyPrefixes(attributes.getValue("location"));
				_test = attributes.getValue("test");
			}
			if (qName.equals("svrl:text")) {
				_inFailedAssert = true;
				_currentValue.setLength(0);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			if (qName.equalsIgnoreCase("svrl:text")) {
				_inFailedAssert = false;
			}

			if (qName.equalsIgnoreCase("svrl:failed-assert")) {
				if ("warning".equals(_flag)) {
					_result.addWarning(_currentValue.toString(), _location, null, null, _test);
				} else {
					_result.addError(_currentValue.toString(), _location, null, null, _test);
				}
			}
		}

		/*
		 * Applies prefixes in the string as follows: '*:foo[namespace-uri() = "bar"]'
		 * will, if 'bar' is a namespace uri with a prefix 'baz', be replaced with:
		 * 'baz:foo'
		 */
		private String applyPrefixes(String input) {
			Pattern pattern = Pattern.compile("(.*/)\\*(:.*)\\[namespace-uri\\(\\)=\\'(.*)\\'\\](.*)",
					Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(input);
			while (matcher.matches()) {
				String uri = matcher.group(3);
				if (_nsPrefixes.containsKey(uri)) {
					input = matcher.group(1) + _nsPrefixes.get(uri) + matcher.group(2) + matcher.group(4);

					matcher = pattern.matcher(input);
				} else {
					// Found unknown uri, just leave the rest of the namespacing as-is
					return input;
				}
			}
			return input;
		}
	}

	/**
	 * Validation handler for the provided XSLT file; this is not much more than a
	 * sanity check. We check whether there is at least one element in the XSLT
	 * itself that has the namespace http://purl.oclc.org/dsdl/svrl
	 */
	private class SVRLCheckHandler extends DefaultHandler {
		private boolean hasSVRLOutput = false;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (uri.equals("http://purl.oclc.org/dsdl/svrl")) {
				hasSVRLOutput = true;
			}
		}

		public boolean isSVRLStylesheet() {
			return hasSVRLOutput;
		}
	}

	/**
	 * Construct an Schematron XSLT Validator with the given XSLT file.
	 * 
	 * @param filename The XSLT file created from a schematron definition
	 */
	public XSLTValidator(String filename) {
		_filename = filename;
		logger = LoggerFactory.getLogger(this.getClass().getName());
		reload();
	}

	/**
	 * Construct an Schematron XSLT Validator with the given input stream
	 * 
	 * @param stream the InputStream containing the XSLT created from a schematron
	 *               definition
	 */
	public XSLTValidator(InputStream stream) {
		_stream = stream;
		logger = LoggerFactory.getLogger(this.getClass().getName());
		reload();
	}

	/**
	 * Reload the XSLT file this validator was initialized with.
	 * 
	 * @throws ValidatorException If there is an error reloading the file, or if
	 *                            this validator was initialized with an input
	 *                            stream.
	 */
	public void reload() {
		if (_filename == null && transformer != null) {
			throw new ValidatorException("Can't reload an XSLT validator based on a stream");
		}
		if (_filename != null) {
			if (transformer == null) {
				logger.debug("Loading {}", _filename);
			} else {
				logger.info("Reloading {}", _filename);
			}
		} else {
			logger.info("Loading XSLT validator from stream");
		}
		transformer = setupTransformer();
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
			SAXResult parseResult = new SAXResult(new SVRLHandler(result));
			transformer.transform(new StreamSource(new ByteArrayInputStream(source)), parseResult);
		} catch (net.sf.saxon.type.ValidationException valError) {
			// Report this as an error
			result.addError("Error during schematron validation: " + valError.getMessage(), "Schematron validation",
					null, null, null);
		} catch (TransformerException tfError) {
			// Report it as an error if the issue was XML parsing
			Throwable t = tfError;
			while (t != null) {
				if (t instanceof SAXParseException) {
					SAXParseException s = (SAXParseException) t;
					result.addError(t.getMessage(), "XML Parsing", s.getLineNumber(), s.getColumnNumber(), null);
					return result;
				} else if (t instanceof XPathException) {
					result.addError(t.getMessage(), "XPath error", null, null, null);
					return result;
				}
				t = t.getCause();
			}
			logger.error("Validation against SVRL Stylesheet failed", tfError);
			tfError.printStackTrace();
			throw new ValidatorException("Error performing XSLT transformation" + tfError.toString(), tfError);
		}
		logger.debug("Validated document against SCH/XSLT {}: {} errors, {} warnings", _filename, result.errorCount(),
				result.warningCount());
		return result;
	}

	/**
	 * Initialize the Saxon Transformer
	 */
	protected Transformer setupTransformer() {
		try {
			// Read the file as bytes, as we'll need to go through it twice
			// and inputstreams are not reliable regarding mark() and reset()
			byte[] fileData;
			if (_filename != null) {
				fileData = Files.readAllBytes(Paths.get(_filename));
			} else {
				fileData = _stream.readAllBytes();
			}
			return loadTransformer(new ByteArrayInputStream(fileData));
		} catch (IOException error) {
			logger.error("Failed to set up XSLT transformer", error);
			throw new ValidatorException("Error setting up XSLT transformer validator for " + _filename, error);
		}
	}

	/**
	 * Load the transformer in the given input stream.
	 * 
	 * @param inputStream The input stream containing the XSLT
	 * @throws ValidatorException if the transformer cannot be initialized from the
	 *                            given stream data
	 */
	protected Transformer loadTransformer(InputStream inputStream) {
		try {
			TransformerFactory transformFactory = TransformerFactory.newInstance();

			transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			transformFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

			// Do a very quick check on the input file: it must be an SVRL stylesheet
			SAXParserFactory checkFactory = SAXParserFactory.newInstance();
			checkFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			checkFactory.setNamespaceAware(true);
			SAXParser parser = checkFactory.newSAXParser();
			SVRLCheckHandler svrlCheckHandler = new SVRLCheckHandler();

			inputStream.mark(Integer.MAX_VALUE);
			parser.parse(inputStream, svrlCheckHandler);
			if (!svrlCheckHandler.isSVRLStylesheet()) {
				throw new ValidatorException("Stylesheet does not appear to be SVRL Stylesheet: " + _filename);
			}
			inputStream.reset();
			Transformer transformer = transformFactory.newTransformer(new StreamSource(inputStream));
			transformer.setParameter(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			transformer.setErrorListener(new IgnoreErrorHandler());
			return transformer;
		} catch (ParserConfigurationException | SAXException | IOException | TransformerConfigurationException error) {
			logger.error("Failed to load XSLT transformer", error);
			throw new ValidatorException("Error setting up XSLT transformer validator for " + _filename, error);
		}
	}

}
