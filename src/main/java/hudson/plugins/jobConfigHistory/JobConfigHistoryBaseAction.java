package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryBaseAction.class.getName());

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
        //man braucht Projektnamen, isJob und Timestamp
        
        final boolean isJob = Boolean.parseBoolean(getRequestParameter("isJob"));
        final JobConfigHistory plugin = hudson.getPlugin(JobConfigHistory.class);
        final String rootDir;
        
        if (isJob) {
            rootDir = plugin.getJobHistoryRootDir().getPath();
        } else {
            rootDir = plugin.getConfiguredHistoryRootDir().getPath();
        }

        final String name = getRequestParameter("name");
        final String path = rootDir + "/" + name + "/" + getRequestParameter("timestamp");
        
        final XmlFile xmlFile = getConfigXml(path);
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
     * {@code diffDir}. {@code diffDir} must either start with
     * {@code HUDSON_HOME} and contain {@code config-history} or be located
     * under the configured {@code historyRootDir}. It also must not contain a
     * '..' pattern. Otherwise an {@link IllegalArgumentException} will be
     * thrown.
     * <p>
     * This is to ensure that this plugin will not be abused to get arbitrary
     * xml configuration files located anywhere on the system.
     * 
     * @param diffDir
     *            timestamped history directory.
     * @return xmlfile.
     */
    protected XmlFile getConfigXml(final String path) {
        //hier sollte nur Datum übergeben werden, Rest zusammengebaut
        LOG.finest("path - " + path);
        
        final JobConfigHistory plugin = hudson.getPlugin(JobConfigHistory.class);
        final String allowedHistoryRootDir;
        if (plugin.getHistoryRootDir() == null || plugin.getHistoryRootDir().isEmpty()) {
            allowedHistoryRootDir = plugin.getConfiguredHistoryRootDir().getAbsolutePath();
        } else {
            allowedHistoryRootDir = plugin.getConfiguredHistoryRootDir().getParent();
        }
        
        
        File configFile = null;
        if (path != null) {
            configFile = plugin.getConfigFile(new File(path));
        }
        
/*        File configFile = null;
        if (diffDir != null) {
            if (!diffDir.startsWith(allowedHistoryRootDir)
                    || diffDir.contains("..")) {
                throw new IllegalArgumentException(diffDir
                        + " does not start with " + allowedHistoryRootDir
                        + " or contains '..'");
            }
            configFile = plugin.getConfigFile(new File(diffDir));
        }
*/        if (configFile == null) {
            throw new IllegalArgumentException("Unable to get history from: "
                    + path);
        } else {
            return new XmlFile(configFile);
        }
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
     * Returns a textual diff between two {@code config.xml} files located in
     * {@code histDir1} and {@code histDir2} directories given as parameters of
     * {@link Stapler#getCurrentRequest()}.
     * 
     * @return diff
     * @throws IOException
     *             if reading one of the config files does not succeed.
     */
    public final String getDiffFile() throws IOException {
        checkConfigurePermission();
        //man braucht Projektnamen, isJob und zwei Timestamps
        
        final boolean isJob = Boolean.parseBoolean(getRequestParameter("isJob"));
        final JobConfigHistory plugin = hudson.getPlugin(JobConfigHistory.class);
        final String rootDir;
        
        if (isJob) {
            rootDir = plugin.getJobHistoryRootDir().getPath();
        } else {
            rootDir = plugin.getConfiguredHistoryRootDir().getPath();
        }

        final String name = getRequestParameter("name");
        final String path1 = rootDir + "/" + name + "/" + getRequestParameter("timestamp1");
        final String path2 = rootDir + "/" + name + "/" + getRequestParameter("timestamp2");

        final XmlFile configXml1 = getConfigXml(path1);
        final String[] configXml1Lines = configXml1.asString().split("\\n");
        final XmlFile configXml2 = getConfigXml(path2);
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
         * 
         * TODO: encapsulate the call as we probably always want the
         * deduplicated lines only.
         */
        public void clearDuplicateLines() {
            final Iterator<Line> iter = lines.iterator();
            final Set<String> duplicateLineChecker = new HashSet<String>();
            while (iter.hasNext()) {
                final Line line = iter.next();
                final String lineNum = line.left.getLineNumber();
                if (lineNum.length() != 0) {
                    if (duplicateLineChecker.contains(lineNum)) {
                        iter.remove();
                    } else {
                        duplicateLineChecker.add(lineNum);
                    }
                }
            }
        }
        
        /**
         * Holds information about a single line, which consists
         * of the left and right information of the diff.
         */
        public static class Line {
            private final Item left = new Item();
            private final Item right = new Item();
            private boolean skipping = false;

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
             * Additional to the the text rhis includes the linenumber
             * as well as the corresponding cssClass which signals wether
             * the item was modified, added or deleted.
             */
            public static class Item {
                private Integer lineNumber;
                private String text;
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
