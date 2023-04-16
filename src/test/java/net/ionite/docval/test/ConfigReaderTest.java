package net.ionite.docval.test;

import net.ionite.docval.config.*;

import org.junit.Assert;
import org.junit.Test;

public class ConfigReaderTest {
	private String getDataFile(String fileName) {
		return ClassLoader.getSystemResource(fileName).getFile();
	}

	@Test
	public void loadGoodConfig() {
		try {
			String configFile = getDataFile("config/test_config_good_1.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			ConfigData configData = configReader.readConfig();
			Assert.assertEquals(true, configData.autoReload);
			Assert.assertEquals(ConfigData.UnknownKeywords.WARN, configData.unknownKeywords);
			Assert.assertNotNull(configData.server);
			Assert.assertNotNull(configData.server.listen);
			Assert.assertEquals(1, configData.server.listen.size());
			Assert.assertEquals("127.0.0.1", configData.server.listen.get(0).address);
			Assert.assertEquals(35791, configData.server.listen.get(0).port);
		} catch (ConfigurationError cfgError) {
			Assert.fail("Should not have raised: " + cfgError);
		}
	}

	@Test
	public void loadBadConfigFile1() {
		try {
			String configFile = getDataFile("config/test_config_bad_1.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			configReader.readConfig();
			Assert.fail("Should have raised ConfigurationError.");
		} catch (ConfigurationError cfgError) {
			// System.err.println(cfgError);
		}
	}

	@Test
	public void loadBadConfigFile2() {
		try {
			String configFile = getDataFile("config/test_config_bad_2.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			configReader.readConfig();
			Assert.fail("Should have raised ConfigurationError.");
		} catch (ConfigurationError cfgError) {
			// System.err.println(cfgError);
		}
	}

	@Test
	public void loadBadConfigFile3() {
		try {
			String configFile = getDataFile("config/test_config_bad_3.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			configReader.readConfig();
			Assert.fail("Should have raised ConfigurationError.");
		} catch (ConfigurationError cfgError) {
			// System.err.println(cfgError);
		}
	}

	@Test
	public void loadBadConfigFile4() {
		try {
			String configFile = getDataFile("config/test_config_bad_4.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			configReader.readConfig();
			Assert.fail("Should have raised ConfigurationError.");
		} catch (ConfigurationError cfgError) {
			// System.err.println(cfgError);
		}
	}

	@Test
	public void loadBadConfigFile5() {
		try {
			String configFile = getDataFile("config/test_config_bad_5.xml");
			ConfigReader configReader = new ConfigReader(configFile);
			configReader.readConfig();
			Assert.fail("Should have raised ConfigurationError.");
		} catch (ConfigurationError cfgError) {
			// System.err.println(cfgError);
		}
	}

}
