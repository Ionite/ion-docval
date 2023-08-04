package net.ionite.docval.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidationResultItem;
import net.sf.saxon.s9api.SaxonApiException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Main class of the command-line client for the HTTP server validator.
 */
public class DocValHttpClientMain {
	private static void printResultItem(String itemType, ValidationResultItem item, boolean showDetails) {
		System.out.println(itemType + ": " + item.message);

		if (showDetails) {
			System.out.println("    Test: " + item.test);
			if (item.location != null) {
				System.out.println("    Location: " + item.location);
			}
			if (item.line != null) {
				System.out.println("    Location: " + item.line);
			}
			if (item.column != null) {
				System.out.println("    Location: " + item.column);
			}
			System.out.println("");
		}
	}

	/**
	 * Parse the command-line arguments and pass the given document to the server
	 * for validation. Theb parse the server response and print the result.
	 * 
	 * Exit code is the number of errors found by the validation, or a negative
	 * number if the file could not be read or the server could not be reached.
	 */
	public static void main(String[] argv) {
		Namespace args;
		ArgumentParser parser;

		parser = ArgumentParsers.newFor("ion-docval-cli").addHelp(true).build()
				.description("Validate a document given any number of XSD or Schematron XSLT files");
		parser.addArgument("-u", "--uri")
				.help("Override the default or configured server URI (defaults to http://localhost:35791/api/validate)")
				.setDefault("http://localhost:35791/api/validate");
		parser.addArgument("-o", "--output-format").setDefault("text")
				.help("Set the output format, one of: text (default), details, xml, json, none");
		parser.addArgument("-v", "--verbose").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print verbose debug output");
		parser.addArgument("-V", "--version").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print the software version and exit");

		parser.addArgument("document-file").nargs("?").help("XML Document to validate");

		try {
			args = parser.parseArgs(argv);

			if ((Boolean) args.get("version")) {
				System.out.println(DocValHttpClient.class.getPackage().getImplementationVersion());
				System.exit(0);
			}
			if (args.get("document_file") == null) {
				System.err.println("Error: missing mandatory document-file argument");
				System.exit(-1);
			}

			if ((Boolean) args.get("verbose")) {
				System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
			} else {
				System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");
			}
			DocValHttpClient client = new DocValHttpClient(args.getString("uri"));

			// System.out.println("[XX] READING: " +
			// Paths.get(args.getString("document_file")));
			byte[] fileData = Files.readAllBytes(Paths.get(args.getString("document_file")));

			ValidationResult result = client.validate(fileData);

			switch (args.getString("output_format")) {
			case "xml":
				try {
					System.out.println(result.toXMLString());
				} catch (SaxonApiException saxError) {
					saxError.printStackTrace();
					System.out.println("<Error><Detail>Unable to convert validation results to XML</Detail></Error>");
				}

				break;
			case "json":
				System.out.println(result.toJSONString());
				break;
			case "text":
			case "details":
				System.out.println("Errors: " + result.errorCount());
				System.out.println("Warnings: " + result.warningCount());
				System.out.println("");
				if (result.errorCount() > 0 || result.warningCount() > 0) {
					for (ValidationResultItem item : result.getErrors()) {
						printResultItem("Error", item, args.getString("output_format").equals("details"));
					}
					for (ValidationResultItem item : result.getWarnings()) {
						printResultItem("Warning", item, args.getString("output_format").equals("details"));
					}
					System.out.println("");
				}
				break;
			case "none":
				break;
			default:
				System.err.println("Unknown output format: " + args.getString("output_format"));
				System.exit(-1);
			}
			System.exit(result.errorCount());
		} catch (ArgumentParserException ape) {
			System.err.println("Error parsing arguments: " + ape.toString());
			System.exit(-1);
		} catch (IOException ioe) {
			System.err.println("Error connecting to server: " + ioe.toString());
			System.exit(-2);
		} catch (DocValClientException dve) {
			System.err.println("Error validating file: " + dve.toString());
			System.exit(-3);
		}
	}
}
