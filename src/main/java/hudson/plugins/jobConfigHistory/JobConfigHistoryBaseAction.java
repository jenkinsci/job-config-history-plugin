package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffRow;
import difflib.DiffRow.Tag;
import difflib.DiffRowGenerator;
import difflib.DiffUtils;
import difflib.Patch;

import bmsi.util.Diff;
import bmsi.util.DiffPrint;
import bmsi.util.Diff.change;

/**
 * Implements some basic methods needed by the
 * {@link JobConfigHistoryRootAction} and {@link JobConfigHistoryProjectAction}.
 * 
 * @author mfriedenhagen
 */
public abstract class JobConfigHistoryBaseAction implements Action {

    /**
     * The hudson instance.
     */
    private final Hudson hudson;

    /**
     * Set the {@link Hudson} instance.
     */
    public JobConfigHistoryBaseAction() {
        hudson = Hudson.getInstance();
    }

    /**
     * {@inheritDoc}
     * 
     * Make method final, as we always want the same display name.
     */
    // @Override
    public final String getDisplayName() {
        return Messages.displayName();
    }

    /**
     * {@inheritDoc}
     * 
     * Do not make this method final as {@link JobConfigHistoryRootAction}
     * overrides this method.
     */
    // @Override
    public String getUrlName() {
        return JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Do we want 'raw' output?
     * 
     * @return true if request parameter type is 'raw'.
     */
    public final boolean wantRawOutput() {
        return isTypeParameter("raw");
    }

    /**
     * Do we want 'xml' output?
     * 
     * @return true if request parameter type is 'xml'.
     */
    public final boolean wantXmlOutput() {
        return isTypeParameter("xml");
    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as
     * String.
     * 
     * @return content of the {@code config.xml} found in directory given by the
     *         request parameter {@code file}.
     * @throws IOException
     *             if the config file could not be read or converted to an xml
     *             string.
     */
    public final String getFile() throws IOException {
        checkConfigurePermission();
        final boolean isJob = Boolean.parseBoolean(getRequestParameter("isJob"));
        final String name = getRequestParameter("name");
        final String timestamp = getRequestParameter("timestamp");

        final XmlFile xmlFile = getConfigXml(name, timestamp, isJob);
        return xmlFile.asString();
    }

    /**
     * Checks whether the type parameter of the current request equals
     * {@code toCompare}.
     * 
     * @param toCompare
     *            the string we want to compare.
     * @return true if {@code toCompare} equals request parameter type.
     */
    private boolean isTypeParameter(final String toCompare) {
        return getRequestParameter("type").equalsIgnoreCase(toCompare);
    }

    /**
     * Returns the configuration file (default is {@code config.xml}) located in
     * {@code path}. The submitted parameters get checked so that they can
     * not be abused to retrieve arbitrary xml configuration files located 
     * anywhere on the system.
     * 
     * @param name The name of the project.
     * @param timestamp The timestamp of the saved configuration as String.
     * @param isJob True if the configuration file belongs to a job (as opposed to a system property).
     * @return The configuration file as XmlFile.
     */
    protected XmlFile getConfigXml(final String name, final String timestamp, final boolean isJob) {
        final JobConfigHistory plugin = hudson.getPlugin(JobConfigHistory.class);
        final String rootDir;
        File configFile = null;
        String path = null;

        if (checkParameters(name, timestamp)) {
            if (isJob || name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
                rootDir = plugin.getJobHistoryRootDir().getPath();
            } else {
                rootDir = plugin.getConfiguredHistoryRootDir().getPath();
            }

            if (isJob) {
                final Item job = Hudson.getInstance().getItem(name);
                if (job == null) {
                    throw new IllegalArgumentException("A job with this name could not be found: " + name);
                } else {
                    job.checkPermission(AbstractProject.CONFIGURE);
                }
            } else {
                if (!name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
                    hudson.checkPermission(Permission.CONFIGURE);
                }
            }
            
            path = rootDir + "/" + name + "/" + timestamp;
            configFile = plugin.getConfigFile(new File(path));
        }

        if (configFile == null) {
            throw new IllegalArgumentException("Unable to get history from: "
                    + path);
        } else {
            return new XmlFile(configFile);
        }
    }

    /**
     * Checks the parameters 'name' and 'timestamp' and returns true if they are neither null 
     * nor suspicious.
     * @param name Name of job or system property.
     * @param timestamp Timestamp of config change.
     * @return True if parameters are okay.
     */
    private boolean checkParameters(String name, String timestamp) {
        if (name == null || timestamp == null || "null".equals(name) || "null".equals(timestamp)) {
            return false;
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException("Invalid directory name because of '..': " + name);
        }
        try {
            new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).parse(timestamp);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Timestamp does not contain a valid date: " + timestamp);
        }

        return true;
    }
    
    /**
     * Returns the parameter named {@code parameterName} from current request.
     * 
     * @param parameterName
     *            name of the parameter.
     * @return value of the request parameter or null if it does not exist.
     */
    protected String getRequestParameter(final String parameterName) {
        return Stapler.getCurrentRequest().getParameter(parameterName);
    }

    /**
     * See whether the current user may read configurations in the object
     * returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     */
    protected abstract void checkConfigurePermission();

    /**
     * Returns whether the current user may read configurations in the object
     * returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     * 
     * @return true if the current user may read configurations.
     */
    protected abstract boolean hasConfigurePermission();
    
    /**
     * Returns the hudson instance.
     * 
     * @return the hudson
     */
    protected final Hudson getHudson() {
        return hudson;
    }

    /**
     * Returns the JobConfigHistory plugin instance.
     * 
     * @return the JobConfigHistory plugin
     */
    protected final JobConfigHistory getPlugin() {
        return hudson.getPlugin(JobConfigHistory.class);
    }

    /**
     * Returns the object for which we want to provide access control.
     * 
     * @return the access controlled object.
     */
    protected abstract AccessControlled getAccessControlledObject();

    /**
     * Parses the incoming {@code POST} request and redirects as
     * {@code GET showDiffFiles}.
     * 
     * @param req
     *            incoming request
     * @param rsp
     *            outgoing response
     * @throws ServletException
     *             when parsing the request as {@link MultipartFormDataParser}
     *             does not succeed.
     * @throws IOException
     *             when the redirection does not succeed.
     */
    public final void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
        throws ServletException, IOException {
        final MultipartFormDataParser parser = new MultipartFormDataParser(req);
        rsp.sendRedirect("showDiffFiles?timestamp1=" + parser.get("timestamp1")
                + "&timestamp2=" + parser.get("timestamp2") + "&name=" + parser.get("name")
                + "&isJob=" + parser.get("isJob"));
    }

    /**
     * Returns a textual diff between two {@code config.xml} files which are identified 
     * by their timestamps and the project they belong to, given as parameters of
     * {@link Stapler#getCurrentRequest()}.
     * 
     * @return diff
     * @throws IOException
     *             if reading one of the config files does not succeed.
     */
    public final String getDiffFile() throws IOException {
        checkConfigurePermission();
        
        final boolean isJob = Boolean.parseBoolean(getRequestParameter("isJob"));
        final String name = getRequestParameter("name");

        final String timestamp1 = getRequestParameter("timestamp1");
        final String timestamp2 = getRequestParameter("timestamp2");

        final XmlFile configXml1 = getConfigXml(name, timestamp1, isJob);
        final String[] configXml1Lines = configXml1.asString().split("\\n");
        final XmlFile configXml2 = getConfigXml(name, timestamp2, isJob);
        final String[] configXml2Lines = configXml2.asString().split("\\n");
        return getDiff(configXml1.getFile(), configXml2.getFile(),
                configXml1Lines, configXml2Lines);
    }

    /**
     * Returns side-by-side (i.e. human-readable) diff view lines.
     * 
     * @return diff lines
     * @throws IOException
     *             if reading one of the config files does not succeed.
     */
    public final List<Line> getDiffLines() throws IOException {
        final List<String> diffLines = Arrays.asList(getDiffFile().split("\n"));

        final Patch diff = DiffUtils.parseUnifiedDiff(diffLines);
        final SideBySideView view = new SideBySideView();
        final DiffRowGenerator.Builder builder = new DiffRowGenerator.Builder();
        builder.columnWidth(Integer.MAX_VALUE);
        final DiffRowGenerator dfg = builder.build();
        
        int previousLeftPos = 0;
        
        for (final Delta delta : diff.getDeltas()) {
            final Chunk original = delta.getOriginal();
            final Chunk revised = delta.getRevised();

            final List<DiffRow> diffRows = dfg.generateDiffRows(
                    (List<String>) original.getLines(),
                    (List<String>) revised.getLines());

            // Chunk#getPosition() returns 0-origin line numbers, but we need 1-origin line numbers
            int leftPos = original.getPosition() + 1;
            int rightPos = revised.getPosition() + 1;

            if (previousLeftPos > 0 && leftPos - previousLeftPos > 1) {
                final Line skippingLine = new Line();
                skippingLine.skipping = true;
                view.addLine(skippingLine);
            }

            for (final DiffRow row : diffRows) {
                final Tag tag = row.getTag();
                final Line line = new Line();

                if (tag == Tag.INSERT) {
                    line.left.cssClass = "diff_original";
                    
                    line.right.lineNumber = rightPos;
                    line.right.text = row.getNewLine();
                    line.right.cssClass = "diff_revised";
                    rightPos++;

                } else if (tag == Tag.CHANGE) {
                    if (StringUtils.isNotEmpty(row.getOldLine())) {
                        line.left.lineNumber = leftPos;
                        line.left.text = row.getOldLine();
                        leftPos++;
                    }
                    line.left.cssClass = "diff_original";
                    
                    if (StringUtils.isNotEmpty(row.getNewLine())) {
                        line.right.lineNumber = rightPos;
                        line.right.text = row.getNewLine();
                        rightPos++;
                    }
                    line.right.cssClass = "diff_revised";

                } else if (tag == Tag.DELETE) {
                    line.left.lineNumber = leftPos;
                    line.left.text = row.getOldLine();
                    line.left.cssClass = "diff_original";
                    leftPos++;
                    
                    line.right.cssClass = "diff_revised";

                } else if (tag == Tag.EQUAL) {
                    line.left.lineNumber = leftPos;
                    line.left.text = row.getOldLine();
                    leftPos++;

                    line.right.lineNumber = rightPos;
                    line.right.text = row.getNewLine();
                    rightPos++;

                } else {
                    throw new IllegalStateException("Unknown tag pattern: " + tag);
                }
                line.tag = tag;
                view.addLine(line);
                previousLeftPos = leftPos;
            }
        }
        view.clearDuplicateLines();
        return view.getLines();
    }
    
    /**
     * Holds information for the SideBySideView.
     */
    public static class SideBySideView {

        /**
         * All lines of the view.
         */
        private final List<Line> lines = new ArrayList<Line>();

        /**
         * Returns the lines of the {@link SideBySideView}.
         *
         * @return an unmodifiable view of the lines.
         */
        public List<Line> getLines() {
            return Collections.unmodifiableList(lines);
        }

        /**
         * Adds a line.
         *
         * @param line A single line.
         */
        public void addLine(Line line) {
            lines.add(line);
        }
        
        /**
         * Deletes all dupes in the given lines.
         */
        public void clearDuplicateLines() {
            final TreeMap<Integer, Line> linesByNumbers = new TreeMap<Integer, Line>();
            final ArrayList<Line> dupes = new ArrayList<Line>();
            final Iterator<Line> iter = lines.iterator();
            while (iter.hasNext()) {
                final Line line = iter.next();
                final String lineNum = line.left.getLineNumber();
                if (lineNum.length() != 0) {
                    final int lineNumInt = Integer.parseInt(lineNum);
                    if (linesByNumbers.containsKey(lineNumInt)) {
                        if (line.tag == Tag.EQUAL) {
                            iter.remove();
                        } else {
                            dupes.add(linesByNumbers.get(lineNumInt));
                        }
                    } else {
                        linesByNumbers.put(lineNumInt, line);
                    }
                }
            }
            lines.removeAll(dupes);
        }
        
        /**
         * Holds information about a single line, which consists
         * of the left and right information of the diff.
         */
        public static class Line {
            /**The left version of a modificated line.*/
            private final Item left = new Item();
            /**The right version of a modificated line.*/
            private final Item right = new Item();
            /**True when line should be skipped.*/
            private boolean skipping;
            /**EQUAL, INSERT, CHANGE or DELETE.*/
            private Tag tag;
            

            /**
             * Returns the left version of a modificated line.
             *
             * @return left item.
             */
            public Item getLeft() {
                return left;
            }

            /**
             * Returns the right version of a modificated line.
             *
             * @return right item.
             */
            public Item getRight() {
                return right;
            }
            /**
             * Should we skip this line.
             * 
             * @return true when the line should be skipped.
             */
            public boolean isSkipping() {
                return skipping;
            }

            /**
             * Simple representation of a diff element.
             *
             * Additional to the the text this includes the linenumber
             * as well as the corresponding cssClass which signals wether
             * the item was modified, added or deleted.
             */
            public static class Item {
                /**Line number of Item.*/
                private Integer lineNumber;
                /**Text of Item.*/
                private String text;
                /**CSS Class of Item.*/
                private String cssClass;

                /**
                 * Returns the line number of the Item.
                 * @return lineNumber.
                 */
                public String getLineNumber() {
                    return lineNumber == null ? "" : String.valueOf(lineNumber);
                }

                /**
                 * Returns the text of the Item.
                 * @return text.
                 */
                public String getText() {
                    return text;
                }

                /**
                 * Returns the cssClass of the Item.
                 *
                 * @return cssClass.
                 */
                public String getCssClass() {
                    return cssClass;
                }
            }
        }
    }
    
    /**
     * Returns a unified diff between two string arrays.
     * 
     * @param file1
     *            first config file.
     * @param file2
     *            second config file.
     * @param file1Lines
     *            the lines of the first file.
     * @param file2Lines
     *            the lines of the second file.
     * @return unified diff
     */
    protected final String getDiff(final File file1, final File file2,
            final String[] file1Lines, final String[] file2Lines) {
        final change change = new Diff(file1Lines, file2Lines).diff_2(false);
        final DiffPrint.UnifiedPrint unifiedPrint = new DiffPrint.UnifiedPrint(
                file1Lines, file2Lines);
        final StringWriter output = new StringWriter();
        unifiedPrint.setOutput(output);
        unifiedPrint.print_header(file1.getPath(), file2.getPath());
        unifiedPrint.print_script(change);
        return output.toString();
    }
}
