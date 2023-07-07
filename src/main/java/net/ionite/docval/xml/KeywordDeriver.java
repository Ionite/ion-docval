
package net.ionite.docval.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.ionite.docval.validation.ValidatorException;

/**
 * Derives a keyword from a given XML file.
 * <p>
 * This can be used instead of manually setting and using your own keywords for
 * each document type. The derived value matches the way document ids are used
 * in the Peppol SBDH, i.e. using the document identifier scheme
 * <i>busdox-docid-qns</i> with a subtype based on the customization id and UBL
 * version.
 * </p>
 * <p>
 * The derivation works as follows:
 * <ul>
 * <li>If the document is UBL(2) or CII (D16B), the format is:
 * 
 * <pre>
 * &lt;namespace&gt;::&lt;root element&gt;##&lt;document type&gt;::&lt;document type version&gt;
 * </pre>
 * 
 * </li>
 * <li>For any other XML document with a namespace, the format is:
 * 
 * <pre>
 * &lt;namespace&gt;::&lt;root element&gt;
 * </pre>
 * 
 * </li>
 * <li>For any other XML document without a namespace, it is simply the root
 * element name:
 * 
 * <pre>
 * &lt;root element&gt;
 * </pre>
 * 
 * </li>
 * </ul>
 * The individual values are as follows:
 * <ul>
 * <li>namespace: The XML namespace of the XML document</li>
 * <li>root element: The tag name of the root element of the XML document</li>
 * <li>document type: either:
 * <ul>
 * <li>The value of the CustomizationID element, if the document is UBL</li>
 * <li>The value of &lt;ReusableAggregateBusinessInformationEntity/ID&gt;, if
 * the document is CII</li>
 * </ul>
 * </li>
 * <li>version, either:
 * <ul>
 * <li>The value of the UBLVersionID Element, if the document is UBL</li>
 * <li>"2.1" if the document is UBL (2), and no UBLVersionID element is
 * present</li>
 * <li>"D16B" if the document is CII</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * For example, a Peppol BIS 3 UBL Invoice document would have the following
 * derived keyword:
 * 
 * <pre>
 * urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1
 * </pre>
 * 
 * But a plain XML document, without a namespace, such as:
 * {@literal <FooBar><Baz /></FooBar>} would have the keyword:
 * 
 * <pre>
 * FooBar
 * </pre>
 * <p>
 * You can use the <b>ion-docval-cli</b> tool to generate a derived keyword for
 * any given XML file (API: {@link net.ionite.docval.commandline.CommandLineValidator}).
 * </p>
 * 
 * @author Ionite
 *
 */
public class KeywordDeriver {
	private class DeriverXMLHandler extends DefaultHandler {
		private StringBuilder _currentValue = new StringBuilder();

		private String rootElement = null;
		private String namespace = null;
		private String customization = null;
		private String version = null;
		private boolean inCIIDocumentParameter = false;

		@Override
		public void characters(char ch[], int start, int length) {
			_currentValue.append(ch, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			_currentValue.setLength(0);
			if (rootElement == null) {
				rootElement = localName;
				namespace = uri;
			}
			if ("GuidelineSpecifiedDocumentContextParameter".equals(localName)) {
				inCIIDocumentParameter = true;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			if (customization == null && localName.equals("CustomizationID")) {
				customization = _currentValue.toString();
			} else if (customization == null && inCIIDocumentParameter && localName.equals("ID")) {
				customization = _currentValue.toString();
			} else if ("GuidelineSpecifiedDocumentContextParameter".equals(localName)) {
				inCIIDocumentParameter = false;
			}
		}

		public String getKeyword() {
			if (namespace == null || "".equals(namespace)) {
				return rootElement;
			}
			if (customization == null) {
				return namespace + "::" + rootElement;
			}
			if (version == null) {
				if (namespace.startsWith("urn:oasis:names:specification:ubl")) {
					version = "2.1";
				} else if (namespace.startsWith("urn:un:unece:uncefact:data:standard:CrossIndustryInvoice")) {
					version = "D16B";
				}
			}

			if (customization == null) {
				return namespace + "::" + rootElement;
			} else if (version == null) {
				return namespace + "::" + rootElement + "##" + customization;
			} else {
				return namespace + "::" + rootElement + "##" + customization + "::" + version;
			}
		}
	}

	/**
	 * Derive the keyword from the given InputStream
	 * 
	 * @param source The XML source to derive the keyword from
	 * @return a String with the keyword.
	 */
	public String deriveKeyword(byte[] source) {
		return deriveKeyword(new BufferedInputStream(new ByteArrayInputStream(source)));
	}

	/**
	 * Derive the keyword from the given InputStream
	 * 
	 * @param source The XML source to derive the keyword from
	 * @return a String with the keyword.
	 */
	public String deriveKeyword(InputStream source) {
		try {
			SAXParserFactory sfactory = SAXParserFactory.newInstance();
			sfactory.setNamespaceAware(true);
			SAXParser parser = sfactory.newSAXParser();
			DeriverXMLHandler handler = new DeriverXMLHandler();
			parser.parse(source, handler);

			String keyword = handler.getKeyword();
			Logger logger = LoggerFactory.getLogger(this.getClass().getName());
			logger.debug("Derived keyword for document: " + keyword);
			return keyword;
		} catch (IOException tfError) {
			throw new ValidatorException("Error Deriving document keyword: " + tfError.getMessage(), tfError);
		} catch (SAXException saxError) {
			throw new ValidatorException("Error Deriving document keyword: " + saxError.getMessage(), saxError);
		} catch (ParserConfigurationException pConfError) {

			throw new ValidatorException("Error Deriving document keyword: " + pConfError.getMessage(), pConfError);
		}
	}

}
