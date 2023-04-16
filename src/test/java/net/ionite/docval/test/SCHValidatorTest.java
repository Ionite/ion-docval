package net.ionite.docval.test;

import net.ionite.docval.validation.ValidationResult;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.Assert;
import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.validation.validator.SCHValidator;
import net.ionite.docval.validation.ValidatorException;

public class SCHValidatorTest {
	public SCHValidatorTest() {
		super();
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");
	}

	@Test
	public void loadSCHTest() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			new SCHValidator(fileName);
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadBadSCH() {
		try {
			String fileName = ClassLoader.getSystemResource("xsd/shiporder_bad.xsd").getFile();
			new SCHValidator(fileName);
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valErr) {
			// good.
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void loadNonexistentSCH() {
		try {
			new SCHValidator("/does/not/exist");
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
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/SI-UBL-2.0_ok_minimal.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile1() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_bad_sch1.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			exc.printStackTrace();
			// Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testBadFile2() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_bad_sch2.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testWarningFile1() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_warning_sch1.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(1, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testWarningFile2() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_warning_sch2.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(0, result.errorCount());
			assertEquals(2, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile1() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe1.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile2() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe2.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

	@Test
	public void testXXEFile3() {
		try {
			String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
			SCHValidator schValidator = new SCHValidator(fileName);

			String xmlFileName = ClassLoader.getSystemResource("xml/shiporder_xxe3.xml").getFile();
			ValidationResult result = schValidator.validate(Files.readAllBytes(Paths.get(xmlFileName)));

			assertEquals(1, result.errorCount());
			assertEquals(0, result.warningCount());
		} catch (Exception exc) {
			Assert.fail("Unexpected exception: " + exc.toString());
		}
	}

}
