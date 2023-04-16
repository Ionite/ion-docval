package net.ionite.docval.validation.validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.xml.IgnoreErrorHandler;

/**
 * The standard validator for SCH (Schematron) files
 * 
 * This is an extension of the XSLTValidator, where the provided sch file is
 * converted to the .xslt file in-place.
 * 
 * Be aware that startup times are much faster if you use the .xsl directly, as
 * this class performs a (slow) conversion using the default Schematron skeleton
 * files.
 */
public class SCHValidator extends XSLTValidator {
	/**
	 * Internal class to resolve URIs used in include/import statements Overrides
	 * one specific file: iso_schematron_skeleton_for_saxon.xsl to load from the jar
	 * bundle. Other files are loaded from the file system.
	 * 
	 * @author Ionite
	 *
	 */
	private static class BasicURIResolver implements URIResolver {
		private String originalPath;

		public BasicURIResolver(String baseFileName) {
			originalPath = new File(baseFileName).getParent() + "/";
		}

		/**
		 * @see URIResolver#resolve(String, String)
		 */
		public Source resolve(String href, String base) throws TransformerException {
			if (href.equals("iso_schematron_skeleton_for_saxon.xsl")) {
				String url = "xsl/" + href;
				ClassLoader classLoader = getClass().getClassLoader();
				InputStream is = classLoader.getResourceAsStream(url);
				return new StreamSource(is);
			} else {
				if (href.startsWith("/")) {
					return new StreamSource(new File(href));
				} else {
					return new StreamSource(new File(originalPath + href));
				}
			}
		}
	}

	/**
	 * Constructor for a Schematron validator from a .sch file
	 * 
	 * @param filename The schematron (.sch) file to load.
	 */
	public SCHValidator(String filename) {
		super(filename);
	}

	/**
	 * Constructor for a Schematron validator from an input stream
	 * 
	 * @param stream InputStream with the contents of an .sch file
	 */
	public SCHValidator(InputStream stream) {
		super(stream);
	}

	/**
	 * Override the initialization in the parent class This implementation loads the
	 * given schematron, and converts it to an SVRL stylesheet, using the default
	 * skeleton implementation as published on the schematron website.
	 */
	@Override
	protected Transformer setupTransformer() {
		try {
			logger.debug("Starting Schematron to SVRL Stylesheet conversion");
			// Run the given file (an .sch file) through the conversion pipeline
			SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
			stf.setURIResolver(new BasicURIResolver(_filename));

			ClassLoader classLoader = getClass().getClassLoader();
			logger.debug("Loading dsdl include stylesheet");
			Templates templates1 = stf
					.newTemplates(new StreamSource(classLoader.getResourceAsStream("xsl/iso_dsdl_include.xsl")));
			logger.debug("Loading abstract-expand stylesheet");
			Templates templates2 = stf
					.newTemplates(new StreamSource(classLoader.getResourceAsStream("xsl/iso_abstract_expand.xsl")));
			logger.debug("Loading svrl-for-xslt2 stylesheet");
			Templates templates3 = stf
					.newTemplates(new StreamSource(classLoader.getResourceAsStream("xsl/iso_svrl_for_xslt2.xsl")));

			TransformerHandler th1 = stf.newTransformerHandler(templates1);
			TransformerHandler th2 = stf.newTransformerHandler(templates2);
			TransformerHandler th3 = stf.newTransformerHandler(templates3);

			th1.setResult(new SAXResult(th2));
			th2.setResult(new SAXResult(th3));

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			StreamResult last = new StreamResult(outputStream);
			th3.setResult(last);

			logger.debug("Transforming sch file");
			Transformer t = stf.newTransformer();
			t.setErrorListener(new IgnoreErrorHandler());
			t.transform(new StreamSource(new File(_filename)), new SAXResult(th1));

			logger.debug("Finished Schematron to SVRL Stylesheet conversion");
			return loadTransformer(new ByteArrayInputStream(outputStream.toByteArray()));
		} catch (Exception error) {
			throw new ValidatorException("Error setting up SCH validator for " + _filename, error);
		}
	}
}
