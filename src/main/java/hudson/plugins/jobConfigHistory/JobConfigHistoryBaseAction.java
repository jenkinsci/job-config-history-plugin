package hudson.plugins.jobConfigHistory;

import hudson.model.Action;
import hudson.model.Hudson;
import hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction.SideBySideView.Line;
import hudson.security.AccessControlled;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Stapler;

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
import org.kohsuke.stapler.StaplerRequest;

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
     * For tests only.
     * @param hudson
     */
    JobConfigHistoryBaseAction(Hudson hudson) {
        this.hudson = hudson;
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
     * Returns how the config file should be formatted in configOutput.jelly: as plain text or xml.
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
     * Checks the url parameter 'timestamp' and returns true if it is parseable as a date.
     * @param timestamp Timestamp of config change.
     * @return True if timestamp is okay.
     */
    protected boolean checkTimestamp(String timestamp) {
        if (timestamp == null || "null".equals(timestamp)) {
            return false;
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
        return getCurrentRequest().getParameter(parameterName);
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
     * Returns side-by-side (i.e. human-readable) diff view lines.
     *
     * @param diffLines Unified diff as list of Strings.
     * @return Nice and clean diff as list of single Lines.
     * @throws IOException
     *             if reading one of the config files does not succeed.
     */
    public final List<Line> getDiffLines(List<String> diffLines) throws IOException {
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
    protected final String getDiffAsString(final File file1, final File file2,
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

    StaplerRequest getCurrentRequest() {
        return Stapler.getCurrentRequest();
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
}
