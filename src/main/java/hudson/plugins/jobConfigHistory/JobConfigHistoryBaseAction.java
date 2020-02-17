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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.InputSource;

import com.google.common.collect.Lists;

import org.w3c.dom.Node;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import static java.util.logging.Level.WARNING;

/**
 * Implements some basic methods needed by the
 * {@link JobConfigHistoryRootAction} and {@link JobConfigHistoryProjectAction}.
 *
 * @author Mirko Friedenhagen
 */
public abstract class JobConfigHistoryBaseAction implements Action {

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private static final Logger LOG = Logger.getLogger(JobConfigHistoryBaseAction.class.getName());

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

	public abstract boolean hasAdminPermission();

	public abstract boolean hasDeleteEntryPermission();

    protected abstract void checkDeleteEntryPermission();

    /**
     * Returns whether the current user may read configurations in the object
     * returned by {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     *
     * @return true if the current user may read configurations.
     */
    protected abstract boolean hasConfigurePermission();

    /**
     *
     * @return the amount of revisions existing for the given request (not page-dependant).
     */
	public abstract int getRevisionAmount();

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
    public final List<Line> getDiffLines(List<String> diffLines) {
        try {
            return new GetDiffLines(diffLines).get();
        } catch (DiffException e) {
            LOG.log(Level.SEVERE, "DiffException occurred while trying to get diffs: {0}", e.getMessage());
            return Collections.emptyList();
        }
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

    private Diff getVersionDiffsOnly(final String file1Str, final String file2Str) {
        DifferenceEvaluator versionDifferenceEvaluator = new DifferenceEvaluator() {
            //takes the comparison and the result that a possible previous DifferenceEvaluator created for this node
            // and compares the node based on whether there was a version change or not.
            // if there wasn't, "comparisonResult" is returned.
            @Override
            public ComparisonResult evaluate(Comparison comparison, ComparisonResult comparisonResult) {
                if (comparison.getType() != ComparisonType.ATTR_VALUE) {
                    //only want to compare attribute values!
                    return ComparisonResult.EQUAL;
                }

                Node controlNode = comparison.getControlDetails().getTarget();
                Node testNode = comparison.getTestDetails().getTarget();
                if (controlNode == null || testNode == null) {
                    return ComparisonResult.EQUAL;
                }

                String[] controlValue = controlNode.getNodeValue().split("@");
                String[] testValue = testNode.getNodeValue().split("@");
                if (!controlValue[0].equals(testValue[0])
                        || controlValue.length != 2 || testValue.length != 2) {
                    //different plugins or misformatted plugin attribute: version number not determinable
                    return ComparisonResult.EQUAL;
                }
                if (controlValue[1].equals(testValue[1])) {
                    return ComparisonResult.EQUAL;
                } else {
                    return ComparisonResult.DIFFERENT;
                }

            }
        };

        return DiffBuilder.compare(Input.fromString(file1Str)).withTest(Input.fromString(file2Str))
                .ignoreWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                //the next line should be used if one wanted to use XMLUnit for the computing of all diffs.
                //.withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default, versionDifferenceEvaluator))
                .withDifferenceEvaluator(versionDifferenceEvaluator)
                .build();
    }


    /**
     * Get the current request's 'showVersionDiffs'-parameter. If there is none, "True" is returned.
     *
     * @return
     * 		<b>true</b> if the current request has set this parameter to true or not at all.
     *		<br>
     * 		<b>false</b> else
     */
    public String getShowVersionDiffs() {
        String showVersionDiffs = this.getRequestParameter("showVersionDiffs");
        return (showVersionDiffs  == null) ? "True" : showVersionDiffs;
    }

    /**
     * Returns the diff between two config files as a list of single lines.
     * Takes the two timestamps and the name of the system property or the
     * deleted job from the url parameters.
     *
     * @return Differences between two config versions as list of lines.
     * @throws IOException
     *             If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines() throws IOException {
        final boolean hideVersionDiffs = !Boolean.parseBoolean(getShowVersionDiffs());
        return getLines(hideVersionDiffs);
    }

    public abstract List<Line> getLines(boolean useRegex) throws IOException;

    private String reformatAndConcatStringArray(String[] arr) {
        String ret = "";

        //this needs to be done because the sorting  process writes the first line as:
        //<?xml version="1.0" encoding="UTF-8"?><project>
        //which can't be processed by xmlUnit...
        final String firstLineRegex = "<[^<>]*>";
        final String firstLine = arr[0];

        if (firstLine.matches(firstLineRegex)) {
            ret += firstLine;
        } else {
            //extract anything that doesnt match.
            String tagThatBelongsInTheNextLine = firstLine.replaceFirst(firstLineRegex, "");

            ret += firstLine.substring(0, firstLine.length()-tagThatBelongsInTheNextLine.length()) + "\n"
                + tagThatBelongsInTheNextLine + "\n";
        }

        for (int i = 1; i < arr.length; ++i) {
            if (i < arr.length-1) {
                ret = ret.concat(arr[i]).concat("\n");
            } else {
                ret = ret.concat(arr[i]);
            }
        }
        return ret;
    }
    /**
     * Returns a unified diff between two string arrays representing an xml file.
     * The order of elements in the xml file is NOT ignored.
     *
     * @param file1               first config file.
     * @param file2               second config file.
     * @param file1Lines          the lines of the first file.
     * @param file2Lines          the lines of the second file.
     * @param hideVersionDiffs            determines whether version diffs shall be shown or not.
     * @return unified diff
     */
    protected final String getDiffAsString(final File file1, final File file2, final String[] file1Lines,
                                           final String[] file2Lines, final boolean hideVersionDiffs) {

        //calculate all diffs.
        final Patch<String> patch;
        try {
            patch = DiffUtils.diff(Arrays.asList(file1Lines), Arrays.asList(file2Lines));
        } catch (DiffException e) {
            LOG.log(Level.SEVERE, "DiffException occurred while trying to calculate diffs: {0}", e.getMessage());
            return "";
        }
        if (hideVersionDiffs) {
            //calculate diffs to be excluded from the output.
            Diff versionDiffs = getVersionDiffsOnly(reformatAndConcatStringArray(file1Lines), reformatAndConcatStringArray(file2Lines));
            //feature in library: empty deltas are shown, too.
            List<AbstractDelta<String>> deltasToBeRemovedAfterTheMainLoop = new LinkedList<>();

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                // Modify both deltas and save the changes.

                List<String> originalLines = Lists.newArrayList((List<String>) delta.getSource().getLines());
                List<String> revisedLines = Lists.newArrayList((List<String>) delta.getTarget().getLines());
                for (Difference versionDifference : versionDiffs.getDifferences()) {
                    //check for each calculated versionDifference where it occurred and delete it.
                    String controlValue = versionDifference.getComparison().getControlDetails().getValue().toString();
                    String testValue     = versionDifference.getComparison().getTestDetails().getValue().toString();

                    //go through both line lists and find pairs.
                    for (int oriLineNumber = 0; oriLineNumber < originalLines.size(); oriLineNumber++) {
                        String currentOriLine = originalLines.get(oriLineNumber);

                        String otherValue = "";
                        if (currentOriLine.contains(controlValue)) {
                            otherValue = testValue;

                        } else if  (currentOriLine.contains(testValue)) {
                            otherValue = controlValue;
                        } else continue;
                            //search for test value in the other list
                        for (int revLineNumber = 0; revLineNumber < revisedLines.size(); revLineNumber++) {
                            String currentRevLine = revisedLines.get(revLineNumber);
                            if (currentRevLine.contains(otherValue)) {
                                //found both lines. Delete them
                                originalLines.remove(oriLineNumber);
                                revisedLines.remove(revLineNumber);
                            }
                        }
                    }
                }
                if (originalLines.isEmpty() && revisedLines.isEmpty()) {
                    //remove the delta from the list.
                    deltasToBeRemovedAfterTheMainLoop.add(delta);
                }
                delta.getSource().setLines(originalLines);
                delta.getTarget().setLines(revisedLines);
            }
            patch.getDeltas().removeAll(deltasToBeRemovedAfterTheMainLoop);
        }

        return StringUtils.join(
            UnifiedDiffUtils.generateUnifiedDiff(
                file1.getPath(),
                file2.getPath(),
                Arrays.asList(file1Lines),
                patch,
                3
            ),
            "\n"
        ) + "\n";
    }

    /**
     *
     * @param currentPageNum the current page number
     * @return the same as {@link #getRelevantPageNums(int, int)}, using {@link #getMaxPageNum()} as second parameter.
     */
    public List<Integer> getRelevantPageNums(int currentPageNum) {
        return getRelevantPageNums(currentPageNum, getMaxPageNum());
    }

    /**
     *
     * @param currentPageNum the current page number
     * @param maxPageNum the highest page number
     * @return a list representing all page navigation entries which are displayed. These include:
     * <ul>
     *     <li>0</li>
     *     <li>maxPageNum</li>
     *     <li>all integers in {k in [currentPageNum - PAGING_EPSILON, currentPageNum + PAGING_EPSILON] | k &gt; 0 &amp;&amp; k &lt; maxPageNum}.</li>
     * </ul>
     */
    public List<Integer> getRelevantPageNums(int currentPageNum, int maxPageNum) {
        final int epsilon = JobConfigHistoryConsts.PAGING_EPSILON;
        final HashSet<Integer> pageNumsSet = new HashSet<>();
        pageNumsSet.add(0);
        pageNumsSet.add(maxPageNum);

        if (maxPageNum > 10) {
            pageNumsSet.add(currentPageNum);
            //add everything in epsilon around current pageNum
            for (int i = currentPageNum; i <= Math.min(currentPageNum+epsilon, maxPageNum); i++) {
                pageNumsSet.add(i);
            }
            for (int i = currentPageNum; i >= Math.max(0, currentPageNum-epsilon); i--) {
                pageNumsSet.add(i);
            }
        } else {
            for (int i = 0; i <= maxPageNum; i++) {
                pageNumsSet.add(i);
            }
        }
        ArrayList<Integer> pageNumsList = new ArrayList<>(pageNumsSet);
        pageNumsList.sort(Comparator.naturalOrder());
        //add code for dots:
        int lastNumber = pageNumsList.get(0);
        for (int i = 1; i < pageNumsList.size(); i++) {
            int thisNumber = pageNumsList.get(i);
            if (lastNumber+1 != thisNumber) {
                //add dots before thisNumber. -1 stands for dots (easier than defining a special class etc)
                pageNumsList.add(i++, -1);
            }

            lastNumber = thisNumber;
        }
        return pageNumsList;
    }

    /**
     * @return the configured maximum entries per page.
     */
    public int getMaxEntriesPerPage() {
        final String maxEntriesPerPage = getPlugin().getMaxEntriesPerPage();
        try {
            return (maxEntriesPerPage == null || maxEntriesPerPage.equals(""))
                ? JobConfigHistoryConsts.DEFAULT_MAX_ENTRIES_PER_PAGE
                : Integer.parseInt(maxEntriesPerPage);
        } catch (NumberFormatException e) {
            LOG.log(WARNING, "Configured MaxEntriesPerPage does not represent an integer: {0}. Falling back to default.", maxEntriesPerPage);
            return JobConfigHistoryConsts.DEFAULT_MAX_ENTRIES_PER_PAGE;
        }
    }

    /**
     * @return the maximum page number given this request's number of entries per page and the revision amount.
     */
    public int getMaxPageNum() {
        String entriesPerPageStr = getCurrentRequest().getParameter("entriesPerPage");
        if (entriesPerPageStr != null && entriesPerPageStr.equals("all")) return 0;
        int entriesPerPage = (entriesPerPageStr != null && !entriesPerPageStr.equals("")) ? Integer.parseInt(entriesPerPageStr) : getMaxEntriesPerPage();
        int revisionAmount = getRevisionAmount();

        int div = revisionAmount / entriesPerPage;
        return (revisionAmount % entriesPerPage) == 0 ? div-1 : div;
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
        //this produces a sorted xml without indentation.
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
	protected final List<Line> getLines(XmlFile leftConfig, XmlFile rightConfig, boolean hideVersionDiffs) throws IOException {

		final String[] leftLines = sort(leftConfig.getFile()).toString().split("\\n");
		final String[] rightLines = sort(rightConfig.getFile()).toString().split("\\n");

		final String diffAsString = getDiffAsString(leftConfig.getFile(), rightConfig.getFile(), leftLines,
                rightLines, hideVersionDiffs);
		final List<String> diffLines = Arrays.asList(diffAsString.split("\n"));

		return getDiffLines(diffLines);
	}

}
