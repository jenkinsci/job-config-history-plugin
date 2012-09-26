package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.security.AccessControlled;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.security.Permission;

/**
 *
 * @author Stefan Brausch, mfriedenhagen
 */

@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction implements RootAction {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryRootAction.class.getName());

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
     * Returns the configuration history entries 
     * for either {@link AbstractItem}s or system changes or deleted jobs 
     * or all of the above.
     *
     * @return list for all {@link AbstractItem}s.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
     public final List<ConfigInfo> getConfigs() throws IOException {
        final String filter = getRequestParameter("filter");
        final List<ConfigInfo> configs;

        if ("jobs".equals(filter)) {
            configs = getJobConfigs();
        } else if ("deleted".equals(filter)) {
            configs = getDeletedJobs();
        } else if ("all".equals(filter)) {
            configs = getJobConfigs();
            configs.addAll(getSystemConfigs());
            configs.addAll(getDeletedJobs());
        } else {
            configs = getSystemConfigs();
        }
        return configs;
     }

     private final List<ConfigInfo> getJobConfigs() throws IOException {
         final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();

         final List<AbstractItem> items = Hudson.getInstance().getAllItems(AbstractItem.class);
         for (final AbstractItem item : items) {
             LOG.finest("getConfigs: Getting configs for " + item.getFullName());
             final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(item);
             final List<ConfigInfo> jobConfigs = action.getConfigs();
             LOG.finest("getConfigs: " + item.getFullName() + " has " + jobConfigs.size() + " history items");
             configs.addAll(jobConfigs);
         }
         Collections.sort(configs, ConfigInfoComparator.INSTANCE);
         return configs;
     }
     
     
    /**
     * Returns the configuration history entries for all System files in this Hudson instance.
     * @param filter
     *            name of the system configuration entity to show
     * @return list for all System configuration files.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    protected List<ConfigInfo> getSystemConfigs() throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File systemHistoryRootDir = getPlugin().getSystemHistoryDir();
        if (!systemHistoryRootDir.isDirectory()) {
            LOG.fine(systemHistoryRootDir + " is not a directory, assuming that no history exists yet.");
        } else {
            configs.addAll(getConfigInfos(systemHistoryRootDir));
        }
        return configs; 
    }
    
    
    public final List<ConfigInfo> getDeletedJobs() throws IOException {
        checkConfigurePermission();
        List<ConfigInfo> list = new ArrayList<ConfigInfo>();
        final File historyRootDir = getPlugin().getDeletedJobsDir();
        if (historyRootDir.isDirectory()) {
            list.addAll(getConfigInfos(historyRootDir));
        }
        return list;
    }
    
    private final List<ConfigInfo> getConfigInfos(File dir) throws IOException {
        List<ConfigInfo> list = new ArrayList<ConfigInfo>();
        for (final File folders : dir.listFiles()) {
            for (final File historyDir : folders.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                final ConfigInfo config = ConfigInfo.create(folders.getName(), historyDir, histDescr);
                list.add(config);
            }
        }
        return list;
    }
    
    
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
