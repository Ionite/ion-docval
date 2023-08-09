package net.ionite.docval.validation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ionite.docval.config.ConfigData;
import net.ionite.docval.config.ConfigurationError;
import net.ionite.docval.validation.validator.DocumentValidator;
import net.ionite.docval.validation.validator.SCHValidator;
import net.ionite.docval.validation.validator.XSDValidator;
import net.ionite.docval.validation.validator.XSLTValidator;
import net.ionite.docval.xml.KeywordDeriver;

/**
 * The validator manager holds any number of validators for a specific keyword,
 * with an option to automatically reload the underlying validation definition
 * (such as an xsd or xslt).
 * 
 * It keeps any validator in memory, so that it can be reused for multiple
 * keywords
 */
public class ValidatorManager {

	/** The loaded validators based on their filename */
	private HashMap<String, ValidatorManagerEntry> _validators;

	/** The mapping of keywords to validation lists */
	private HashMap<String, ArrayList<String>> _validationLists;

	/**
	 * If true, automatically check whether entries need to be reloaded
	 */
	private boolean _autoReload = false;

	/**
	 * Specify the way unknown keywords (i.e. unknown document types) are handled.
	 */
	private ConfigData.UnknownKeywords unknownKeywords = ConfigData.UnknownKeywords.FAIL;

	private Logger logger;

	/**
	 * Inner class to hold a single validator, along with the file it was loaded
	 * from and the time it was loaded
	 */
	private class ValidatorManagerEntry {
		private long _lastModified;
		private String _fileName;
		private DocumentValidator _validator;

		public ValidatorManagerEntry(String fileName) {
			_fileName = fileName;
			loadValidator();
		}

		// TODO: consider making these real classes and add an inputstream version
		/**
		 * Creates a validator entry with a given name, this is probably only useful for
		 * unit testing, as it will fail on reload
		 * 
		 * @param name      The fake filename for the entry
		 * @param validator The initialized DocumentValidator implementation instance
		 */
		public ValidatorManagerEntry(String name, DocumentValidator validator) {
			_fileName = name;
			_validator = validator;
		}

		public DocumentValidator getValidator() {
			return _validator;
		}

		public void loadValidator() {
			_lastModified = new File(_fileName).lastModified();
			if (_fileName.endsWith(".xsd")) {
				_validator = new XSDValidator(_fileName);
			} else if (_fileName.endsWith(".xslt") || _fileName.endsWith(".xsl")) {
				_validator = new XSLTValidator(_fileName);
			} else if (_fileName.endsWith(".sch")) {
				_validator = new SCHValidator(_fileName);
			} else {
				throw new ValidatorException(
						"Unsupported validator file extension, must be .xsd, .xsl, or .xslt: " + _fileName);
			}
		}

		public void checkReload() {
			long lastModified = new File(_fileName).lastModified();
			if (lastModified > _lastModified) {
				loadValidator();
			}
		}
	}

	/**
	 * Inner class to perform validator loading
	 * This class allows loading of new validators
	 * while the old list of validators is still in use,
	 * then replace all of them at once
	 */
	private class ValidatorLoader {
		/** The loaded validators based on their filename */
		private HashMap<String, ValidatorManagerEntry> validators;

		/** The mapping of keywords to validation lists */
		private HashMap<String, ArrayList<String>> validationLists;

		public ValidatorLoader() {
			validators = new HashMap<String, ValidatorManagerEntry>();
			validationLists = new HashMap<String, ArrayList<String>>();
		}

		public void addValidator(String keyword, String fileName, boolean lazyLoad) {
			ArrayList<String> validatorsForKeyword = validationLists.get(keyword);
			if (validatorsForKeyword == null) {
				validatorsForKeyword = new ArrayList<String>();
				validationLists.put(keyword, validatorsForKeyword);
			}
			if (!validatorsForKeyword.contains(fileName)) {
				validatorsForKeyword.add(fileName);
			}
			if (!lazyLoad) {
				validators.get(fileName);
			}
		}

		public boolean hasValidatorsForKeyword(String keyword) {
			return validationLists.containsKey(keyword);
		}

		public HashMap<String, ValidatorManagerEntry> getValidators() {
			return validators;
		}

		public HashMap<String, ArrayList<String>> getValidationLists() {
			return validationLists;
		}
	}

	/**
	 * Construct a new ValidatorManager
	 */
	public ValidatorManager() {
		logger = LoggerFactory.getLogger(this.getClass().getName());
		_validators = new HashMap<String, ValidatorManagerEntry>();
		_validationLists = new HashMap<String, ArrayList<String>>();
	}

	/**
	 * Enable or disable autoreload. Autoreload will automatically reload validation
	 * files if they change on disk.<br />
	 * When autoreload is enabled, each time a validation file is used, the
	 * last-modified time of the on-disk file is checked, and the file is reloaded
	 * if it has been changed.<br />
	 * Note that this does perform any checks on the main configuration file: any
	 * change there needs a full reload of the entire program. Autoreload can only
	 * reload already-configured files.
	 * 
	 * @param value boolean specifying whether to enable or disable autoreload
	 */
	public void setAutoReload(boolean value) {
		logger.debug("Autoreload set to {}", value);
		_autoReload = value;
	}

	/**
	 * Set the way the validator manager should handle requests for unknown
	 * keywords.<br />
	 * 
	 * The keyword specifies which validation(s) to run, and can be derived
	 * automatically from documents.
	 * 
	 * @param unknownKeywords The way to handle unknown keywords.
	 */
	public void setUnknownKeywords(ConfigData.UnknownKeywords unknownKeywords) {
		logger.debug("Allow unknown set to {}", unknownKeywords);
		this.unknownKeywords = unknownKeywords;
	}

	/**
	 * Apply the given configuration data.
	 *
	 * If successful, this removes and replaces all validators and keywords
	 * that were added through earlier calls to any of applyConfig() or
	 * addValidator().
	 *
	 * If it fails, the currently running set is kept.
	 *
	 * @param configData the configuration data to apply
	 * @throws IOException        If there is an I/O error reading any file
	 *                            specified in the configuration data
	 * @throws ConfigurationError if the configuration itself contains an error.
	 */
	public void applyConfig(ConfigData configData) throws IOException, ConfigurationError {
		ValidatorLoader loader = new ValidatorLoader();

		for (ConfigData.DocumentType docType : configData.documentTypes) {
			logger.info("Loading document type {} with keyword {}", docType.name, docType.keyword);
			if (loader.hasValidatorsForKeyword(docType.keyword)) {
				throw new ConfigurationError("Duplicate Keyword for " + docType.name + ": " + docType.keyword);
			}
			for (String validationFile : docType.validationFiles) {
				logger.info("Adding validation file {} to {}", validationFile, docType.name);
				loader.addValidator(docType.keyword, validationFile, true);
				// TODO: move to the loader?
				if (configData.lazyLoad) {
					// Only perform cursory checks
					File vf = new File(validationFile);
					FileReader reader = new FileReader(vf);
					reader.close();
				} else {
					// Load it by retrieving it once
					getValidator(validationFile);
				}
			}
		}
		_validators = loader.getValidators();
		_validationLists = loader.getValidationLists();

		setAutoReload(configData.autoReload);
		setUnknownKeywords(configData.unknownKeywords);
	}

	/**
	 * Returns the DocumentValidator instance for the given filename If not loaded
	 * yet, tries to load it.
	 */
	public DocumentValidator getValidator(String fileName) {
		logger.debug("Retrieving validator for {}", fileName);
		ValidatorManagerEntry entry = _validators.get(fileName);
		if (entry == null) {
			logger.debug("Validator for {} not loaded yet", fileName);
			entry = new ValidatorManagerEntry(fileName);
			_validators.put(fileName, entry);
		} else if (_autoReload) {
			logger.debug("Validator for {} loaded, checking whether a reload is necessary", fileName);
			entry.checkReload();
		}
		return entry.getValidator();
	}

	/**
	 * Check whether there are validators for the given keyword
	 * 
	 * @param keyword The keyword to check
	 * @return true if there are validators configured for the keyword, false if not
	 */
	public boolean hasValidatorsForKeyword(String keyword) {
		return _validationLists.containsKey(keyword);
	}

	/**
	 * Adds the Validator for the given file to the list of entries for the given
	 * keyword. Creates the keyword entry if it doesn't exist. No-op if the
	 * validator file is already in the list for this keyword.
	 * 
	 * @param keyword  The keyword to add this valdidator to.
	 * @param fileName File name of the validation file
	 * @param lazyLoad if false, immediately load the validation file. If true, load
	 *                 it upon first use.
	 */
	public void addValidator(String keyword, String fileName, boolean lazyLoad) {
		ArrayList<String> validatorsForKeyword = _validationLists.get(keyword);
		if (validatorsForKeyword == null) {
			validatorsForKeyword = new ArrayList<String>();
			_validationLists.put(keyword, validatorsForKeyword);
		}
		if (!validatorsForKeyword.contains(fileName)) {
			validatorsForKeyword.add(fileName);
		}
		if (!lazyLoad) {
			_validators.get(fileName);
		}
	}

	/**
	 * Add a pre-initialized validator for a given (potentially fake) filename
	 * 
	 * @param keyword   The keyword to add this validator to
	 * @param fileName  The (potentially fake) filename that will be used to
	 *                  reference this validator
	 * @param validator An instance of a DocumentValidator implementation class
	 */
	public void addValidator(String keyword, String fileName, DocumentValidator validator) {
		/* Still need to check whether we have the keyword */
		ArrayList<String> validatorsForKeyword = _validationLists.get(keyword);
		if (validatorsForKeyword == null) {
			validatorsForKeyword = new ArrayList<String>();
			_validationLists.put(keyword, validatorsForKeyword);
		}
		if (!validatorsForKeyword.contains(fileName)) {
			validatorsForKeyword.add(fileName);
		}
		// But in this case, we always simply put it in the map of validators

		ValidatorManagerEntry entry = new ValidatorManagerEntry(fileName, validator);
		_validators.put(fileName, entry);
	}

	/**
	 * Return the names of the validators for the given keyword
	 * 
	 * @param keyword The keyword to get the validator names for
	 * @return ArrayList of strings with the validator names configured for the
	 *         given keyword
	 */
	private ArrayList<String> getValidatorNamesForKeyword(String keyword) {
		ArrayList<String> names = _validationLists.get(keyword);
		if (names == null) {
			return new ArrayList<String>();
		}
		return names;
	}

	/**
	 * Return the validators for the given keyword
	 * 
	 * @param keyword the keyword to get the validators for
	 * @return ArrayList of DocumentValidator implementation instances, configured
	 *         for the given keyword. Returns an empty list if the keyword is not
	 *         known.
	 */
	public ArrayList<DocumentValidator> getValidatorsForKeyword(String keyword) {
		ArrayList<DocumentValidator> result = new ArrayList<DocumentValidator>();
		for (String validatorName : getValidatorNamesForKeyword(keyword)) {
			result.add(getValidator(validatorName));
		}
		return result;
	}

	/**
	 * Validate the given XML data for the given keyword.
	 * 
	 * @param keyword The keyword that selects which validation(s) to execute
	 * @param source  byte-array containing the XML document
	 * @return ValidationResult The result of the validation
	 * @throws ValidatorException if there is no configuration for the given
	 *                            keyword, and the manager is configured to raise an
	 *                            exception in that case.
	 */
	public ValidationResult validate(String keyword, byte[] source) {
		ValidationResult result = new ValidationResult();
		if (keyword == null) {
			try {
				KeywordDeriver kwd = new KeywordDeriver();
				keyword = kwd.deriveKeyword(source);
			} catch (ValidatorException derivationError) {
				String msg = "Unable to derive document type keyword: " + derivationError.toString();
				if (msg.contains("Content is not allowed in prolog")) {
					msg = msg + " There may be a mismatch between the encoding of the XML data and the encoding in the XML declaration.";
				}
				switch (unknownKeywords) {
				case WARN:
					result.addWarning(msg, null, null, null,
							"Validator selection");
					break;
				case ERROR:
					result.addError(msg, null, null, null,
							"Validator selection");
					break;
				case FAIL:
					throw derivationError;
				case IGNORE:
					break;
				}
				// Don't continue if we can't even derive the keyword.
				return result;
			}
		}

		ArrayList<String> validatorNames = getValidatorNamesForKeyword(keyword);
		if (validatorNames.isEmpty()) {
			logger.info("No document type configured with keyword: " + keyword + ", raising exception");
			switch (unknownKeywords) {
			case WARN:
				result.addWarning("No document type configured with keyword: " + keyword, null, null, null,
						"Validator selection");
				break;
			case ERROR:
				result.addError("No document type configured with keyword: " + keyword, null, null, null,
						"Validator selection");
				break;
			case FAIL:
				throw new ValidatorException("No document type configured with keyword: " + keyword);
			case IGNORE:
				break;
			}
		} else {
			for (String validatorName : validatorNames) {
				getValidator(validatorName).validate(source, result);
			}
		}
		return result;
	}

};
