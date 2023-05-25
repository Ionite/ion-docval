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
	volatile private HashMap<String, ValidatorManagerEntry> _validators;

	/** The mapping of keywords to validation lists */
	volatile private HashMap<String, ArrayList<String>> _validationLists;

	/**
	 * In order to keep serving data during a (potentially long) reload action, we
	 * keep a bit of state regarding reloads. When a reload is triggered, we set
	 * this state to 'true'. When 'true', any action that would add a validator or
	 * validation file to the live structures adds them to the temporary structures
	 * instead. When done, the live structures are replaced by the temporary ones
	 */
	boolean reloadingFullConfiguration = false;
	/** Temporary map of validators, used when reloading */
	private HashMap<String, ValidatorManagerEntry> _tmpValidators;
	/** Temporary mapping of keywords to validation lists, used when reloading */
	private HashMap<String, ArrayList<String>> _tmpValidationLists;

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
	 * Apply the given configuration data
	 * 
	 * @param configData the configuration data to apply
	 * @throws IOException        If there is an I/O error reading any file
	 *                            specified in the configuration data
	 * @throws ConfigurationError if the configuration itself contains an error.
	 */
	public void applyConfig(ConfigData configData) throws IOException, ConfigurationError {
		startReload();
		for (ConfigData.DocumentType docType : configData.documentTypes) {
			logger.info("Loading document type {} with keyword {}", docType.name, docType.keyword);
			if (haveValidatorsForKeyword(docType.keyword)) {
				throw new ConfigurationError("Duplicate Keyword for " + docType.name + ": " + docType.keyword);
			}
			for (String validationFile : docType.validationFiles) {
				logger.info("Adding validation file {} to {}", validationFile, docType.name);
				addValidator(docType.keyword, validationFile, true);
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
		setAutoReload(configData.autoReload);
		setUnknownKeywords(configData.unknownKeywords);
		finishReload();
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
	public boolean haveValidatorsForKeyword(String keyword) {
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
		HashMap<String, ValidatorManagerEntry> validators;
		HashMap<String, ArrayList<String>> validationLists;

		if (reloadingFullConfiguration) {
			validators = _tmpValidators;
			validationLists = _tmpValidationLists;
		} else {
			validators = _validators;
			validationLists = _validationLists;
		}

		ArrayList<String> validatorsForKeyword = validationLists.get(keyword);
		if (validatorsForKeyword == null) {
			validatorsForKeyword = new ArrayList<String>();
			validationLists.put(keyword, validatorsForKeyword);
			logger.debug("[XX] ADDING KEYWORD TO LIST");
			logger.debug("[XX] CURRENTLY BUILT LIST: " + validationLists);
		}
		if (!validatorsForKeyword.contains(fileName)) {
			validatorsForKeyword.add(fileName);
		}
		if (!lazyLoad) {
			validators.get(fileName);
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
		HashMap<String, ValidatorManagerEntry> validators;
		HashMap<String, ArrayList<String>> validationLists;

		if (reloadingFullConfiguration) {
			validators = _tmpValidators;
			validationLists = _tmpValidationLists;
		} else {
			validators = _validators;
			validationLists = _validationLists;
		}

		/* Still need to check whether we have the keyword */
		ArrayList<String> validatorsForKeyword = _validationLists.get(keyword);
		if (validatorsForKeyword == null) {
			validatorsForKeyword = new ArrayList<String>();
			validationLists.put(keyword, validatorsForKeyword);
		}
		if (!validatorsForKeyword.contains(fileName)) {
			validatorsForKeyword.add(fileName);
		}
		// But in this case, we always simply put it in the map of validators

		ValidatorManagerEntry entry = new ValidatorManagerEntry(fileName, validator);
		validators.put(fileName, entry);
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
                switch (unknownKeywords) {
                case WARN:
                    result.addWarning("Unable to derive document type keyword: " + derivationError.toString(), null, null, null,
                            "Validator selection");
                    break;
                case ERROR:
                    result.addError("Unable to derive document type keyword: " + derivationError.toString(), null, null, null,
                            "Validator selection");
                    break;
                case FAIL:
                    throw derivationError;
                case IGNORE:
                    break;
                }
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

	private void startReload() {
		logger.info("Starting full (re)load of configuration");
		reloadingFullConfiguration = true;
		_tmpValidators = new HashMap<String, ValidatorManagerEntry>();
		_tmpValidationLists = new HashMap<String, ArrayList<String>>();
	}

	private synchronized void finishReload() {
		_validators = _tmpValidators;
		_tmpValidators = null;
		_validationLists = _tmpValidationLists;
		_tmpValidationLists = null;
		logger.info("Finished full (re)load of configuration");
		reloadingFullConfiguration = false;
	}
};
