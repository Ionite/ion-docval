package net.ionite.docval.server;

import java.io.File;
import java.io.IOException;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.SimpleLogger;

import net.ionite.docval.config.ConfigurationError;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import sun.misc.Signal;

/**
 * Main class of the HTTP server validator,
 */
public class DocValHttpServerMain {
	/**
	 * Parse the command line arguments and run the server<br />
	 * Exit code is non-zero if there is an error.
	 * 
	 * @param argv The command-line arguments provided
	 */
	public static void main(String[] argv) {
		ArgumentParser parser = ArgumentParsers.newFor("ion-docval-server").addHelp(true).build()
				.description("Validate a document given any number of XSD or Schematron XSLT files");
		parser.addArgument("-c", "--config").help("Use configuration file with schema/schematron definitions");
		parser.addArgument("-v", "--verbose").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print verbose debug output");
        parser.addArgument("-q", "--quiet").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Disable logging output");
		parser.addArgument("-V", "--version").action(Arguments.storeConst()).setConst(true).setDefault(false)
				.help("Print the software version and exit");

		try {
			Namespace args = parser.parseArgs(argv);

			if ((Boolean) args.get("version")) {
				System.out.println(DocValHttpServerMain.class.getPackage().getImplementationVersion());
				System.exit(0);
			}

			if ((Boolean) args.get("verbose")) {
				System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
			} else if ((Boolean) args.get("quiet")) {
				System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");
			} else {
				System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
			}
			Logger logger = LoggerFactory.getLogger(DocValHttpServer.class.getName());


			try {
				// Use, in order of preference:
				// - the file provided with the -c/--config argument
				// - {user.home}/.config/ion-docval.conf
				// - /etc/ion-docval.conf
				// - {current directory}/default_config.xml
				String configFile = args.get("config");
				//System.out.println("");
                List<File> files = List.of(
                    new File(System.getProperty("user.home") + "/.config/ion-docval.conf"),
                    new File("/etc/ion-docval.conf"),
                    new File(System.getProperty("user.dir") + "/ion-docval.conf"),
                    new File(System.getProperty("user.dir") + "/../ion-docval.conf")
                );
                for (File file : files) {
					logger.debug("Try configuration file path: " + file.getCanonicalPath());
					if (file.exists()) {
                        logger.debug("Found configuration file, using: " + file.getCanonicalPath());
						configFile = file.getCanonicalPath();
                        break;
					}
                }
				if (configFile == null) {
					System.err.println("No configuration file found and no file provided, aborting.");
                    System.err.println("The following configuration files locations were tried:");
                    for (File file : files) {
                        System.err.println(file.getCanonicalPath());
                    }
					System.exit(-1);
				}
				try {
					logger.debug("Using configuration file: " + configFile);

					DocValHttpServer server = new DocValHttpServer(configFile);
                    try {
                        Signal.handle(new Signal("XHUP"), signal -> {
                            try {
                                System.out.println(signal.getName() + " (" + signal.getNumber() + ")");
                                server.loadConfigFile();
                            } catch (Exception exc) {
                                System.err.println("Error reloading configuration file. Not updating configuration");
                                exc.printStackTrace();
                            }
                        });
                    } catch (IllegalArgumentException iae) {
                        // HUP not supported on this platform
                    }
					server.start();

					Thread.currentThread().join();
					System.exit(0);
				} catch (ConfigurationError configError) {
					System.out.println(configError.getMessage());
					System.exit(-2);
				} catch (IOException ioe) {
					System.out.println(ioe.getMessage());
					System.exit(-3);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} catch (ArgumentParserException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
	}
}
