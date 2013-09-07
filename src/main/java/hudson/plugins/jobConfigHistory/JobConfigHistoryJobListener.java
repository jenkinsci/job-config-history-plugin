package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractItem;
import hudson.model.listeners.ItemListener;

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
            getHistoryDao().createNewItem(((AbstractItem) item));
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
            getHistoryDao().renameItem((AbstractItem) item, oldName, newName);
        }
        LOG.log(FINEST, "Completed onRename for {0} done.", item);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleted(Item item) {
        LOG.log(FINEST, "In onDeleted for {0}", item);
        if (item instanceof AbstractItem) {
            getHistoryDao().deleteItem((AbstractItem) item);
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
