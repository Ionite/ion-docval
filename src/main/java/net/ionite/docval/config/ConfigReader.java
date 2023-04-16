package net.ionite.docval.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.ionite.docval.config.ConfigData.UnknownKeywords;
import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidationResultItem;
import net.ionite.docval.validation.validator.XSDValidator;

/**
 * Reads a configuration file, into a ConfigData object.<br />
 * <br />
 * 
 * @author Ionite
 */
public class ConfigReader {
	private String _fileName;
	ConfigData config;
	private Logger logger;

	private class ConfigFileXMLHandler extends DefaultHandler {
		public ConfigData configData = new ConfigData();
		private StringBuilder _currentValue = new StringBuilder();
		private ConfigData.DocumentType _currentDocumentType;
		private String _currentAddress = null;
		private int _currentPort = 0;

		@Override
		public void characters(char ch[], int start, int length) {
			_currentValue.append(ch, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			_currentValue.setLength(0);
			switch (qName) {
			case "Server":
				configData.server = configData.new Server();
				break;
			case "DocumentType":
				_currentDocumentType = configData.new DocumentType();
				break;
			case "Keyword":
				break;
			case "ValidationFile":
				break;
			case "":
				break;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			switch (qName) {
			case "AutoReload":
				configData.autoReload = Boolean.parseBoolean(_currentValue.toString());
				break;
			case "UnknownKeywords":
				switch (_currentValue.toString().toLowerCase()) {
				case "warn":
					configData.unknownKeywords = UnknownKeywords.WARN;
					break;
				case "error":
					configData.unknownKeywords = UnknownKeywords.ERROR;
					break;
				case "fail":
					configData.unknownKeywords = UnknownKeywords.FAIL;
					break;
				case "ignore":
					configData.unknownKeywords = UnknownKeywords.IGNORE;
					break;
				}
				break;
			case "LazyLoad":
				configData.lazyLoad = Boolean.parseBoolean(_currentValue.toString());
				break;
			case "Server":
				break;
			case "Listen":
				configData.server.listen.add(configData.new Listen(_currentAddress, _currentPort));
				break;
			case "Address":
				_currentAddress = _currentValue.toString();
				break;
			case "Port":
				_currentPort = Integer.parseInt(_currentValue.toString());
				break;
			case "DocumentType":
				configData.documentTypes.add(_currentDocumentType);
				break;
			case "Name":
				_currentDocumentType.name = _currentValue.toString();
				break;
			case "Description":
				_currentDocumentType.description = _currentValue.toString();
				break;
			case "Keyword":
				_currentDocumentType.keyword = _currentValue.toString();
				break;
			case "ValidationFile":
				_currentDocumentType.validationFiles.add(_currentValue.toString());
				break;
			}
		}
	}

	/**
	 * Constructor for the configuration file reader
	 * 
	 * @param fileName The file to read the configuration from. Must be an XML file
	 *                 that is valid for the configuration xsd.
	 */
	public ConfigReader(String fileName) {
		logger = LoggerFactory.getLogger(ConfigReader.class);
		logger.info("Reading configuration from file " + fileName);

		_fileName = fileName;
		config = new ConfigData();
		config.documentTypes.add(config.new DocumentType());
	}

	/**
	 * Read the configuration file set in the constructor.
	 * 
	 * @throws ConfigurationError if the configuration data could not be parsed.
	 * @return ConfigData The configuration as set in the config file.
	 */
	public ConfigData readConfig() throws ConfigurationError {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			XSDValidator configXMLValidator = new XSDValidator(
					classLoader.getResourceAsStream("xsd/ion-docval-config.xsd"));
			ValidationResult vr = configXMLValidator.validate(Files.readAllBytes(Paths.get(_fileName)));
			for (ValidationResultItem item : vr.getWarnings()) {
				// TODO log instead of print
				System.err.println("Warning: " + item.message);
			}
			if (vr.errorCount() > 0) {
				String errorMessage = "";
				for (ValidationResultItem item : vr.getErrors()) {
					if (!errorMessage.equals("")) {
						errorMessage += ", ";
					}
					errorMessage += item.message;
				}
				throw new ConfigurationError("Error in configuration file " + _fileName + ": " + errorMessage);
			}

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			ConfigFileXMLHandler configXMLHandler = new ConfigFileXMLHandler();
			saxParser.parse(_fileName, configXMLHandler);
			return configXMLHandler.configData;
		} catch (IOException ioError) {
			throw new ConfigurationError("Error while reading configuration file: " + ioError.toString(), ioError);
		} catch (ParserConfigurationException parserError) {
			throw new ConfigurationError(
					"Error while reading configuration file" + _fileName + ": " + parserError.getMessage(),
					parserError);
		} catch (SAXException saxException) {
			throw new ConfigurationError(
					"Error while reading configuration file" + _fileName + ": " + saxException.getMessage(),
					saxException);
		}
	}
}
