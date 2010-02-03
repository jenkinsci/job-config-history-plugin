package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

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
     * Returns the job for which the health report will be generated.
     *
     * @return job
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
        rsp.sendRedirect("showDiffFiles?histDir1=" + parser.get("histDir1") + "&histDir2="
                        + parser.get("histDir2"));

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
        final String[] x = getConfigXml(getRequestParameter("histDir1")).asString().split("\\n");
        final String[] y = getConfigXml(getRequestParameter("histDir2")).asString().split("\\n");
        return getDiff(x, y);
    }

    /**
     * Returns a textual diff between two string arrays.
     *
     * @see <a href="http://www.cs.princeton.edu/introcs/96optimization/Diff.java.html">Diff</a>
     *
     * @param x
     *            first array
     * @param y
     *            second array
     * @return diff
     */
    String getDiff(final String[] x, final String[] y) {
        final StringBuilder diff = new StringBuilder("\nDiffs:\n\n");
        if (x != null && y != null) {
            // number of lines of each file
            final int xLength = x.length;
            final int yLength = y.length;

            // opt[i][j] = length of LCS of x[i..M] and y[j..N]
            final int[][] opt = new int[xLength + 1][yLength + 1];

            // compute length of LCS and all subproblems via dynamic
            // programming
            for (int i = xLength - 1; i >= 0; i--) {
                for (int j = yLength - 1; j >= 0; j--) {
                    if (x[i].equals(y[j])) {
                        opt[i][j] = opt[i + 1][j + 1] + 1;
                    } else {
                        opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
                    }
                }
            }

            // recover LCS itself and print out non-matching lines to
            // standard
            // output
            int i = 0, j = 0;
            while (i < xLength && j < yLength) {
                if (x[i].equals(y[j])) {
                    i++;
                    j++;
                } else if (opt[i + 1][j] >= opt[i][j + 1]) {
                    diff.append("< " + x[i++] + "\n");
                } else {
                    diff.append("> " + y[j++] + "\n");
                }
            }

            // dump out one remainder of one string if the other is
            // exhausted
            while (i < xLength || j < yLength) {
                if (i == xLength) {
                    diff.append("> " + y[j++] + "\n");
                } else if (j == yLength) {
                    diff.append("< " + x[i++] + "\n");
                }
            }

        }
        return diff.toString();
    }

}
