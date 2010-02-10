package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import bmsi.util.Diff;
import bmsi.util.DiffPrint;
import bmsi.util.Diff.change;

/**
 * @author Stefan Brausch
 */
public class JobConfigHistoryProjectAction extends JobConfigHistoryBaseAction {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryProjectAction.class.getName());

    /**
     * @param project
     *            for which configurations should be returned.
     */
    public JobConfigHistoryProjectAction(AbstractProject<?, ?> project) {
        super();
        this.project = project;
    }

    /** The project. */
    private final transient AbstractProject<?, ?> project;

    /**
     * Returns the configuration history entries for one {@link AbstractProject}.
     *
     * @return history list for one {@link AbstractProject}.
     * @throws IOException
     *             if {@code history.xml} might not be read or the path might not be urlencoded.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = new File(project.getRootDir(), "config-history");
        if (!historyRootDir.isDirectory()) {
            LOG.info(historyRootDir + " is not a directory, assuming that no history exists yet.");
            return Collections.emptyList();
        }
        for (final File historyDir : historyRootDir.listFiles()) {
            final XmlFile historyXml = new XmlFile(new File(historyDir, "history.xml"));
            final HistoryDescr histDescr;
            histDescr = (HistoryDescr) historyXml.read();
            final ConfigInfo config = new ConfigInfo(project, historyDir, histDescr);
            configs.add(config);
        }
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }

    /**
     * Returns the project for which we want to see the config history, the config files or the diff.
     *
     * @return project
     */
    public final AbstractProject<?, ?> getProject() {
        return project;
    }

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
    String getDiff(final File file1, final File file2, final String[] file1Lines, final String[] file2Lines) {
        final change change = new Diff(file1Lines, file2Lines).diff_2(false);
        final DiffPrint.UnifiedPrint unifiedPrint = new DiffPrint.UnifiedPrint(file1Lines, file2Lines);
        final StringWriter output = new StringWriter();
        unifiedPrint.setOutput(output);
        unifiedPrint.print_header(file1.getPath(), file2.getPath());
        unifiedPrint.print_script(change);
        return output.toString();
    }

    /**
     * {@inheritDoc} Returns the project.
     */
    @Override
    protected AccessControlled getAccessControlledObject() {
        return project;
    }

}
