package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

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
    public JobConfigHistoryProjectAction(AbstractItem project) {
        super();
        this.project = project;
    }

    /** The project. */
    private final transient AbstractItem project;

    /**
     * Returns the configuration history entries for one {@link AbstractItem}.
     *
     * @return history list for one {@link AbstractItem}.
     * @throws IOException
     *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be read or the path might not be urlencoded.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = getPlugin().getHistoryDir(project.getConfigFile());
        if (!historyRootDir.isDirectory()) {
            LOG.info(historyRootDir + " is not a directory, assuming that no history exists yet.");
            return Collections.emptyList();
        }
        for (final File historyDir : historyRootDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
            final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
            final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
            final ConfigInfo config = ConfigInfo.create(project, historyDir, histDescr);
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
    public final AbstractItem getProject() {
        return project;
    }

    /**
     * {@inheritDoc} Returns the project.
     */
    @Override
    protected AccessControlled getAccessControlledObject() {
        return project;
    }

    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(AbstractProject.CONFIGURE);
    }

    @Override
    protected boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(AbstractProject.CONFIGURE);
    }
    
 
    public final void doRevertOrNot(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        checkConfigurePermission();
        final String bla = req.getParameter("file");
        final Writer writer = rsp.getCompressedWriter(req);
        try {
            writer.append("HUHU! " + bla);              
        } finally {
            writer.close();
        }
        
 //       String version = req.getParameter("version");
 //       rsp.sendRedirect("showRevertQuestion?version=" + version);
    }

    /* Tut ned!
    public final String getDate(StaplerRequest req){
        final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);

        File configFile = plugin.getConfigFile(new File(req.getParameter("file")));
        return configFile.getDate();
    }*/
    
}
