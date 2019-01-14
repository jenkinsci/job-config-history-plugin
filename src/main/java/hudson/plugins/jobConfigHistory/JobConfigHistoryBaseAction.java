/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.InputSource;

import com.google.common.collect.Lists;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.StringUtills;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;
import jenkins.model.Jenkins;

/**
 * Implements some basic methods needed by the
 * {@link JobConfigHistoryRootAction} and {@link JobConfigHistoryProjectAction}.
 *
 * @author Mirko Friedenhagen
 */
public abstract class JobConfigHistoryBaseAction implements Action {

	/**
	 * The jenkins instance.
	 */
	private final Jenkins jenkins;

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private static final Logger LOG = Logger.getLogger(JobConfigHistoryBaseAction.class.getName());

	/**
	 * Set the {@link Jenkins} instance.
	 */
	public JobConfigHistoryBaseAction() {
		jenkins = Jenkins.getInstance();
	}

	/**
	 * For tests only.
	 *
	 * @param jenkins injected jenkins
	 */
	JobConfigHistoryBaseAction(Jenkins jenkins) {
		this.jenkins = jenkins;
	}

	@Override
	public String getDisplayName() {
		return Messages.displayName();
	}

	@Override
	public String getUrlName() {
		return JobConfigHistoryConsts.URLNAME;
	}

	/**
	 * Returns how the config file should be formatted in configOutput.jelly: as
	 * plain text or xml.
	 *
	 * @return "plain" or "xml"
	 */
	public final String getOutputType() {
		if (("xml").equalsIgnoreCase(getRequestParameter("type"))) {
			return "xml";
		}
		return "plain";
	}

	/**
	 * Checks the url parameter 'timestamp' and returns true if it is parseable as a
	 * date.
	 * 
	 * @param timestamp Timestamp of config change.
	 * @return True if timestamp is okay.
	 */
	protected boolean checkTimestamp(String timestamp) {
		if (timestamp == null || "null".equals(timestamp)) {
			return false;
		}
		PluginUtils.parsedDate(timestamp);
		return true;
	}

	/**
	 * Returns the parameter named {@code parameterName} from current request.
	 *
	 * @param parameterName name of the parameter.
	 * @return value of the request parameter or null if it does not exist.
	 */
	protected String getRequestParameter(final String parameterName) {
		return getCurrentRequest().getParameter(parameterName);
	}

	/**
	 * See whether the current user may read configurations in the object returned
	 * by {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
	 */
	protected abstract void checkConfigurePermission();

	/**
	 * Returns whether the current user may read configurations in the object
	 * returned by {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
	 *
	 * @return true if the current user may read configurations.
	 */
	protected abstract boolean hasConfigurePermission();

	/**
	 * Returns the jenkins instance.
	 *
	 * @return the jenkins
	 */
	protected Jenkins getJenkins() {
		return jenkins;
	}

	/**
	 * Returns the object for which we want to provide access control.
	 *
	 * @return the access controlled object.
	 */
	protected abstract AccessControlled getAccessControlledObject();

	/**
	 * Returns side-by-side (i.e. human-readable) diff view lines.
	 *
	 * @param diffLines Unified diff as list of Strings.
	 * @return Nice and clean diff as list of single Lines. if reading one of the
	 *         config files does not succeed.
	 */
	public final List<Line> getDiffLines(List<String> diffLines) throws IOException {
		return new GetDiffLines(diffLines).get();
	}

	/**
	 * Returns a unified diff between two string arrays.
	 *
	 * @param file1      first config file.
	 * @param file2      second config file.
	 * @param file1Lines the lines of the first file.
	 * @param file2Lines the lines of the second file.
	 * @return unified diff
	 */
	protected final String getDiffAsString(final File file1, final File file2, final String[] file1Lines,
			final String[] file2Lines) {
		/*
		 * final Patch patch = DiffUtils.diff(Arrays.asList(file1Lines),
		 * Arrays.asList(file2Lines)); final List<String> unifiedDiff =
		 * DiffUtils.generateUnifiedDiff( file1.getPath(), file2.getPath(),
		 * Arrays.asList(file1Lines), patch, 3); return StringUtills.join(unifiedDiff,
		 * "\n") + "\n";
		 */
		// return getDiffAsString(file1, file2,
		// file1Lines, file2Lines, "");
		return getDiffAsString(file1, file2, file1Lines, file2Lines, false, "");
	}

	protected final String getDiffAsString(final File file1, final File file2, final String[] file1Lines,
			final String[] file2Lines, boolean useRegex, final String ignoredLinesPattern) {
		final Patch patch = DiffUtils.diff(Arrays.asList(file1Lines), Arrays.asList(file2Lines));

		//TODO figure out something better than the bool-solution
		if (useRegex) {
			//bug in library: empty deltas are shown, too.
			//TODO probably need to adjust the non-empty deltas also...
			List<Delta> deltasToBeRemovedAfterTheMainLoop = new LinkedList<Delta>();
			for (Delta delta : patch.getDeltas()) {
				List<String> originalLines = Lists.newArrayList((List<String>) delta.getOriginal().getLines());
				List<String> revisedLines = Lists.newArrayList((List<String>) delta.getRevised().getLines());
				//---------------------------------------------------------------------------------------------------------------DEBUG
				System.out.println("---BEFORE:");
				System.out.println("Original Lines: " + originalLines);
				System.out.println("Revised Lines: " +revisedLines);
				//--------------------------------------------------------------------------------------------------------------\DEBUG
				for (int line = 0; line < Math.max(originalLines.size(), revisedLines.size()); ++line) {
					// TODO: this is crappy, O(n) if not ArrayList, change that.
					int oriLinesSize = originalLines.size();
					int revLinesSize = revisedLines.size();
					//---------------------------------------------------------------------------------------------------------------DEBUG
					System.out.println("---MID:");
					//--------------------------------------------------------------------------------------------------------------\DEBUG
					if (line > oriLinesSize - 1) {
						// line <= revLinesSize-1, because of loop invariant.
						// ori line is empty.
						//---------------------------------------------------------------------------------------------------------------DEBUG
						System.out.println("Line Pair: [" + "EMPTYY" + ", " + revisedLines.get(line) + "]");
						//--------------------------------------------------------------------------------------------------------------\DEBUG
						if (revisedLines.get(line).matches(ignoredLinesPattern)) {
							revisedLines.remove(line);
						}
					} else if (line > revLinesSize - 1) {
						// line <= oriLinesSize-1, because of loop invariant.
						// rev line is empty.
						//---------------------------------------------------------------------------------------------------------------DEBUG
						System.out.println("Line Pair: [" + originalLines.get(line) + ", " + "EMPTYY" + "]");
						//--------------------------------------------------------------------------------------------------------------\DEBUG
						if (originalLines.get(line).matches(ignoredLinesPattern)) {
							originalLines.remove(line);
						}
					} else {
						// both lines are non-empty
						//---------------------------------------------------------------------------------------------------------------DEBUG
						System.out.println(
								"Line Pair: [" + originalLines.get(line) + ", " + revisedLines.get(line) + "]");
						//--------------------------------------------------------------------------------------------------------------\DEBUG
						if ((originalLines.get(line).matches(ignoredLinesPattern))
								&& (revisedLines.get(line).matches(ignoredLinesPattern))) {
							originalLines.remove(line);
							revisedLines.remove(line);
						}
					}
				}
				if (originalLines.isEmpty() && revisedLines.isEmpty()) {
					//remove the delta from the list.
					deltasToBeRemovedAfterTheMainLoop.add(delta);
				}
				//---------------------------------------------------------------------------------------------------------------DEBUG
				System.out.println("---AFTER:");
				System.out.println("Original Lines: " + originalLines);
				System.out.println("Revised Lines: " +revisedLines);
				//--------------------------------------------------------------------------------------------------------------\DEBUG
				delta.getOriginal().setLines(originalLines);
				delta.getRevised().setLines(revisedLines);
			}
			patch.getDeltas().removeAll(deltasToBeRemovedAfterTheMainLoop);
			//patch.
		}

		final List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(file1.getPath(), file2.getPath(),
				Arrays.asList(file1Lines), patch, 3);
		return StringUtills.join(unifiedDiff, "\n") + "\n";
	}

	/**
	 * Parses the incoming {@literal POST} request and redirects as
	 * {@literal GET showDiffFiles}.
	 *
	 * @param req incoming request
	 * @param rsp outgoing response
	 * @throws ServletException when parsing the request as
	 *                          {@link MultipartFormDataParser} does not succeed.
	 * @throws IOException      when the redirection does not succeed.
	 */
	public void doDiffFiles(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
		String timestamp1 = req.getParameter("timestamp1");
		String timestamp2 = req.getParameter("timestamp2");

		if (PluginUtils.parsedDate(timestamp1).after(PluginUtils.parsedDate(timestamp2))) {
			timestamp1 = req.getParameter("timestamp2");
			timestamp2 = req.getParameter("timestamp1");
		}
		rsp.sendRedirect("showDiffFiles?timestamp1=" + timestamp1 + "&timestamp2=" + timestamp2);
	}

	/**
	 * Action when 'Prev' or 'Next' button in showDiffFiles.jelly is pressed.
	 * Forwards to the previous or next diff.
	 * 
	 * @param req StaplerRequest created by pressing the button
	 * @param rsp Outgoing StaplerResponse
	 * @throws IOException If XML file can't be read
	 */
	public final void doDiffFilesPrevNext(StaplerRequest req, StaplerResponse rsp) throws IOException {
		final String timestamp1 = req.getParameter("timestamp1");
		final String timestamp2 = req.getParameter("timestamp2");
		rsp.sendRedirect("showDiffFiles?timestamp1=" + timestamp1 + "&timestamp2=" + timestamp2);
	}

	/**
	 * Overridable for tests.
	 *
	 * @return current request
	 */
	protected StaplerRequest getCurrentRequest() {
		return Stapler.getCurrentRequest();
	}

	/**
	 * Returns the plugin for tests.
	 *
	 * @return plugin
	 */
	protected JobConfigHistory getPlugin() {
		return PluginUtils.getPlugin();
	}

	/**
	 * For tests.
	 *
	 * @return historyDao
	 */
	protected HistoryDao getHistoryDao() {
		return PluginUtils.getHistoryDao();
	}

	private Writer sort(File file) throws IOException {
		try (Reader source = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			InputStream xslt = JobConfigHistoryBaseAction.class.getResourceAsStream("xslt/sort.xslt");
			Objects.requireNonNull(xslt);
			Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslt));
			Writer result = new StringWriter();
			transformer.transform(new SAXSource(new InputSource(source)), new StreamResult(result));
			transformer.reset();

			return result;
		} catch (TransformerFactoryConfigurationError | TransformerException | NullPointerException e) {
			LogRecord lr = new LogRecord(Level.WARNING, "Diff may have extra changes for XML config {0}");
			lr.setParameters(new Object[] { file.toPath() });
			lr.setThrown(e);
			LOG.log(lr);
		}

		// fallback - return an original file as is
		Writer fallback = new StringWriter();
		new XmlFile(file).writeRawTo(fallback);
		return fallback;
	}

	/**
	 * Takes the two config files and returns the diff between them as a list of
	 * single lines.
	 *
	 * @param leftConfig  first config file
	 * @param rightConfig second config file
	 * @return Differences between two config versions as list of lines.
	 * @throws IOException If diff doesn't work or xml files can't be read.
	 */
	protected final List<Line> getLines(XmlFile leftConfig, XmlFile rightConfig) throws IOException {
		final String[] leftLines = sort(leftConfig.getFile()).toString().split("\\n");
		final String[] rightLines = sort(rightConfig.getFile()).toString().split("\\n");
		final String diffAsString = getDiffAsString(leftConfig.getFile(), rightConfig.getFile(), leftLines, rightLines);
		final List<String> diffLines = Arrays.asList(diffAsString.split("\n"));

		return getDiffLines(diffLines);
	}

}
