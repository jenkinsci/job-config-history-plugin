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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.InputSource;

import com.google.common.collect.Lists;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.StringUtills;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlunit.*;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;
import jenkins.model.Jenkins;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;
import org.xmlunit.util.Predicate;

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
     * config files does not succeed.
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
        return getDiffAsString(file1, file2, file1Lines, file2Lines, false);
    }

    private Diff getVersionDiffsOnly(final File file1, final File file2) {
        //TODO implement this, this is the way.
        DifferenceEvaluator versionDifferenceEvaluator = new DifferenceEvaluator() {
            //takes the comparison and the result that a possible previous DifferenceEvaluator created for this node
            // and compares the node based on whether there was a version change or not.
            // if there wasn't, "comparisonResult" is returned.
            //TODO find the name of this software pattern
            @Override
            public ComparisonResult evaluate(Comparison comparison, ComparisonResult comparisonResult) {

                //System.out.println("++++++++++++Test4++++++++++++: " + comparisonResult + " compType: " + comparison.getType());
                try {

                    if (comparison.getControlDetails().getValue().toString().contains("periodic-reincarnation@")) {
                        System.out.println("comparison with periodic stuff: " + comparison.getControlDetails().getValue().toString()
                                + "Type: " + comparison.getType());
                    }
                } catch (NullPointerException e) {;}
                if (comparison.getType() != ComparisonType.ATTR_VALUE) {
                    //only want to compare attribute values!


                    //TODO wieder zurückändern, falls XML-Unit für alle diffs genutzt werden soll.
                    //return comparisonResult;
                    return ComparisonResult.EQUAL;
                }

                Node controlNode = comparison.getControlDetails().getTarget();
                Node testNode = comparison.getTestDetails().getTarget();
                if (controlNode == null || testNode == null) {

                    //TODO wieder zurückändern, falls XML-Unit für alle diffs genutzt werden soll.
                    //return comparisonResult;
                    return ComparisonResult.EQUAL;
                }
                System.out.println(
                        "  Control Node: " + controlNode.getNodeName() + ", " + controlNode.getNodeValue()
                                + ", Test Node: " + testNode.getNodeName() + ", " + testNode.getNodeValue());

                String[] controlValue = controlNode.getNodeValue().split("@");
                String[] testValue = testNode.getNodeValue().split("@");


                if (!controlValue[0].equals(testValue[0])
                        || controlValue.length != 2 || testValue.length != 2) {
                    //different plugins or misformatted plugin attribute: version number not determinable
                    //TODO wieder zurückändern, falls XML-Unit für alle diffs genutzt werden soll.
                    //return comparisonResult;
                    return ComparisonResult.EQUAL;
                }
                System.out.println("  found as equal: " + controlValue[1] + ", " + testValue[1]);
                //TODO wieder zurückändern, falls XML-Unit für alle diffs genutzt werden soll.
                //return ComparisonResult.EQUAL;
                if (controlValue[1].equals(testValue[1])) {
                    return ComparisonResult.EQUAL;
                } else {
                    return ComparisonResult.DIFFERENT;
                }

            }
        };

        Diff diff = DiffBuilder.compare(Input.fromFile(file1)).withTest(Input.fromFile(file2))
                .ignoreWhitespace()
                //the next line should be used if one wanted to use XMLUnit for the computing of all diffs.
                //.withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default, versionDifferenceEvaluator))
                .withDifferenceEvaluator(versionDifferenceEvaluator)
                .build();
        //System.out.println("\n\n\n\n DIFFERENCES! \n\n\n\n");
        return diff;
    }

    /**
     * Returns a unified diff between two string arrays representing an xml file.
     * The order of elements in the xml file is NOT ignored.
     *
     * @param file1               first config file.
     * @param file2               second config file.
     * @param file1Lines          the lines of the first file.
     * @param file2Lines          the lines of the second file.
     * @param useRegex            determines whether <b>ignoredLinesPattern</b> shall be used.
     * @return unified diff
     */
    protected final String getDiffAsString(final File file1, final File file2, final String[] file1Lines,
                                           final String[] file2Lines, final boolean useRegex) {

        //calculate diffs to be excluded from the output.
        Diff versionDiffs = getVersionDiffsOnly(file1, file2);
        //calculate all diffs.
        final Patch patch = DiffUtils.diff(Arrays.asList(file1Lines), Arrays.asList(file2Lines));

        if (useRegex) {
            //bug/ feature in library: empty deltas are shown, too.
            List<Delta> deltasToBeRemovedAfterTheMainLoop = new LinkedList<Delta>();
            for (Delta delta : patch.getDeltas()) {
                // Modify both deltas and save the changes.
                List<String> originalLines = Lists.newArrayList((List<String>) delta.getOriginal().getLines());
                List<String> revisedLines = Lists.newArrayList((List<String>) delta.getRevised().getLines());

                for (int line = 0; line < Math.max(originalLines.size(), revisedLines.size()); ++line) {
                    // Delete lines which match the regex from both deltas.

                    //These must be calculated IN the loop because it changes in some iterations.
                    int oriLinesSize = originalLines.size();
                    int revLinesSize = revisedLines.size();


                    if (line > oriLinesSize - 1) {
                        // line <= revLinesSize-1, because of loop invariant.
                        // ori line is empty.
                    } else if (line > revLinesSize - 1) {
                        // line <= oriLinesSize-1, because of loop invariant.
                        // rev line is empty.
                    } else {
                        String originalLine = originalLines.get(line);
                        String revisedLine = revisedLines.get(line);
                        String diffStr = StringUtils.difference(originalLine, revisedLine);
                        // both lines are non-empty
                        for (Difference difference : versionDiffs.getDifferences()) {
                            String controlValue = difference.getComparison().getControlDetails().getValue().toString();
                            String testValue     = difference.getComparison().getTestDetails().getValue().toString();

                            if ((originalLine.contains(controlValue) && revisedLine.contains(testValue))
                            || (originalLine.contains(testValue) && revisedLine.contains(controlValue))) {
                                originalLines.remove(line);
                                revisedLines.remove(line);
                            }
                        }
                    }
                }
                if (originalLines.isEmpty() && revisedLines.isEmpty()) {
                    //remove the delta from the list.
                    deltasToBeRemovedAfterTheMainLoop.add(delta);
                }
                delta.getOriginal().setLines(originalLines);
                delta.getRevised().setLines(revisedLines);
            }
            patch.getDeltas().removeAll(deltasToBeRemovedAfterTheMainLoop);
        }

        final List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(file1.getPath(), file2.getPath(),
                Arrays.asList(file1Lines), patch, 3);
        return StringUtills.join(unifiedDiff, "\n") + "\n";
    }

    private static String stringArrayToString(String[] arr) {
        String result = "";
        for (int i = 0; i < arr.length; ++i) {
            result += arr[i];
            if (i < arr.length - 1) {
                result += "\n";
            }
        }
        return result;
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
