package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.security.AccessControlled;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.stream.StreamSource;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Stefan Brausch
 */
public class JobConfigHistoryProjectAction extends JobConfigHistoryBaseAction {

    /** Our logger. */
//  private static final Logger LOG = Logger.getLogger(JobConfigHistoryProjectAction.class.getName());

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
    public final List<ConfigInfo> getJobConfigs() throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = getPlugin().getHistoryDir(project.getConfigFile());
        if (historyRootDir.exists()) {
            for (final File historyDir : historyRootDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                final ConfigInfo config = ConfigInfo.create(project, historyDir, histDescr);
                configs.add(config);
            }
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
    
    /**
     * Action when 'restore' button is pressed.
     * @param req incoming StaplerRequest
     * @param rsp outgoing StaplerResponse
     * @throws IOException if something goes wrong
     */
    public final void doRestore(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        checkConfigurePermission();
        
        final XmlFile xmlFile = getConfigXml(req.getParameter("file"));
        final String oldConfig = xmlFile.asString();
        final InputStream is = new ByteArrayInputStream(oldConfig.getBytes("UTF-8"));

        project.updateByXml(new StreamSource(is));
        project.save();
        rsp.sendRedirect(Hudson.getInstance().getRootUrl() + project.getUrl());
    }
    
    /**
     * Action when 'restore' button in showDiffFiles.jelly is pressed.
     * Gets required parameters and forwards to restoreQuestion.jelly.
     * @param req StaplerRequest created by pressing the button
     * @param rsp outgoing StaplerResponse
     * @throws IOException If XML file can't be read
     */
    public final void doForwardToRestoreQuestion(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        final String histDir = req.getParameter("histDir");
        final XmlFile historyXml = new XmlFile(new File(histDir, JobConfigHistoryConsts.HISTORY_FILE));
        final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
        rsp.sendRedirect("restoreQuestion?file=" + historyXml.getFile().getParent() 
                + "&date=" + histDescr.getTimestamp() + "&user=" + histDescr.getUser());
    }
}
