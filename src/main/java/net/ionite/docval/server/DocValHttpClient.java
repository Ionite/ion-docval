package net.ionite.docval.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ionite.docval.validation.ValidationResult;

/**
 * A client to the ion-docval server
 * 
 * @author Ionite
 *
 */
public class DocValHttpClient {
	private String uri = "http://localhost:35791/api/validate";
	private Logger logger;

	/**
	 * Construct a client with a default server URI:
	 * http://localhost:35791/api/validate
	 */
	public DocValHttpClient() {
		logger = LoggerFactory.getLogger(this.getClass().getName());
	}

	/**
	 * Construct a client with the given server URI
	 * 
	 * @param uri The URI where the ion-docval-server is hosted
	 */
	public DocValHttpClient(String uri) {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		this.uri = uri;
	}

	/**
	 * Send the given XML document to the server for validation, using a keyword
	 * derived from the document (see {@link net.ionite.docval.xml.KeywordDeriver}
	 * for more information on keyword derivation). Parses the server's response
	 * into a ValidationResult object.
	 * 
	 * @param documentData Byte-array containing the XML document to validate
	 * @return ValidationResult The validation result from the server
	 * @throws DocValClientException If the server could not be reached, or it did
	 *                               not return a validation result.
	 */
	public ValidationResult validate(byte[] documentData) throws DocValClientException {
		return validate(documentData, null);
	}

	/**
	 * Send the given XML document to the server for validation, using the given
	 * keyword. Parses the server's response into a ValidationResult object.
	 * 
	 * @param documentData Byte-array containing the XML document to validate
	 * @param keyword      The keyword the server uses to select which validation to
	 *                     use
	 * @return ValidationResult The validation result from the server
	 * @throws DocValClientException If the server could not be reached, or it did
	 *                               not return a validation result.
	 */
	public ValidationResult validate(byte[] documentData, String keyword) throws DocValClientException {
		try {
			if (keyword == null) {
				logger.debug("Sending document of " + documentData.length + " bytes to server at " + uri
						+ ", derive keyword");
			} else {
				logger.debug("Sending document of " + documentData.length + " bytes to server at " + uri
						+ " with keyword " + keyword);
			}
			if (keyword != null) {
				uri += "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
			}
			HttpRequest request = HttpRequest.newBuilder().uri(new URI(uri)).header("Accept", "application/json")
					.header("Content-Type", "application/xml")
					.POST(HttpRequest.BodyPublishers.ofByteArray(documentData)).build();

			HttpResponse<String> response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString());

			logger.debug("Status code from " + uri + ": " + response.statusCode());

			return ValidationResult.fromJSONString(response.body());
		} catch (URISyntaxException e) {
			throw new DocValClientException("Malformed URI: " + uri, e);
		} catch (IOException e) {
			throw new DocValClientException("Error communicating with server at: " + uri, e);
		} catch (InterruptedException e) {
			throw new DocValClientException("Client request interrupted", e);
		}
	}
}
