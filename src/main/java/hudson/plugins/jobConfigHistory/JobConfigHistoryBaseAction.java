package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import bmsi.util.Diff;
import bmsi.util.DiffPrint;
import bmsi.util.Diff.change;

/**
 * Implements some basic methods needed by the {@link JobConfigHistoryRootAction} and {@link JobConfigHistoryProjectAction}.
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
        return JobConfigHistoryConsts.DISPLAYNAME;
    }

    /**
     * {@inheritDoc}
     *
     * Make method final, as we always want the same icon file. Returns {@code null} to hide the icon if the user is not
     * allowed to configure jobs.
     */
    // @Override
    public final String getIconFileName() {
        return hasConfigurePermission() ? JobConfigHistoryConsts.ICONFILENAME : null;
    }

    /**
     * {@inheritDoc}
     *
     * Do not make this method final as {@link JobConfigHistoryRootAction} overrides this method.
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
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as String.
     *
     * @return content of the {@code config.xml} found in directory given by the request parameter {@code file}.
     * @throws IOException
     *             if the config file could not be read or converted to an xml string.
     */
    public final String getFile() throws IOException {
        checkConfigurePermission();
        final XmlFile xmlFile = getConfigXml(getRequestParameter("file"));
        return xmlFile.asString();
    }

    /**
     * Checks whether the type parameter of the current request equals {@code toCompare}.
     *
     * @param toCompare
     *            the string we want to compare.
     * @return true if {@code toCompare} equals request parameter type.
     */
    private boolean isTypeParameter(final String toCompare) {
        return getRequestParameter("type").equalsIgnoreCase(toCompare);
    }

    /**
     * Returns the configuration file (default is {@code config.xml}) located in {@code diffDir}. 
     * {@code diffDir} must either start with {@code HUDSON_HOME} and contain {@code config-history}
     * or be located under the configured {@code historyRootDir}.  It also must not contain a '..' pattern.
     * Otherwise an {@link IllegalArgumentException} will be thrown.
     * <p>This is to ensure that this plugin will not be abused to get arbitrary 
     * xml configuration files located anywhere on the system.
     *
     * @param diffDir
     *            timestamped history directory.
     * @return xmlfile.
     */
    protected XmlFile getConfigXml(final String diffDir) {
        final JobConfigHistory plugin = hudson.getPlugin(JobConfigHistory.class);
        final File configuredHistoryRootDir = plugin.getConfiguredHistoryRootDir();
        final String allowedHistoryRootDir = configuredHistoryRootDir == null
                ? getHudson().getRootDir().getAbsolutePath() : configuredHistoryRootDir.getAbsolutePath();
        File configFile = null;
        if (diffDir != null) {
            if (!diffDir.startsWith(allowedHistoryRootDir) || diffDir.contains("..")) {
                throw new IllegalArgumentException(diffDir + " does not start with "
                        + allowedHistoryRootDir + " or contains '..'");
            } else if (configuredHistoryRootDir == null && !diffDir.contains(JobConfigHistoryConsts.DEFAULT_HISTORY_DIR)) {
                throw new IllegalArgumentException(diffDir + " does not contain '"
                        + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR + "'");
            }
            configFile = plugin.getConfigFile(new File(diffDir));
        }
        if (configFile == null) {
            throw new IllegalArgumentException("Unable to get history from: " + diffDir);
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
     * See whether the current user may read configurations in the object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     */
    protected abstract  void checkConfigurePermission();
    /**
     * Returns whether the current user may read configurations in the object returned by
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
     * Parses the incoming {@code POST} request and redirects as {@code GET showDiffFiles}.
     *
     * @param req
     *            incoming request
     * @param rsp
     *            outgoing response
     * @throws ServletException
     *             when parsing the request as {@link MultipartFormDataParser} does not succeed.
     * @throws IOException
     *             when the redirection does not succeed.
     */
    public final void doDiffFiles(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        final MultipartFormDataParser parser = new MultipartFormDataParser(req);
        rsp.sendRedirect("showDiffFiles?histDir1=" + parser.get("histDir1") + "&histDir2=" + parser.get("histDir2"));

    }

    /**
     * Returns a textual diff between two {@code config.xml} files located in {@code histDir1} and {@code histDir2}
     * directories given as parameters of {@link Stapler#getCurrentRequest()}.
     *
     * @return diff
     * @throws IOException
     *             if reading one of the config files does not succeed.
     */
    public final String getDiffFile() throws IOException {
        checkConfigurePermission();
        final XmlFile configXml1 = getConfigXml(getRequestParameter("histDir1"));
        final String[] configXml1Lines = configXml1.asString().split("\\n");
        final XmlFile configXml2 = getConfigXml(getRequestParameter("histDir2"));
        final String[] configXml2Lines = configXml2.asString().split("\\n");
        return getDiff(configXml1.getFile(), configXml2.getFile(), configXml1Lines, configXml2Lines);
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
    protected final String getDiff(final File file1, final File file2, final String[] file1Lines, final String[] file2Lines) {
        final change change = new Diff(file1Lines, file2Lines).diff_2(false);
        final DiffPrint.UnifiedPrint unifiedPrint = new DiffPrint.UnifiedPrint(file1Lines, file2Lines);
        final StringWriter output = new StringWriter();
        unifiedPrint.setOutput(output);
        unifiedPrint.print_header(file1.getPath(), file2.getPath());
        unifiedPrint.print_script(change);
        return output.toString();
    }   
}
