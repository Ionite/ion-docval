
package net.ionite.docval.config;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.sapling.SaplingDocument;
import net.sf.saxon.sapling.SaplingElement;
import net.sf.saxon.sapling.Saplings;

/**
 * Configuration data class, for the several components of ion-docval
 * 
 * @author Ionite
 */
public class ConfigData {

	/**
	 * Defines the way to handle unknown keywords
	 */
	public enum UnknownKeywords {
		/** Process as usual, but add a warning to the ValidationResult */
		WARN,
		/** Process as usual, but add a error to the ValidationResult */
		ERROR,
		/** Fail with a ValidatorException */
		FAIL,
		/** Ignore the document without any errors or warnings */
		IGNORE
	}

	/**
	 * If set to true, the ValidatorManager will automatically reload validation
	 * files when they have changed on disk
	 */
	public boolean autoReload = false;
	/**
	 * If set to true, validation will not validate documents with an unknown
	 * keyword, but return 0 errors and a single warning
	 */
	public UnknownKeywords unknownKeywords = UnknownKeywords.FAIL;
	/**
	 * If set to true, validation files are not loaded until the first time they are
	 * accessed though cursory checks (existence, read access) are performed
	 */
	public boolean lazyLoad = false;
	/** Validation server configuration */
	public Server server;

	/** The document types that are configured to be validated */
	public ArrayList<DocumentType> documentTypes;

	/** Listen options for the server configuration */
	public class Listen {
		/** The IP address to listen on */
		public String address;
		/** The port number to listen on */
		public int port;

		/**
		 * Constructor for the Listen options
		 * 
		 * @param address The IP address to listen on
		 * @param port    The port number to listen on
		 */
		public Listen(String address, int port) {
			this.address = address;
			this.port = port;
		}
	}

	/** Server configuration */
	public class Server {
		/** The listen options, one entry for each address/port combination */
		public ArrayList<Listen> listen;

		/**
		 * Constructor for the Server options
		 */
		public Server() {
			listen = new ArrayList<Listen>();
		}
	}

	/**
	 * Document type information for the validator. A DocumentType entry specifies
	 * the keyword and a list of validation files to validate against
	 * 
	 * @author Ionite
	 *
	 */
	public class DocumentType {
		/**
		 * The (short) name for this document type, such as "SI-UBL 2.0" or "Peppol BIS
		 * 3 Invoice"
		 */
		public String name;
		/**
		 * A description of this document type. MAy be null or empty
		 */
		public String description;
		/**
		 * The keyword that is used to match a given document to a set of validation
		 * files
		 * 
		 * In principle, the keyword can be any unique value, as set by the caller of
		 * the validator. There is, however, a standard format for automatic processing.
		 * See the @see
		 * net.ionite.docval.KeywordDeriver#deriveKeyword(java.io.InputStream)
		 * deriveKeyword for more information
		 */
		public String keyword;
		/**
		 * The list of validation files to validate a document for this DocumentType
		 * against
		 */
		public ArrayList<String> validationFiles;

		/**
		 * Constructor for DocumentType options
		 */
		public DocumentType() {
			validationFiles = new ArrayList<String>();
		}

		/**
		 * Serialize this document type configuration as a JSON object
		 * 
		 * @return JSONObject containing this document type configuration
		 */
		@SuppressWarnings("unchecked") // JSONObject extends HashMap but is not generic itself
		public JSONObject toJSON(boolean addValidationFiles) {
			JSONObject result = new JSONObject();
			result.put("name", name);
			result.put("description", description);
			result.put("keyword", keyword);
			if (addValidationFiles) {
				JSONArray jsonValidationFiles = new JSONArray();
				for (String validationFile : validationFiles) {
					jsonValidationFiles.add(validationFile);
				}
				result.put("validation_files", jsonValidationFiles);
			}
			return result;
		}

		/**
		 * Serialize this document type configuration as a JSON string
		 * 
		 * @return String containing a JSON representation of this document type
		 *         configuration
		 */
		public String toJSONString(boolean addValidationFiles) {
			return toJSON(addValidationFiles).toString();
		}

		/**
		 * Serialize this document type configuration as an XML Sapling
		 * 
		 * @return SaplingElement containing an XML representation of this document type
		 *         configuration
		 */
		public SaplingElement toXMLSaplingElement() {
			SaplingElement element = Saplings.elem("DocumentType").withChild(Saplings.elem("Name").withText(name));
			if (description != null && !"".equals(description)) {
				element = element.withChild(Saplings.elem("Description").withText(description));
			}
			element = element.withChild(Saplings.elem("Keyword").withText(keyword));

			return element;
		}
	}

	/**
	 * Constructor for an empty ConfigData instance
	 */
	public ConfigData() {
		documentTypes = new ArrayList<DocumentType>();
	}

	/**
	 * Serialize the document types configured in this ConfigData as a JSON object
	 * 
	 * @return JSONObject containing the document types configured in this
	 *         ConfigData instance
	 */
	@SuppressWarnings("unchecked") // JSONObject extends HashMap but is not generic itself
	public JSONObject documentTypesAsJSON() {
		JSONObject result = new JSONObject();
		JSONArray jsonDocumentTypes = new JSONArray();
		for (DocumentType docType : documentTypes) {
			jsonDocumentTypes.add(docType.toJSON(false));
		}
		result.put("document_types", jsonDocumentTypes);
		return result;
	}

	/**
	 * Serialize the document types configured in this ConfigData as a JSON string
	 * 
	 * @return String containing a JSON representation of the document types
	 *         configured in this ConfigData instance
	 */
	public String documentTypesAsJSONString() {
		return documentTypesAsJSON().toString();
	}

	/**
	 * Serialize the document types configured in this ConfigData instance as an XML
	 * string
	 * 
	 * @return String containing an XML representation of the document types
	 *         configured in this ConfigData instance
	 * @throws SaxonApiException if there is an error constructing the XML data
	 */
	public String documentTypesAsXMLString() throws SaxonApiException {
		SaplingElement root = Saplings.elem("DocumentTypes");

		for (DocumentType docType : documentTypes) {
			root = root.withChild(docType.toXMLSaplingElement());
		}

		SaplingDocument doc = Saplings.doc().withChild(root);
		Processor processor = new Processor(false); // False = does not required a feature from a licensed version of
													// Saxon.
		Serializer serializer = processor.newSerializer();
		// Other properties found here:
		// http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/Serializer.Property.html
		serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");
		serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
		XdmNode xdmNode;
		xdmNode = doc.toXdmNode(processor);
		String result = serializer.serializeNodeToString(xdmNode);
		return result;
	}
}
