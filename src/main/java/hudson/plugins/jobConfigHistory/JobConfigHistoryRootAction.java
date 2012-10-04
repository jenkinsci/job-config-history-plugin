package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.security.AccessControlled;
import hudson.security.Permission;

/**
 *
 * @author Stefan Brausch, mfriedenhagen
 */

@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction implements RootAction {

    /** Our logger. */
//    private static final Logger LOG = Logger.getLogger(JobConfigHistoryRootAction.class.getName());

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly, so prefix {@link JobConfigHistoryConsts#URLNAME} with a
     * slash.
     */
    @Override
    public final String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }


    /**
     * Returns the configuration history entries for one group of system files.
     * @return Configs list for one group of system configuration files.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getSingleConfigs(StaplerRequest req) throws IOException {
        checkConfigurePermission();
        String name = req.getParameter("name");
        String type = req.getParameter("type");
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir;
        if ("system".equals(type)) {
            historyRootDir = new File(getPlugin().getConfiguredHistoryRootDir(), JobConfigHistoryConsts.SYSTEM_HISTORY_DIR);
        } else {
            historyRootDir = new File(getPlugin().getConfiguredHistoryRootDir(), JobConfigHistoryConsts.JOBS_HISTORY_DIR);
        }

        for (final File folder : historyRootDir.listFiles()) {
            if (folder.getName().equals(name)){
                for (final File historyDir : folder.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(folder.getName(), historyDir, histDescr);
                    configs.add(config);
                }
                return configs; 
            }
        }
        return configs; 
    }

    /**
     * Returns the configuration history entries for one group of system files.
     * @return Configs list for one group of system configuration files.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
/*    protected List<ConfigInfo> getSingleSystemConfigs(String name) throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File systemHistoryRootDir = new File(getPlugin().getConfiguredHistoryRootDir(), 
                JobConfigHistoryConsts.SYSTEM_HISTORY_DIR);
        for (final File folder : systemHistoryRootDir.listFiles()) {
            if (folder.getName().equals(name)){
                for (final File historyDir : folder.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(folder.getName(), historyDir, histDescr);
                    configs.add(config);
                }
                return configs; 
            }
        }
        return configs; 
    }
*/
    
    /**
     * Returns the configuration history entries for a single {@link AbstractItem}.
     *
     * @return Configs list for one {@link AbstractItem}.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
/*    protected List<ConfigInfo> getSingleJobConfigs(String name) throws IOException {
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final AbstractItem item = (AbstractItem)Hudson.getInstance().getItem(name);
        final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(item);
        final List<ConfigInfo> jobConfigs = projectAction.getJobConfigs();
        configs.addAll(jobConfigs);
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }
*/    
    /**
     * {@inheritDoc}
     *
     * Returns the hudson instance.
     */
    @Override
    protected AccessControlled getAccessControlledObject() {
        return getHudson();
    }
    
    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Permission.CONFIGURE);
    }

    @Override
    protected boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(Permission.CONFIGURE);
    }
}
