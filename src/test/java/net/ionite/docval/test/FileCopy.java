package net.ionite.docval.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * synchronized file copier to use instead of Files.copy(), as Files.copy() does
 * not synchronize the file system, leading to potential failed test cases
 * because a file hasn't been written yet.
 * 
 * @author Ionite
 *
 */
public class FileCopy {
	public static void copy(Path inputfile, Path outputfile) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Files.newInputStream(inputfile);
			out = Files.newOutputStream(outputfile, StandardOpenOption.SYNC, StandardOpenOption.CREATE);
			in.transferTo(out);
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
