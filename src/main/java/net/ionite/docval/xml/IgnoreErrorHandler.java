package net.ionite.docval.xml;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * Make the transformer silent on errors (these are handled through the
 * exception that is thrown
 */
public class IgnoreErrorHandler implements ErrorListener {
	@Override
	public void fatalError(TransformerException exception) {
		// ignore
	}

	@Override
	public void error(TransformerException exception) {
		// ignore
	}

	@Override
	public void warning(TransformerException exception) {
		// ignore
	}
}
