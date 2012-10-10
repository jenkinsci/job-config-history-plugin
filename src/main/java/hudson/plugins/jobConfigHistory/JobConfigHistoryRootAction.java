package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

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
     * @return list of configuration histories (as ConfigInfo)
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        final String filter = getRequestParameter("filter");
        final List<ConfigInfo> configs;

        if (filter == null){
            configs = getAllConfigs("system");
        } else if ("all".equals(filter)){
            configs = getAllConfigs("jobs");
            configs.addAll(getAllConfigs("system"));
            configs.addAll(getAllConfigs("deleted"));
        } else {
            configs = getAllConfigs(filter);
        }
        return configs;
    }

    
   /**
    * Returns the configuration history entries for either all system files
    * or jobs or deleted jobs in this Hudson instance.
    * @return List of config infos.
    * @throws IOException
    *             if one of the history entries might not be read.
    */
   protected List<ConfigInfo> getAllConfigs(String type) throws IOException {
       checkConfigurePermission();
       final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
       final File historyRootDir;
       if ("system".equals(type)) {
           historyRootDir = new File(getPlugin().getConfiguredHistoryRootDir(), JobConfigHistoryConsts.SYSTEM_HISTORY_DIR);
       } else {
           historyRootDir = new File(getPlugin().getConfiguredHistoryRootDir(), JobConfigHistoryConsts.JOBS_HISTORY_DIR);
       }

       if (!historyRootDir.isDirectory()) {
           LOG.fine(historyRootDir + " is not a directory, assuming that no history exists yet.");
       } else {
           final File[] itemDirs;
           if ("deleted".equals(type)){
               itemDirs = historyRootDir.listFiles(JobConfigHistory.DELETED_FILTER);
           } else {
               itemDirs = historyRootDir.listFiles();
           }
           for (final File itemDir : itemDirs) {
               for (final File historyDir : itemDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                   final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                   final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                   final ConfigInfo config;
                   if ("jobs".equals(type) && !itemDir.getName().contains(JobConfigHistoryConsts.DELETED_MARKER)){
                       config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, true);
                   } else {
                       config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, false);
                   }
                   if (!("deleted".equals(type) && !"Deleted".equals(config.getOperation()))){
                       configs.add(config);
                   }
               }
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

        for (final File itemDir : historyRootDir.listFiles()) {
            if (itemDir.getName().equals(name)){
                for (final File historyDir : itemDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config;
                    if ("jobs".equals(type)){
                        config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, true);
                    } else {
                        config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, false);
                    }
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
