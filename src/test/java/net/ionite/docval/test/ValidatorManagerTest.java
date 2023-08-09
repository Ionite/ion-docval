package net.ionite.docval.test;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.ValidatorManager;
import net.ionite.docval.validation.validator.DocumentValidator;
import net.ionite.docval.config.ConfigData;
import net.ionite.docval.config.ConfigurationError;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;

public class ValidatorManagerTest {
	ValidatorManager validatorManager;

	private String getDataFile(String fileName) {
		return ClassLoader.getSystemResource(fileName).getFile();
	}

	private Path getDataPath(String fileName) {
		return new File(ClassLoader.getSystemResource(fileName).getFile()).toPath();
	}

	@Before
	public void setUp() {
		validatorManager = new ValidatorManager();
	}

	@Test
	public void basicLoadTest() {
		DocumentValidator validator;
		try {
			validator = validatorManager.getValidator(getDataFile("xsd/shiporder_good.xsd"));
			Assert.assertNotNull(validator);
			validator = validatorManager.getValidator(getDataFile("xslt/si-ubl-2.0.xsl"));
			Assert.assertNotNull(validator);
			try {
				validator = validatorManager.getValidator("/does/not/exist");
				Assert.fail("Bad filename should have resulted in an exception");
			} catch (ValidatorException valErr1) {
				// good, ignore
			}
			try {
				validator = validatorManager.getValidator("/does/not/exist.xsd");
				Assert.fail("Nonexistent filename should have resulted in an exception");
			} catch (ValidatorException valErr1) {
				// good, ignore
			}
		} catch (Exception exc) {
			exc.printStackTrace();
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void autoReloadTest() throws IOException {
		Path temp = Files.createTempFile("ivdtest", ".xsd");

		// Create a DocumentValidator with this xsd specification
		FileCopy.copy(getDataPath("xsd/shiporder_good.xsd"), temp);
		DocumentValidator validator = validatorManager.getValidator(temp.toString());
		DocumentValidator originalValidator = validator;

		String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_good.xml").getFile();
		ValidationResult result = validator.validate(Files.readAllBytes(Paths.get(xmlFileName)));
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());

		// Without autoreload, replacing the file should not have any effect
		FileCopy.copy(getDataPath("xsd/shiporder_negative.xsd"), temp);
		validator = validatorManager.getValidator(temp.toString());
		result = validator.validate(Files.readAllBytes(Paths.get(xmlFileName)));
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		Assert.assertSame(originalValidator, validator);

		// With autoreload, replacing the file should make it reload automatically.
		validatorManager.setAutoReload(true);

		// Let's overwrite it with a different specification and check that it no longer
		// validates our file
		FileCopy.copy(getDataPath("xsd/shiporder_negative.xsd"), temp);
		validator = validatorManager.getValidator(temp.toString());
		result = validator.validate(Files.readAllBytes(Paths.get(xmlFileName)));
		Assert.assertEquals(1, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		Assert.assertNotSame(originalValidator, validator);

		// Loading a bad one should raise an exception when it is retrieved (due to
		// reloading)
		FileCopy.copy(getDataPath("xslt/si-ubl-2.0.xsl"), temp);
		try {
			validator = validatorManager.getValidator(temp.toString());
			result = validator.validate(Files.readAllBytes(Paths.get(xmlFileName)));
			Assert.fail("The validator should have thrown an exception");
		} catch (ValidatorException valError) {
			// good
		}

		Files.delete(temp);
	}

	@Test
	public void configReloadTest() throws IOException, ConfigurationError {
		// Set up two different configurations
		// This test will check whether reloading a different configuration
		// actually adds new keywords, and removes the old ones

		ConfigData configData1 = new ConfigData();
		ConfigData configData2 = new ConfigData();

		ConfigData.DocumentType documentType1 = configData1.new DocumentType();
		documentType1.name = "Type 1";
		documentType1.keyword = new String("type1");
		documentType1.validationFiles.add(getDataFile("xsd/shiporder_good.xsd"));

		ConfigData.DocumentType documentType2 = configData1.new DocumentType();
		documentType2.name = "Type 2";
		documentType2.keyword = new String("type2");
		documentType2.validationFiles.add(getDataFile("xslt/cen-ubl.xsl"));

		configData1.documentTypes.add(documentType1);
		configData2.documentTypes.add(documentType1);
		configData2.documentTypes.add(documentType2);

		validatorManager.applyConfig(configData1);
		Assert.assertTrue(validatorManager.hasValidatorsForKeyword("type1"));
		Assert.assertFalse(validatorManager.hasValidatorsForKeyword("type2"));

		// Reloading with config 2 should add a validator for type 2 too
		validatorManager.applyConfig(configData2);
		Assert.assertTrue(validatorManager.hasValidatorsForKeyword("type1"));
		Assert.assertTrue(validatorManager.hasValidatorsForKeyword("type2"));

		// And then reloading 1 should remove it again
		validatorManager.applyConfig(configData1);
		Assert.assertTrue(validatorManager.hasValidatorsForKeyword("type1"));
		Assert.assertFalse(validatorManager.hasValidatorsForKeyword("type2"));

	}

	@Test
	public void testValidationLists() {
		// Create two validation lists, with the same validation file.
		// It should return the same instance for both.
		validatorManager.addValidator("Foo", getDataFile("xsd/shiporder_good.xsd"), false);
		validatorManager.addValidator("Bar", getDataFile("xsd/shiporder_good.xsd"), false);
		Assert.assertEquals(1, validatorManager.getValidatorsForKeyword("Foo").size());
		Assert.assertEquals(1, validatorManager.getValidatorsForKeyword("Bar").size());
		DocumentValidator fooVal = validatorManager.getValidatorsForKeyword("Foo").get(0);
		DocumentValidator barVal = validatorManager.getValidatorsForKeyword("Bar").get(0);
		Assert.assertSame(fooVal, barVal);
	}
}
