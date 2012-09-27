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


    /*
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
