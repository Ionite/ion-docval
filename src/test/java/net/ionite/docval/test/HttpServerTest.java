package net.ionite.docval.test;

import net.ionite.docval.config.ConfigData.UnknownKeywords;
import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.server.DocValHttpClient;
import net.ionite.docval.server.DocValClientException;
import net.ionite.docval.server.DocValHttpServer;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.ValidatorManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.simple.SimpleLogger;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.IOException;

public class HttpServerTest {
	static int DEFAULT_TEST_PORT = 35793;
	private ValidatorManager validatorManager;
	private DocValHttpServer server = null;
	private DocValHttpClient client;

	private byte[] loadTestFile(String filename) throws IOException {
		String dataFileName = ClassLoader.getSystemResource(filename).getFile();
		return Files.readAllBytes(Paths.get(dataFileName));
	}

	@Before
	public void setUp() {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");

		validatorManager = new ValidatorManager();
		server = new DocValHttpServer(validatorManager);

		client = new DocValHttpClient("http://localhost:" + DEFAULT_TEST_PORT + "/api/validate");
	}

	@After
	public void tearDown() {
		if (server != null) {
			server.halt(0);
		}
	}

	@Test
	public void testUnknownDocumentTypeIgnore() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.IGNORE);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"));

		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testUnknownDocumentTypeWarn() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.WARN);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"));

		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(1, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testUnknownDocumentTypeError() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"));

		Assert.assertEquals(1, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testUnknownDocumentTypeFail() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.FAIL);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		try {
			client.validate(loadTestFile("xml/shiporder_good.xml"));
			Assert.fail("Should have thrown ValidatorException");
		} catch (ValidatorException valError) {
			// expected!
		}

		server.halt(0);
	}

	@Test
	public void testExplicitUnknownKeyword() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);

		String fileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
		validatorManager.addValidator("test1", fileName, false);
		// validatorManager.addValidator("asdf", , false);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"), "unknown");
		Assert.assertEquals(1, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testAutomaticKeyword() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);

		String fileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
		validatorManager.addValidator("shiporder", fileName, false);
		// validatorManager.addValidator("asdf", , false);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"));
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testExplicitKeyword() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);

		String fileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
		validatorManager.addValidator("test1", fileName, false);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"), "test1");
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testExplicitKeyword2() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);

		String fileName = ClassLoader.getSystemResource("xsd/shiporder_good.xsd").getFile();
		validatorManager.addValidator("test!@#$^:;',./<>", fileName, false);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_good.xml"), "test!@#$^:;',./<>");
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(0, result.warningCount());
		server.halt(0);
	}

	@Test
	public void testExplicitKeyword3() throws DocValClientException, IOException, InterruptedException {
		validatorManager.setUnknownKeywords(UnknownKeywords.ERROR);

		String fileName = ClassLoader.getSystemResource("sch/shiporder_good.sch").getFile();
		validatorManager.addValidator("test1", fileName, false);
		server.addListener("127.0.0.1", DEFAULT_TEST_PORT);
		server.start();
		ValidationResult result = client.validate(loadTestFile("xml/shiporder_warning_sch1.xml"), "test1");
		Assert.assertEquals(0, result.errorCount());
		Assert.assertEquals(1, result.warningCount());
		server.halt(0);
	}

}
