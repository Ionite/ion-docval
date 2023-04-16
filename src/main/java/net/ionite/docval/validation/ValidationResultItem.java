package net.ionite.docval.validation;

import org.json.simple.JSONObject;

/**
 * Holds the data for a single validation warning or error.
 * 
 * @author Ionite
 *
 */
public class ValidationResultItem {
	/** The message of the warning or error */
	public String message;
	/**
	 * The location of the warning or error, e.g. an XPath, can be null if now known
	 * or not relevant
	 */
	public String location;
	/**
	 * The line in the source document where the test failed. Can be null if the
	 * line is not known
	 */
	public Integer line;
	/**
	 * The column in the source document where the test failed. Can be null if the
	 * column is not known
	 */
	public Integer column;
	/**
	 * The failed test, can be an XPath expression or a general String, such as "XML
	 * Schema"
	 */
	public String test;

	/**
	 * Constructor for the validation result item
	 * 
	 * @param message  The message of the warning or error
	 * @param location The location of the warning or error, e.g. an XPath, can be
	 *                 null if now known or not relevant
	 * @param line     The line in the source document where the test failed. Can be
	 *                 null if the line is not known
	 * @param column   The column in the source document where the test failed. Can
	 *                 be null if the column is not known
	 * @param test     The failed test, can be an XPath expression or a general
	 *                 String, such as "XML Schema"
	 */
	public ValidationResultItem(String message, String location, Integer line, Integer column, String test) {
		this.message = message;
		this.location = location;
		this.line = line;
		this.column = column;
		this.test = test;
	}

	/**
	 * Serialize this validation result item to a JSONObject
	 * 
	 * @return The JSONObject representing this validation result item
	 */
	@SuppressWarnings("unchecked") // JSONObject extends HashMap but is not generic itself
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("test", this.test);
		result.put("message", this.message);
		if (this.location != null) {
			result.put("location", this.location);
		}
		if (this.line != null) {
			result.put("line", this.line);
		}
		if (this.column != null) {
			result.put("column", this.column);
		}
		return result;
	}
};
