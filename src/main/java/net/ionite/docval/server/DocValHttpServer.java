package net.ionite.docval.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import net.ionite.docval.config.ConfigData;
import net.ionite.docval.config.ConfigData.Listen;
import net.ionite.docval.config.ConfigReader;
import net.ionite.docval.config.ConfigurationError;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.ValidatorManager;
import net.ionite.docval.xml.KeywordDeriver;
import net.sf.saxon.s9api.SaxonApiException;

enum ContentType {
	XML, JSON
}

/**
 * HTTP Server for bulk- or continuous validation.
 */
public class DocValHttpServer extends Thread {
	private Logger logger;
	private ValidatorManager validatorManager;
	private ArrayList<HttpServer> listeners;
	private String configFile = null;
	private ConfigData configData = null;

	private void respondToRequest(HttpExchange t, String responseContent, int statusCode) throws IOException {
		byte[] responseBytes = responseContent.getBytes();
		t.sendResponseHeaders(statusCode, responseBytes.length);

		OutputStream os = t.getResponseBody();
		os.write(responseBytes);
		os.close();
	}

	class IndexHandler implements HttpHandler {
		private Logger logger;

		public IndexHandler() {
			logger = LoggerFactory.getLogger(this.getClass().getName());
		}

		private byte[] readLocalFileData(String localPath) {
			try {
				ClassLoader classLoader = getClass().getClassLoader();
				InputStream is = classLoader.getResourceAsStream(localPath);
				return is.readAllBytes();
			} catch (IOException ioe) {
				return "Error reading template".getBytes();
			}
		}

		private void GET(HttpExchange t) throws IOException {
			logger.debug("received GET request");
			// Just return the standard form
			byte[] formData = readLocalFileData("html/index.html");
			logger.debug("read standard response data (", formData.length, "bytes )");
			t.sendResponseHeaders(200, formData.length);
			OutputStream os = t.getResponseBody();
			os.write(formData);
			os.close();
			logger.debug("GET response data sent to client");
		}

		public void handle(HttpExchange t) throws IOException {
			logger.debug("IndexHandler.handle() called");
			try {
				String method = t.getRequestMethod();
				logger.debug("Request method: " + method);

				if ("GET".equals(method)) {
					GET(t);
				} else {
					respondToRequest(t, "\"Method " + method + " not allowed", 405);
				}
			} catch (Exception e) {
				logger.error("Exception while handling client request", e);
				throw e;
			}
		}
	}

	class DocumentTypesHandler implements HttpHandler {
		private Logger logger;
		private ConfigData configData;

		public DocumentTypesHandler(ConfigData configData) {
			logger = LoggerFactory.getLogger(this.getClass().getName());
			this.configData = configData;
		}

		private void GET(HttpExchange t) throws IOException {
			Headers requestHeaders = t.getRequestHeaders();
			Headers responseHeaders = t.getResponseHeaders();

			String accept = requestHeaders.getFirst("Accept");
			// We support three output formats: json, xml and html, and default to html?
			int statusCode = 200;
			String responseData = "";

			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				responseHeaders.set("Content-Type", accept);
				try {
					responseData = configData.documentTypesAsXMLString();
				} catch (SaxonApiException e) {
					logger.error("Unable to convert document types to XML", e);
					responseData = "<Error><Detail>Unable to convert document types to XML</Detail></Error>";
					statusCode = 500;
				}
			} else if (accept.equals("application/json") || accept.equals("*/*")) {
				responseHeaders.set("Content-Type", "application/json");
				responseData = configData.documentTypesAsJSONString();
			} else {
				respondToRequest(t, "\"Could not satisfy the request Accept header '" + accept + "' \"", 406);
				return;
			}
			respondToRequest(t, responseData, statusCode);
		}

		public void handle(HttpExchange t) throws IOException {
			logger.debug("DocumentTypesHandler.handle() called");
			try {
				String method = t.getRequestMethod();
				logger.debug("Request method: " + method);

				if ("GET".equals(method)) {
					GET(t);
				} else {
					respondToRequest(t, "\"Method " + method + " not allowed", 405);
				}
			} catch (Exception e) {
				logger.error("Exception while handling client request", e);
				throw e;
			}
		}
	}

	class ValidatorHandler implements HttpHandler {
		private Logger logger;
		private ValidatorManager validatorManager;

		public ValidatorHandler(ValidatorManager validatorManager) {
			this.validatorManager = validatorManager;
			logger = LoggerFactory.getLogger(this.getClass().getName());
		}

		/*
		 * Return a HashMap of the query parameters Parameter values are URLDecoded
		 */
		private HashMap<String, String> readRequestParameters(HttpExchange t) {
			HashMap<String, String> result = new HashMap<String, String>();
			String queryString = t.getRequestURI().getQuery();
			if (queryString == null) {
				return result;
			}

			int last = 0, next, l = queryString.length();
			while (last < l) {
				next = queryString.indexOf('&', last);
				if (next == -1)
					next = l;

				if (next > last) {
					int eqPos = queryString.indexOf('=', last);
					try {
						if (eqPos < 0 || eqPos > next)
							result.put(URLDecoder.decode(queryString.substring(last, next), "utf-8"), "");
						else
							result.put(URLDecoder.decode(queryString.substring(last, eqPos), "utf-8"),
									URLDecoder.decode(queryString.substring(eqPos + 1, next), "utf-8"));
					} catch (UnsupportedEncodingException e) {
						throw new ValidatorException("Unable to decode URL string", e);
					}
				}
				last = next + 1;
			}
			return result;
		}

		private byte[] readRequestBody(HttpExchange t) throws IOException {
			InputStream is = t.getRequestBody();
			byte[] inBuffer = new byte[8192];
			ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();

			int readBytes = 0;
			while ((readBytes = is.read(inBuffer, 0, inBuffer.length)) != -1) {
				outBuffer.write(inBuffer, 0, readBytes);
			}
			return outBuffer.toByteArray();
		}

		private void POST(HttpExchange t) throws IOException {
			Headers requestHeaders = t.getRequestHeaders();
			Headers responseHeaders = t.getResponseHeaders();

			int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
			byte[] inputData;

			String contentType = requestHeaders.getFirst("Content-Type");
			if (contentType.startsWith("application/xml") || contentType.startsWith("text/xml")) {
				inputData = readRequestBody(t);
				if (inputData.length != contentLength) {
					throw new IOException("Data in HTTP POST body differs from length of Content-Length header");
				}
			} else {
				respondToRequest(t, "\"Unsupported Content-Type: '" + contentType + "' \"", 415);
				return;
			}

			String accept = requestHeaders.getFirst("Accept");
			// We support three output formats: json, xml and html, and default to html?
			int statusCode = 200;
			String responseData = "";
			ContentType responseContentType;

			if (accept.equals("application/xml") || accept.equals("text/xml")) {
				responseHeaders.set("Content-Type", accept);
				responseContentType = ContentType.XML;
			} else if (accept.equals("application/json") || accept.equals("*/*")) {
				responseHeaders.set("Content-Type", "application/json");
				responseContentType = ContentType.JSON;
			} else {
				respondToRequest(t, "\"Could not satisfy the request Accept header '" + accept + "' \"", 406);
				return;
			}

			// Determine output format based on Accept: header

			// If the keyword is not specified, derive automatically
			String keyword = readRequestParameters(t).getOrDefault("keyword", null);

			try {
				switch (responseContentType) {
				case XML:
					try {
						responseData = validatorManager.validate(keyword, inputData).toXMLString();
					} catch (SaxonApiException saxError) {
						logger.error("Unable to convert validation results to XML", saxError);
						responseData = "<Error><Detail>Unable to convert validation results to XML</Detail></Error>";
					}
					break;
				case JSON:
					responseData = validatorManager.validate(keyword, inputData).toJSONString();
					break;
				}

				statusCode = 200;
			} catch (ValidatorException valError) {
				logger.error("Error processing request: " + valError.getMessage(), valError);
				responseData = "Error: " + valError.getMessage();
				statusCode = 400;
			}

			respondToRequest(t, responseData, statusCode);
		}

		public void handle(HttpExchange t) throws IOException {
			logger.debug("ValidatorHandler.handle() called");
			try {
				String method = t.getRequestMethod();
				logger.debug("Request method: " + method);

				if ("POST".equals(method)) {
					POST(t);
				} else {
					respondToRequest(t, "\"Method " + method + " not allowed", 405);
				}
			} catch (Exception e) {
				logger.error("Exception while handling client request", e);
				throw e;
			}
		}
	}

	/**
	 * Initialize a server with the configuration in the given configuration file.
	 * 
	 * @param configFile The configuration file to read
	 * @throws IOException        if the server could not be started
	 * @throws ConfigurationError if there is an error in the configuration
	 */
	public DocValHttpServer(String configFile) throws IOException, ConfigurationError {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		listeners = new ArrayList<HttpServer>();

		this.configFile = configFile;
		setValidatorManager(new ValidatorManager());
		this.loadConfigFile();

		/* For now, listeners are only started once */
		if (configData.server == null || configData.server.listen == null || configData.server.listen.size() == 0) {
			throw new ConfigurationError("No Server or Listen section in provided configuration file " + configFile);
		}
		for (Listen listen : configData.server.listen) {
			addListener(listen.address, listen.port);
		}
		setValidatorManager(validatorManager);
	}

	/**
	 * Initialize a server with the configuration in the given configuration data.
	 * 
	 * @param configData The configuration data to use
	 * @throws IOException        if the server could not be started
	 * @throws ConfigurationError if there is an error in the configuration
	 */
	public DocValHttpServer(ConfigData configData) throws IOException, ConfigurationError {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		listeners = new ArrayList<HttpServer>();

		this.configData = configData;

		ValidatorManager validatorManager = new ValidatorManager();
		validatorManager.applyConfig(configData);
		setValidatorManager(validatorManager);
	}

	/**
	 * Initialize a server with a validator manager initialized by the caller. This
	 * server does not start listening automatically, the caller must call
	 * addListener() as well.
	 * 
	 * @param validatorManager the validator manager to use for validation
	 */
	public DocValHttpServer(ValidatorManager validatorManager) {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		listeners = new ArrayList<HttpServer>();
		setValidatorManager(validatorManager);
	}

	/* this only loads the validation config, not the server listeners! */
	/**
	 * (Re)load the validation part of the configuration file used in the
	 * initializer (if any).<br />
	 * <br />
	 * Note that this does *not* reconfigure or re-initialize the listeners
	 * specified in the configuration.
	 */
	public void loadConfigFile() throws IOException, ConfigurationError {
		if (configFile != null) {
			ConfigReader configReader = new ConfigReader(configFile);
			ConfigData newConfigData = configReader.readConfig();
			validatorManager.applyConfig(newConfigData);
			configData = newConfigData;
		}
	}

	/**
	 * Replace the validator manager with the given one
	 */
	public void setValidatorManager(ValidatorManager validatorManager) {
		this.validatorManager = validatorManager;
	}

	/**
	 * Add the given IP address/hostname and port number to the listeners.
	 * 
	 * @throws IOException if the port could not be opened for listening.
	 */
	public void addListener(String host, int port) throws IOException {
		logger.info("ion-docval HTTP server binding to " + host + " port " + port);
		HttpServer listener = HttpServer.create(new InetSocketAddress(host, port), 100);
		listener.createContext("/validate", new IndexHandler());
		listener.createContext("/api/validate", new ValidatorHandler(validatorManager));
		if (configData != null) {
			listener.createContext("/api/document_types", new DocumentTypesHandler(configData));
		}
		listener.setExecutor(null); // creates a default executor
		listeners.add(listener);
	}

	/**
	 * Start the server on all the configured listeners
	 */
	public void start() {
		for (HttpServer listener : listeners) {
			listener.start();
			logger.info("ion-docval-server listening on " + listener.getAddress().getHostName() + " port "
					+ listener.getAddress().getPort());
		}
	}

	/**
	 * Stop the server
	 */
	public void halt(int delay) {
		logger.info("ion-docval-server shutting down");
		for (HttpServer listener : listeners) {
			listener.stop(delay);
		}
	}
}
