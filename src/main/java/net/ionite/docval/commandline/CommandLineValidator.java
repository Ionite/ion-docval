package net.ionite.docval.commandline;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.config.ConfigReader;
import net.ionite.docval.config.ConfigurationError;
import net.ionite.docval.validation.ValidationResult;
import net.ionite.docval.validation.ValidationResultItem;
import net.ionite.docval.validation.ValidatorException;
import net.ionite.docval.validation.ValidatorManager;
import net.ionite.docval.xml.KeywordDeriver;
import net.sf.saxon.s9api.SaxonApiException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Stand-alone command-line validation tool.
 * 
 * This is a complete validation tool that can be run from the command-line. It
 * accepts a single document to validate, and multiple XSD, SCH or SVRL (XSLT)
 * files to validate the document against. <br />
 * <br />
 * Prints information about the validation results depending on the given
 * output-format value.<br />
 * <br />
 * Instead of validating a document, the command line tool can also print the
 * value of the derived keyword for the given document file. See
 * {@link net.ionite.docval.xml.KeywordDeriver} for more information about
 * derived keywords.<br />
 * <br />
 * Return code is the number of errors encountered, or a negative value if the
 * validation could not be performed.<br />
 *
 * @author Ionite
 */
public class CommandLineValidator {
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

	private Namespace args;
	private ArgumentParser parser;

	/**
	 * Construct an CommandLineValidator with the given command-line arguments
	 * 
	 * @param argv String-array containing the command-line arguments
	 */
	public CommandLineValidator(String[] argv) {
		parseArguments(argv);
	}

	private void parseArguments(String[] argv) {
		parser = ArgumentParsers.newFor("ion-docval-command-line").addHelp(true).build()
				.description("Validate a document given any number of XSD or Schematron XSLT files");
		parser.addArgument("-c", "--config").help("Use configuration file with schema/schematron definitions");
		parser.addArgument("-o", "--output-format").setDefault("text")
				.help("Output format, one of: short, text, details (default), none, json, xml");
		parser.addArgument("-d", "--derive-keyword").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Do not validate the file, but print the derived keyword");
		parser.addArgument("-v", "--verbose").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print verbose debug output");
		parser.addArgument("-V", "--version").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print the software version and exit");
		parser.addArgument("-k", "--keyword")
				.help("Use the given keyword to select the correct validation when using a config file");

		parser.addArgument("-s", "--schemafile").action(Arguments.append())
				.help("Validation file XML Schema or Schematron XSLT file. Can be specified multiple times.");
		parser.addArgument("document-file").help("XML Document to validate");
		try {
			args = parser.parseArgs(argv);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(-1);
		}
	}

	/**
	 * Execute the validation
	 * 
	 * @return The number of errors found by the validation, or a negative number if
	 *         the validation could not be performed.
	 */
	public int run() {
		if ((Boolean) args.get("version")) {
			System.out.println(this.getClass().getPackage().getImplementationVersion());
			System.exit(0);
		}
		if ((Boolean) args.get("verbose")) {
			System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
		} else {
			System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");
		}

		ValidatorManager validatorManager = new ValidatorManager();
		String documentFile = args.get("document_file");
		FileInputStream documentStream;
		if (documentFile == null) {
			System.err.println("Missing required argument: -f/--document-file");
			parser.printHelp();
			return -1;
		}

		try {
			documentStream = new FileInputStream(documentFile);
		} catch (FileNotFoundException fnfe) {
			System.err.println(fnfe);
			return -1;
		}

		if (args.getBoolean("derive_keyword")) {
			KeywordDeriver kwd = new KeywordDeriver();
			System.out.println(kwd.deriveKeyword(documentStream));
			return 0;
		}

		String keyword = args.get("keyword");
		if (keyword == null) {
			KeywordDeriver kwd = new KeywordDeriver();
			keyword = kwd.deriveKeyword(documentStream);
		}

		String configFile = args.get("config");
		if (configFile != null) {
			ConfigReader configReader = new ConfigReader(configFile);
			try {
				validatorManager.applyConfig(configReader.readConfig());
			} catch (ConfigurationError configError) {
				System.out.println(configError.getMessage());
				return -2;
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
				return -3;
			}
		}

		ArrayList<String> sfiles = args.get("schemafile");
		if ((sfiles == null || sfiles.size() < 1) && configFile == null) {
			System.err.println("At least one schema/xsl file is required: -s/--schemafile or -c/--config");
			parser.printHelp();
			return -1;
		}
		if (sfiles != null) {
			for (String validationFile : sfiles) {
				validatorManager.addValidator(keyword, validationFile, false);
			}
		}

		ValidationResult result;
		try {
			result = validatorManager.validate(keyword, Files.readAllBytes(Paths.get(documentFile)));
			String of = args.get("output_format");
			if (of == null) {
				of = "text";
			}
			switch (of) {
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
			case "short":
				System.out.println("Errors: " + result.errorCount());
				System.out.println("Warnings: " + result.warningCount());
				break;
			case "none":
				break;
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
			default:
				System.out.println("Unknown output format: " + args.get("output_format"));
			}
			return result.errorCount();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Parse the command-line arguments and execute the validation. Exit code is the
	 * number of errors found by the validation, or a negative number if the
	 * validation could not be executed.
	 */
	public static void main(String[] argv) {
		try {
			CommandLineValidator validator = new CommandLineValidator(argv);
			System.exit(validator.run());
		} catch (ValidatorException valError) {
			System.err.print("Error: " + valError.toString());
			if (valError.getCause() != null) {
				Throwable root = valError;
				while (root.getCause() != null) {
					root = root.getCause();
				}
				System.err.println(": " + root.toString());
			} else {
				System.err.println();
			}
			System.exit(-1);
		}
	}
}
