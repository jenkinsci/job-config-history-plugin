package hudson.plugins.jobConfigHistory;

import hudson.model.Action;
import hudson.model.Hudson;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerResponse;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.StringUtills;

import java.util.Arrays;

import javax.servlet.ServletException;

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
     *
     * @param hudson injected jenkins
     */
    JobConfigHistoryBaseAction(Hudson hudson) {
        this.hudson = hudson;
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
     * See whether the current user may read and write configurations in the
     * object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     */
    protected abstract void checkConfigurePermission();

    /**
     * Returns whether the current user may read and write configurations in
     * the object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     *
     * @return true if the current user may read configurations.
     */
    protected abstract boolean hasConfigurePermission();

    /**
     * See whether the current user may read only configurations in the object
     * returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     */
    protected abstract void checkReadPermission();

    /**
     * Returns whether the current user may read only configurations in the
     * object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     *
     * @return true if the current user may read configurations.
     */
    protected abstract boolean hasReadPermission();

    /**
     * Returns the hudson instance.
     *
     * @return the hudson
     */
    protected Hudson getHudson() {
        return hudson;
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
        return new GetDiffLines(diffLines).get();
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
        final Patch patch = DiffUtils.diff(Arrays.asList(file1Lines), Arrays.asList(file2Lines));
        final List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(
                file1.getPath(), file2.getPath(), Arrays.asList(file1Lines), patch, 3);
        return StringUtills.join(unifiedDiff, "\n") + "\n";
    }
    
    /**
     * Parses the incoming {@literal POST} request and redirects as
     * {@literal GET showDiffFiles}.
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
    public void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
        throws ServletException, IOException {
        final MultipartFormDataParser parser = new MultipartFormDataParser(req);
        String timestamp1 = parser.get("timestamp1");
        String timestamp2 = parser.get("timestamp2");
        
        if (PluginUtils.parsedDate(timestamp1).after(PluginUtils.parsedDate(timestamp2))) {
            timestamp1 = parser.get("timestamp2");
            timestamp2 = parser.get("timestamp1");
        }
        rsp.sendRedirect("showDiffFiles?timestamp1=" + timestamp1
                + "&timestamp2=" + timestamp2);
    }
    
    /**
     * Action when 'Prev' or 'Next' button in showDiffFiles.jelly is pressed.
     * Forwards to the previous or next diff.

     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If XML file can't be read
     */
    public final void doDiffFilesPrevNext(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        final String timestamp1 = req.getParameter("timestamp1");
        final String timestamp2 = req.getParameter("timestamp2");
        rsp.sendRedirect("showDiffFiles?timestamp1=" + timestamp1
               + "&timestamp2=" + timestamp2);
    }

    /**
     * Overridable for tests.
     *
     * @return current request
     */
    StaplerRequest getCurrentRequest() {
        return Stapler.getCurrentRequest();
    }

    /**
     * Returns the plugin for tests.
     *
     * @return plugin
     */
    JobConfigHistory getPlugin() {
        return PluginUtils.getPlugin();
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

}
