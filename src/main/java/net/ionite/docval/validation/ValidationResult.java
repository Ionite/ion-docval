package net.ionite.docval.validation;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.sapling.SaplingDocument;
import net.sf.saxon.sapling.SaplingElement;
import net.sf.saxon.sapling.Saplings;

/**
 * Collects and serializes validation results.
 * 
 * This class holds results from validation, in the form of a list of errors and
 * warnings Each ValidationResultItem contains a description, and, if known, a
 * location (xpath), a line and column number, and the test that was performed.
 * 
 * @author Ionite
 *
 */
public class ValidationResult {
	/** The list of errors that were encountered during the validation */
	private ArrayList<ValidationResultItem> errors;
	/** The list of warnings that were encountered during the validation */
	private ArrayList<ValidationResultItem> warnings;
    /** The name of the document type, if set. May be null */
    private String _documentTypeName = null;

	/**
	 * Constructor
	 */
	public ValidationResult() {
		errors = new ArrayList<ValidationResultItem>();
		warnings = new ArrayList<ValidationResultItem>();
	}
    
    public void setDocumentTypeName(String name) {
        _documentTypeName = name;
    }

	/**
	 * Returns the number of errors
	 * 
	 * @return The number of errors
	 */
	public int errorCount() {
		return errors.size();
	}

	/**
	 * Returns the number of warnings
	 * 
	 * @return The number of warnings
	 */
	public int warningCount() {
		return warnings.size();
	}

	/**
	 * Add an error to the results
	 * 
	 * @param message  The error message
	 * @param location The error location (an xpath), may be null
	 * @param line     The line number of the error, may be null
	 * @param column   The column of the error, may be null
	 * @param test     The test that failed (either an xpath expression or a general
	 *                 String)
	 */
	public void addError(String message, String location, Integer line, Integer column, String test) {
		errors.add(new ValidationResultItem(message, location, line, column, test));
	}

	/**
	 * Add a warning to the results
	 * 
	 * @param message  The error message
	 * @param location The error location (an xpath), may be null
	 * @param line     The line number of the error, may be null
	 * @param column   The column of the error, may be null
	 * @param test     The test that failed (either an xpath expression or a general
	 *                 String)
	 */
	public void addWarning(String message, String location, Integer line, Integer column, String test) {
		warnings.add(new ValidationResultItem(message, location, line, column, test));
	}

	/**
	 * Returns the list of error items
	 * 
	 * @return An ArrayList of ValidationResultItem objects
	 */
	public ArrayList<ValidationResultItem> getErrors() {
		return errors;
	}

	/**
	 * Returns the list of warning items
	 * 
	 * @return An ArrayList of ValidationResultItem objects
	 */
	public ArrayList<ValidationResultItem> getWarnings() {
		return warnings;
	}

	/**
	 * Serialize this ValidationResult to JSON
	 * 
	 * @return the JSONObject representing this validation result
	 */
	@SuppressWarnings("unchecked") // JSONObject extends HashMap but is not generic itself
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
        if (_documentTypeName != null) {
            result.put("document_type", _documentTypeName);
        }
		result.put("error_count", errorCount());
		result.put("warning_count", warningCount());
		JSONArray errorList = new JSONArray();
		for (ValidationResultItem item : errors) {
			errorList.add(item.toJSON());
		}
		result.put("errors", errorList);
		JSONArray warningList = new JSONArray();
		for (ValidationResultItem item : warnings) {
			warningList.add(item.toJSON());
		}
		result.put("warnings", warningList);
		return result;
	}

	/**
	 * Deserialize the validation result object from the given JSON data
	 */
	@SuppressWarnings("unchecked") // JSONObject extends HashMap but is not generic itself
	public static ValidationResult fromJSON(JSONObject jsonData) throws ValidatorException {
		ValidationResult result = new ValidationResult();
		Iterator<JSONObject> iterator;

		JSONArray jsonErrors = (JSONArray) jsonData.get("errors");
		if (jsonErrors == null) {
			throw new ValidatorException("no 'errors' field in JSON data");
		}

		iterator = jsonErrors.iterator();

		while (iterator.hasNext()) {
			JSONObject err = iterator.next();
			Long lineLong = (Long) err.get("line");
			Long columnLong = (Long) err.get("column");
			result.addError((String) err.get("message"), (String) err.get("location"),
					lineLong != null ? lineLong.intValue() : null, columnLong != null ? columnLong.intValue() : null,
					(String) err.get("test"));
		}

		JSONArray jsonWarnings = (JSONArray) jsonData.get("warnings");
		if (jsonWarnings == null) {
			throw new ValidatorException("no 'errors' field in JSON data");
		}
		iterator = jsonWarnings.iterator();

		while (iterator.hasNext()) {
			JSONObject err = iterator.next();
			result.addWarning((String) err.get("message"), (String) err.get("location"), (Integer) err.get("line"),
					(Integer) err.get("column"), (String) err.get("test"));
		}

		return result;
	}

	/**
	 * Deserialize the validation result object from the given JSON string
	 */
	public static ValidationResult fromJSONString(String jsonString) throws ValidatorException {
		try {
			return fromJSON((JSONObject) new JSONParser().parse(jsonString));
		} catch (ParseException e) {
			throw new ValidatorException("JSON parse error: " + e.getMessage(), e);
		}
	}

	/**
	 * Serialize this ValidationResult to JSON, and return the JSON as a String
	 * 
	 * @return The String representation of the JSON for this validation result
	 */
	public String toJSONString() {
		return toJSON().toString();
	}

	private SaplingElement createErrorWarningXMLElement(String tag, ValidationResultItem item) {
		SaplingElement errorElement = Saplings.elem(tag).withChild(Saplings.elem("Message").withText(item.message))
				.withChild(Saplings.elem("Test").withText(item.test));
		if (item.location != null) {
			errorElement = errorElement.withChild(Saplings.elem("Location").withText(item.location));
		}
		if (item.line != null) {
			errorElement = errorElement.withChild(Saplings.elem("Line").withText(item.line.toString()));
		}
		if (item.column != null) {
			errorElement = errorElement.withChild(Saplings.elem("Column").withText(item.column.toString()));
		}
		return errorElement;
	}

	/**
	 * Serialize this ValidatonResult to XML, and return the XML as a String
	 * 
	 * @return The String representation of the XML for this validation result
	 * @throws SaxonApiException
	 */
	public String toXMLString() throws SaxonApiException {
		SaplingElement root = Saplings.elem("ValidationResult")
				.withChild(Saplings.elem("ErrorCount").withText(Integer.valueOf(errorCount()).toString()))
				.withChild(Saplings.elem("WarningCount").withText(Integer.valueOf(warningCount()).toString()));
        if (_documentTypeName != null) {
            root = root.withChild(Saplings.elem("DocumentType").withText(_documentTypeName));
        }
		if (errorCount() > 0) {
			SaplingElement errorsElement = Saplings.elem("Errors");
			for (ValidationResultItem item : errors) {
				errorsElement = errorsElement.withChild(createErrorWarningXMLElement("Error", item));
			}
			root = root.withChild(errorsElement);
		}
		if (warningCount() > 0) {
			SaplingElement warningsElement = Saplings.elem("Warnings");
			for (ValidationResultItem item : warnings) {
				warningsElement = warningsElement.withChild(createErrorWarningXMLElement("Warning", item));
			}
			root = root.withChild(warningsElement);
		}
		SaplingDocument doc = Saplings.doc().withChild(root);

		Processor processor = new Processor(false); // False = does not required a feature from a licensed version of
													// Saxon.
		Serializer serializer = processor.newSerializer();
		// Other properties found here:
		// http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/Serializer.Property.html
		serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");
		serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
		XdmNode xdmNode = doc.toXdmNode(processor);
		String result = serializer.serializeNodeToString(xdmNode);
		return result;
	}
};
