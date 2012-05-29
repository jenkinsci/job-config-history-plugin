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
     * Returns the configuration history entries for all {@link AbstractItem}s and System files in this Hudson instance.
     *
     * @return list for all {@link AbstractItem}s.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        final String filter = getRequestParameter("filter");
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        // we don't display any project info if we are filtered (applies to system configuration only)
        if (filter == null) {
            @SuppressWarnings("unchecked")
            final List<AbstractItem> items = Hudson.getInstance().getAllItems(AbstractItem.class);
            for (final AbstractItem item : items) {
                LOG.finest("getConfigs: Getting configs for " + item.getFullName());
                final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(item);
                final List<ConfigInfo> jobConfigs = action.getConfigs();
                LOG.finest("getConfigs: " + item.getFullName() + " has " + jobConfigs.size() + " history items");
                configs.addAll(jobConfigs);
            }
        }
        final List<ConfigInfo> systemConfigs = getSystemConfigs(filter);
        LOG.finest("getSystemConfigs: has " + systemConfigs.size() + " history items");
        configs.addAll(systemConfigs);
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
    protected List<ConfigInfo> getSystemConfigs(final String filter) throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File systemHistoryRootDir = getPlugin().getSystemHistoryDir();
        if (!systemHistoryRootDir.isDirectory()) {
            LOG.fine(systemHistoryRootDir + " is not a directory, assuming that no history exists yet.");
        } else {
            for (final File systemConfigEntry : systemHistoryRootDir.listFiles()) {
                if (filter != null && !filter.equals(systemConfigEntry.getName())) {
                    continue;
                }
                for (final File historyDir : systemConfigEntry.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(systemConfigEntry.getName(), historyDir, histDescr);
                    configs.add(config);
                }
            }
        }
        return configs; 
    }

    /**
     *
     * @return true is the page is loaded with a filter applied
     */
    public boolean isFiltered() {
        return getRequestParameter("filter") != null;
    }

    /**
     *
     * @return the filter text used when loading the page, or null if not filtered
     */
    public String getFilter() {
        return getRequestParameter("filter");
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
