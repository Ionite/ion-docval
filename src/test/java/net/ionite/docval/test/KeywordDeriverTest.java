package net.ionite.docval.test;

import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.ionite.docval.xml.KeywordDeriver;

public class KeywordDeriverTest {
	private void testDocument(String filename, String expectedKeyword) {
		try {
			String xmlFileName = ClassLoader.getSystemResource(filename).getFile();
			byte[] fileBytes = Files.readAllBytes(Paths.get(xmlFileName));
			String keyword = new KeywordDeriver().deriveKeyword(fileBytes);
			Assert.assertEquals(expectedKeyword, keyword);
		} catch (IOException ioe) {
			Assert.fail("Error reading " + filename + ": " + ioe.toString());
		}
	}

	@Test
	public void testKeywordDeriverNoNamespace() {
		testDocument("xml/shiporder_good.xml", "shiporder");
	}

	@Test
	public void testKeywordDeriverNLCIUSUBLInvoice() {
		testDocument("xml/SI-UBL-2.0_ok_minimal.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:nen.nl:nlcius:v1.0::2.1");
	}

	@Test
	public void testKeywordDeriverNLCIUSUBLCreditNote() {
		testDocument("xml/SI-UBL-2.0_CreditNote.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:cen.eu:en16931:2017#compliant#urn:fdc:nen.nl:nlcius:v1.0::2.1");
	}

	@Test
	public void testKeywordDeriverNLCIUSCII() {
		testDocument("xml/NLCIUS-CII_ok_example.xml",
				"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100::CrossIndustryInvoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:nen.nl:nlcius:v1.0::D16B");
	}

	@Test
	public void testKeywordDeriverPB3UBLInvoice() {
		testDocument("xml/PB3_ok_minimal.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
	}

	@Test
	public void testKeywordDeriverPB3UBLOrder() {
		testDocument("xml/PB3-Order_ok_minimal.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:fdc:peppol.eu:poacc:trns:order:3::2.1");
	}

	@Test
	public void testKeywordDeriverPB3CII() {
		testDocument("xml/PB3_CII_ok_base.xml",
				"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100::CrossIndustryInvoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::D16B");
	}

}
