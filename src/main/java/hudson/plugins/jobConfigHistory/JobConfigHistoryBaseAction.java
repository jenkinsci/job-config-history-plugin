package hudson.plugins.jobConfigHistory;

import hudson.model.Action;
import hudson.model.Hudson;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
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
        PluginUtils.parsedDate(timestamp);
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
                skippingLine.setSkipping(true);
                view.addLine(skippingLine);
            }

            for (final DiffRow row : diffRows) {
                final Tag tag = row.getTag();
                final Line line = new Line();
                final Line.Item left = line.getLeft();
                final Line.Item right = line.getRight();
                if (tag == Tag.INSERT) {
                    left.setCssClass("diff_original");

                    right.setLineNumber(rightPos);
                    right.setText(row.getNewLine());
                    right.setCssClass("diff_revised");
                    rightPos++;

                } else if (tag == Tag.CHANGE) {
                    if (StringUtils.isNotEmpty(row.getOldLine())) {
                        left.setLineNumber(leftPos);
                        left.setText(row.getOldLine());
                        leftPos++;
                    }
                    left.setCssClass("diff_original");

                    if (StringUtils.isNotEmpty(row.getNewLine())) {
                        right.setLineNumber(rightPos);
                        right.setText(row.getNewLine());
                        rightPos++;
                    }
                    right.setCssClass("diff_revised");

                } else if (tag == Tag.DELETE) {
                    left.setLineNumber(leftPos);
                    left.setText(row.getOldLine());
                    left.setCssClass("diff_original");
                    leftPos++;

                    right.setCssClass("diff_revised");

                } else if (tag == Tag.EQUAL) {
                    left.setLineNumber(leftPos);
                    left.setText(row.getOldLine());
                    leftPos++;

                    right.setLineNumber(rightPos);
                    right.setText(row.getNewLine());
                    rightPos++;

                } else {
                    throw new IllegalStateException("Unknown tag pattern: " + tag);
                }
                line.setTag(tag);
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
}
