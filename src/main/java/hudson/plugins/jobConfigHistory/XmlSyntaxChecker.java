package hudson.plugins.jobConfigHistory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class XmlSyntaxChecker {

	public static Answer check(File xmlFile) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);

		final boolean[] wellFormatted = {true};
		final String[] message = {""};
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new ErrorHandler() {



				private final String ERROR_STR = Messages.XmlSyntaxChecker_error();
				private final String WARNING_STR = Messages.XmlSyntaxChecker_warning();
				private final String FATAL_ERROR_STR = Messages.XmlSyntaxChecker_fatalError();
				private String getMessage(String prefix, Exception exception) { return prefix + exception.getMessage(); }

				@Override
				public void warning(SAXParseException exception) throws SAXException {  }

				@Override
				public void error(SAXParseException exception) throws SAXException {
					//TODO check necessity.
					wellFormatted[0] =false;
					message[0] = getMessage(ERROR_STR, exception);
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					wellFormatted[0] =false;
					message[0] = getMessage(FATAL_ERROR_STR, exception);
				}
			});
			try {
				builder.parse(xmlFile);
			} catch (SAXException | IOException exception) {

				message[0] = exception.getClass().getSimpleName() + Messages.XmlSyntaxChecker_occuredWhile() + exception.getMessage();
				wellFormatted[0] = false;
			}

		} catch (ParserConfigurationException exception) {

			message[0] = "ParserConfigurationException" + Messages.XmlSyntaxChecker_occuredWhile() + exception.getMessage();
			wellFormatted[0] = false;
		}
		return new Answer(message[0], wellFormatted[0]);
	}


	public static class Answer {
		private String message;
		private boolean wellFormatted;

		public Answer(String message, boolean wellFormatted) {
			this.message = message;
			this.wellFormatted = wellFormatted;
		}

		public String getMessage() {
			return message;
		}

		public boolean isWellFormatted() {
			return wellFormatted;
		}
	}
}
