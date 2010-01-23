package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

/**
 * @author Stefan Brausch
 */

public class JobConfigHistoryProjectAction extends JobConfigHistoryBaseAction {

    public JobConfigHistoryProjectAction(AbstractProject<?, ?> project) {
        super();
        this.project = project;
    }

    /** The project. */
    private final transient AbstractProject<?, ?> project;

    @Exported
    public List<ConfigInfo> getConfigs() {

        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File dirList = new File(project.getRootDir(), "config-history");
        final File[] configDirs = dirList.listFiles();
        for (final File configDir : configDirs) {
            final XmlFile myConfig = new XmlFile(new File(configDir, "history.xml"));
            final HistoryDescr histDescr;
            try {
                histDescr = (HistoryDescr) myConfig.read();
            } catch (IOException e) {
                throw new RuntimeException("Error reading " + myConfig, e);
            }
            ConfigInfo config = new ConfigInfo();
            config.setDate(histDescr.getTimestamp());
            config.setUser(histDescr.getUser());
            config.setOperation(histDescr.getOperation());
            config.setFile(configDir.getAbsolutePath());
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
    public AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * See {@link JobConfigHistoryBaseAction#getConfigFileContent()}.
     *
     * @return content of the file.
     */
    @Exported
    public String getFile() {
        return getConfigFileContent();
    }

    @Exported
    public String getType() {
        return Stapler.getCurrentRequest().getParameter("type");
    }

    public void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {

        MultipartFormDataParser parser = new MultipartFormDataParser(req);
        rsp.sendRedirect("showDiffFiles?diffFile1=" + parser.get("DiffFile1")
                + "&diffFile2=" + parser.get("DiffFile2"));

    }

    @Exported
    public String getDiffFile() {

        final String diffFile1 = Stapler.getCurrentRequest()
                .getParameter("diffFile1");
        final String diffFile2 = Stapler.getCurrentRequest()
                .getParameter("diffFile2");
        final StringBuilder diff = new StringBuilder("\nDiffs:\n\n");
        final XmlFile myConfig1 = new XmlFile(new File(diffFile1, "config.xml"));
        final XmlFile myConfig2 = new XmlFile(new File(diffFile2, "config.xml"));
        assert myConfig1.exists();
        assert myConfig2.exists();

        // http://www.cs.princeton.edu/introcs/96optimization/Diff.java.html

        final String[] x;
        final String[] y;
        try {
            x = myConfig1.asString().split("\\n");
            y = myConfig2.asString().split("\\n");
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + diffFile1 + " or " + diffFile2, e);
        }

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
                    if (x[i].equals(y[j]))
                        opt[i][j] = opt[i + 1][j + 1] + 1;
                    else
                        opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
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
