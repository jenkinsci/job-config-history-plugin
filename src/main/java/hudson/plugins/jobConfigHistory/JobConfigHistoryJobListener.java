package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractItem;
import hudson.model.listeners.ItemListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryJobListener extends ItemListener {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryJobListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onCreated(Item item) {
        LOG.log(FINEST, "In onCreated for {0}", item);
        if (item instanceof AbstractItem) {
            final HistoryDao configHistoryListenerHelper = getHistoryDao();
            configHistoryListenerHelper.createNewItem(((AbstractItem) item));
        } else {
            LOG.finest("onCreated: not an AbstractItem, skipping history save");
        }
        LOG.log(FINEST, "onCreated for {0} done.", item);
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }

    /** {@inheritDoc}
     *
     * <p>
     * Also checks if we have history stored under the old name.  If so, copies
     * all history to the folder for new name, and deletes the old history folder.
     */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        final String onRenameDesc = " old name: " + oldName + ", new name: " + newName;
        LOG.log(FINEST, "In onRenamed for {0}{1}", new Object[] {item, onRenameDesc});
        if (item instanceof AbstractItem) {
            final HistoryDao configHistoryListenerHelper = getHistoryDao();
            // Must do this after moving old history, in case a CHANGED was fired during the same second under the old name.            
            configHistoryListenerHelper.renameItem((AbstractItem) item, oldName, newName);
        }
        LOG.log(FINEST, "Completed onRename for {0} done.", item);
//        new Exception("STACKTRACE for double invocation").printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleted(Item item) {
        LOG.log(FINEST, "In onDeleted for {0}", item);
        if (item instanceof AbstractItem) {
            final JobConfigHistory plugin = getPlugin();
            final HistoryDao configHistoryListenerHelper = getHistoryDao();
            configHistoryListenerHelper.deleteItem((AbstractItem) item);
            final File currentHistoryDir = plugin.getHistoryDir(((AbstractItem) item).getConfigFile());

            final SimpleDateFormat buildDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
            final String timestamp = buildDateFormat.format(new Date());
            final String deletedHistoryName = item.getName() + JobConfigHistoryConsts.DELETED_MARKER + timestamp;
            final File deletedHistoryDir = new File(currentHistoryDir.getParentFile(), deletedHistoryName);

            if (!currentHistoryDir.renameTo(deletedHistoryDir)) {
                LOG.warning("unable to rename deleted history dir to: " + deletedHistoryDir);
            }
        }
        LOG.log(FINEST, "onDeleted for {0} done.", item);
    }

    /**
     * Returns the plugin for tests.
     *
     * @return plugin
     */
    JobConfigHistory getPlugin() {
        return PluginUtils.getPlugin();
    }

    /**
     * For tests.
     *
     * @return listener
     */

    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
}
