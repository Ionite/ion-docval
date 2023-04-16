package net.ionite.docval.test;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.validator.XSLTValidator;

public class XSLTValidatorTest {

	public XSLTValidatorTest() {
		super();
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
	}

	public void setUp() {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
	}

	@Test
	public void loadXSLTTest() {
		try {
			String fileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			new XSLTValidator(fileName);
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadBadXSLT() {
		try {
			String fileName = ClassLoader.getSystemResource("xsd/shiporder_bad.xsd").getFile();
			new XSLTValidator(fileName);
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valErr) {
			// good.
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadNonexistentXSLT() {
		try {
			new XSLTValidator("/does/not/exist");
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
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_ok_minimal.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testWrongFile() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_good.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warning_count\": 0, \"error_count\": 1, \"warnings\":[],\"errors\":[{\"test\":\"ubl:Invoice or cn:CreditNote\",\"location\":\"\\/shiporder[1]\",\"message\":\"[SI-INV-R000]-This is not an SI-UBL 2.0 Invoice or CreditNote, validation cannot continue\"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile1() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_error_no_customizationid.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			// assertEquals(2, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":1,\"errors\":[{\"test\":\"cbc:CustomizationID\",\"location\":\"\\/ubl:Invoice[1]\",\"message\":\"[BII2-T10-R001] An invoice MUST have a customization identifier\"}]}");
			// JSONObject expected = (JSONObject)
			// parser.parse("{\"warnings\":[],\"warning_count\":0,\"error_count\":2,\"errors\":[{\"test\":\"cbc:CustomizationID\",\"location\":\"\\/*:Invoice[namespace-uri()='urn:oasis:names:specification:ubl:schema:xsd:Invoice-2'][1]\",\"message\":\"[BII2-T10-R001]
			// An invoice MUST have a customization
			// identifier\"},{\"test\":\"(cbc:CustomizationID) !=
			// ''\",\"location\":\"\\/*:Invoice[namespace-uri()='urn:oasis:names:specification:ubl:schema:xsd:Invoice-2'][1]\",\"message\":\"[BR-01]-An
			// Invoice shall have a Specification identifier (BT-24).   \"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile2() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_BR-NL-5_error_no_streetname.xml")
					.getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":1,\"errors\":[{\"test\":\"(cac:Country\\/cbc:IdentificationCode != 'NL') or (cbc:StreetName and cbc:CityName and cbc:PostalZone)\",\"location\":\"\\/ubl:Invoice[1]\\/cac:TaxRepresentativeParty[1]\\/cac:PostalAddress[1]\",\"message\":\"[BR-NL-5] For suppliers in the Netherlands, if the fiscal representative is in the Netherlands, the representative's address (cac:TaxRepresentativeParty\\/cac:PostalAddress) MUST contain street name (cbc:StreetName), city (cbc:CityName) and postal zone (cbc:PostalZone)\"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile3() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/decimal_error.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(2, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":2,\"errors\":[{\"test\":\"starts-with(normalize-space(.), 'urn:cen.eu:en16931:2017#compliant#urn:fdc:nen.nl:nlcius:v1.0')\",\"location\":\"\\/ubl:Invoice[1]\\/cbc:CustomizationID[1]\",\"message\":\"[SI-V20-INV-R000]-This XML instance is NOT tagged as an SI-UBL 2.0 invoice or credit note; please check the CustomizationID value\"},{\"test\":null,\"location\":\"Schematron validation\",\"message\":\"Error during schematron validation: Cannot convert string \\\"1,018.00\\\" to xs:decimal: invalid character ','\"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile4() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/cen-ubl.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/PB3_error_double_name.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[],\"warning_count\":0,\"error_count\":1,\"errors\":[{\"test\":null,\"location\":\"XPath error\",\"message\":\"A sequence of more than one item is not allowed as the first argument of fn:normalize-space() (\\\"Customer name as registered in KvK\\\", \\\"Customer name as registered in KvK\\\") \"}]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testWarningFile1() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_BR-NL-26_warning_legalform.xml")
					.getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(1, result.warningCount());
			JSONParser parser = new JSONParser();
			JSONObject expected = (JSONObject) parser.parse(
					"{\"warnings\":[{\"test\":\"false\",\"location\":\"\\/ubl:Invoice[1]\\/cac:AccountingSupplierParty[1]\\/cac:Party[1]\\/cac:PartyLegalEntity[1]\\/cbc:CompanyLegalForm[1]\",\"message\":\"[BR-NL-26] The use of the seller additional legal information field (cac:AccountingSupplierParty\\/cac:Party\\/cac:PartyLegalEntity\\/cbc:CompanyLegalForm) is not recommended, since this is not applicable for suppliers in the Netherlands\"}],\"warning_count\":1,\"error_count\":0,\"errors\":[]}");
			assertEquals(expected.toString(), result.toJSON().toString());
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());

		}
	}

	@Test
	public void testXXEFile1() {
		try {
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_xxe1.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			// assertEquals("", result.toJSONString());
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
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_xxe2.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			// assertEquals("", result.toJSONString());
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
			String xsltFileName = ClassLoader.getSystemResource("xslt/si-ubl-2.0.xsl").getFile();
			XSLTValidator xsltValidator = new XSLTValidator(xsltFileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_xxe3.xml").getFile();
			ValidationResult result = xsltValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			// assertEquals("", result.toJSONString());
			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());

		}
	}
}
