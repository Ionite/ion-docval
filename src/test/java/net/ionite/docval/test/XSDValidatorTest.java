package net.ionite.docval.test;

import net.ionite.docval.validation.ValidationResult;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Assert;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.validator.XSDValidator;

public class XSDValidatorTest {
	public XSDValidatorTest() {
		super();
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");
	}

	@Test
	public void loadXSDTest() {
		try {
			String fileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			new XSDValidator(fileName);
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadBadXSD() {
		try {
			String fileName = ClassLoader.getSystemResource("xsd/shiporder_bad.xsd").getFile();
			new XSDValidator(fileName);
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valErr) {
			// good.
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadNonXSD() {
		try {
			String fileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			new XSDValidator(fileName);
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valErr) {
			// good.
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadNonexistentXSD() {
		try {
			new XSDValidator("/does/not/exist");
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valErr) {
			// good.
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testGoodFile() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			// FileInputStream fin = new
			// FileInputStream(ClassLoader.getSystemResource("xml/shiporder_good.xml").toURI().openStream());
			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_good.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile1() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			// FileInputStream fin = new
			// FileInputStream(ClassLoader.getSystemResource("xml/shiporder_good.xml").toURI().openStream());
			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe1.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			// External entity resolution results in a single 'xsd' error
			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile2() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			// FileInputStream fin = new
			// FileInputStream(ClassLoader.getSystemResource("xml/shiporder_good.xml").toURI().openStream());
			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe2.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile3() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			// FileInputStream fin = new
			// FileInputStream(ClassLoader.getSystemResource("xml/shiporder_good.xml").toURI().openStream());
			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe3.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile1() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_bad1.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":1,\"errors\":[{\"test\":\"XML Schema\",\"line\":14,\"column\":30,\"message\":\"cvc-minInclusive-valid: Value '-100' is not facet-valid with respect to minInclusive '1' for type 'positiveInteger'.\"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile2() {
		try {
			String xsdFileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
			XSDValidator xsdValidator = new XSDValidator(xsdFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_bad2.xml").getFile();
			ValidationResult result = xsdValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":1,\"errors\":[{\"test\":\"XML Schema\",\"line\":3,\"column\":135,\"message\":\"cvc-elt.1.a: Cannot find the declaration of element 'shiporderBADTAG'.\"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

}
