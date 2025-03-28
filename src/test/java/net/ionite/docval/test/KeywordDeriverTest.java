package net.ionite.docval.test;

import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.ionite.docval.validation.ValidatorException;
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

	@Test
	public void testKeywordDeriverEUSR110() {
		testDocument("xml/eusr-1.1.0.xml",
				"urn:fdc:peppol:end-user-statistics-report:1.1::EndUserStatisticsReport##urn:fdc:peppol.eu:edec:trns:end-user-statistics-report:1.1::1.1");
	}

	@Test
	public void testKeywordDeriverTSR100() {
		testDocument("xml/tsr-1.0.0.xml",
				"urn:fdc:peppol:transaction-statistics-report:1.0::TransactionStatisticsReport##urn:fdc:peppol.eu:edec:trns:transaction-statistics-reporting:1.0::1.0");
	}

    @Test
    public void testKeywordDeriverLogisticsAdvancedDespatchAdvice_Example_Full() {
		testDocument("xml/AdvancedDespatchAdvice_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:DespatchAdvice-2::DespatchAdvice##urn:fdc:peppol.eu:logistics:trns:advanced_despatch_advice:1::2.1");
    }

    @Test
    public void testKeywordDeriverLogisticsReceiptAdvice_Example_Full() {
		testDocument("xml/ReceiptAdvice_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:ReceiptAdvice-2::ReceiptAdvice##urn:fdc:peppol.eu:logistics:trns:receipt_advice:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsTransportExecutionPlanRequest_Example_Full() {
		testDocument("xml/TransportExecutionPlanRequest_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:TransportExecutionPlanRequest-2::TransportExecutionPlanRequest##urn:fdc:peppol.eu:logistics:trns:transport_execution_plan_request:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsTransportExecutionPlan_Example_Full() {
		testDocument("xml/TransportExecutionPlan_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:TransportExecutionPlan-2::TransportExecutionPlan##urn:fdc:peppol.eu:logistics:trns:transport_execution_plan:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsTransportationStatusRequest_Example_Full() {
		testDocument("xml/TransportationStatusRequest_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:TransportationStatusRequest-2::TransportationStatusRequest##urn:fdc:peppol.eu:logistics:trns:transportation_status_request:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsTransportationStatus_Example_Full() {
		testDocument("xml/TransportationStatus_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:TransportationStatus-2::TransportationStatus##urn:fdc:peppol.eu:logistics:trns:transportation_status:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsWaybill_Example_Full() {
		testDocument("xml/Waybill_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:Waybill-2::Waybill##urn:fdc:peppol.eu:logistics:trns:waybill:1::2.3");
    }

    @Test
    public void testKeywordDeriverLogisticsWeightStatement_Example_Full() {
		testDocument("xml/WeightStatement_Example_Full.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:WeightStatement-2::WeightStatement##urn:fdc:peppol.eu:logistics:trns:weight_statement:1::2.3");
    }

    @Test
    public void testKeywordDeriverPB3AdvancedOrder() {
		testDocument("xml/PB3_Advanced_Order_Example.xml",
				"urn:oasis:names:specification:ubl:schema:xsd:OrderResponse-2::OrderResponse##urn:fdc:peppol.eu:poacc:trns:order_response_advanced:3::2.3");
    }

	@Test
	public void testKeywordDeriverXXE1() {
		try {
			testDocument("xml/shiporder_xxe1.xml", "N/A");
			Assert.assertTrue("Expected exception to be thrown", false);
		} catch (ValidatorException ve) {
			// ok
		}
	}

	@Test
	public void testKeywordDeriverXXE2() {
		try {
			testDocument("xml/shiporder_xxe2.xml", "N/A");
			Assert.assertTrue("Expected exception to be thrown", false);
		} catch (ValidatorException ve) {
			// ok
		}
	}

	@Test
	public void testKeywordDeriverXXE3() {
		try {
			testDocument("xml/shiporder_xxe3.xml", "N/A");
			Assert.assertTrue("Expected exception to be thrown", false);
		} catch (ValidatorException ve) {
			// ok
		}
	}

}
