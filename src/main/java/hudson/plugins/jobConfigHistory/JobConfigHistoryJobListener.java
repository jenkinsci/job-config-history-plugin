package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractItem;
import hudson.model.listeners.ItemListener;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryJobListener extends ItemListener {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryJobListener.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreated(Item item) {
        LOG.log(FINEST, "In onCreated for {0}", item);
        switchHistoryDao(item).createNewItem((item));
        LOG.log(FINEST, "onCreated for {0} done.", item);
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Also checks if we have history stored under the old name. If so, copies all history to the folder for new name, and deletes
     * the old history folder.
     */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        final String onRenameDesc = " old name: " + oldName + ", new name: " + newName;
        LOG.log(FINEST, "In onRenamed for {0}{1}", new Object[]{item, onRenameDesc});
        switchHistoryDao(item).renameItem(item, oldName, newName);
        LOG.log(FINEST, "Completed onRename for {0} done.", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeleted(Item item) {
        LOG.log(FINEST, "In onDeleted for {0}", item);
        switchHistoryDao(item).deleteItem(item);
        LOG.log(FINEST, "onDeleted for {0} done.", item);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdated(Item item) {
        LOG.log(FINEST, "In onUpdate for {0}", item);
        switchHistoryDao(item).saveItem(item);
        LOG.log(FINEST, "onUpdate for {0} done.", item);
    }

    /**
     * Returns ItemListenerHistoryDao depending on the item type.
     *
     * @param item the item to switch on.
     * @return dao
     */
    private ItemListenerHistoryDao switchHistoryDao(Item item) {
        return item instanceof AbstractItem ? getHistoryDao() : NoOpItemListenerHistoryDao.INSTANCE;
    }

    /**
     * Just for tests.
     *
     * @return ItemListenerHistoryDao.
     */
    ItemListenerHistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

    /**
     * No operation ItemListenerHistoryDao.
     */
    private static class NoOpItemListenerHistoryDao implements ItemListenerHistoryDao {

        /**
         * The instance.
         */
        static final NoOpItemListenerHistoryDao INSTANCE = new NoOpItemListenerHistoryDao();

        @Override
        public void createNewItem(Item item) {
            LOG.log(Level.FINEST, "onCreated: not an AbstractItem {0}, skipping.", item);
        }

        @Override
        public void renameItem(Item item, String oldName, String newName) {
            LOG.log(Level.FINEST, "onRenamed: not an AbstractItem {0}, skipping.", item);
        }

        @Override
        public void deleteItem(Item item) {
            LOG.log(Level.FINEST, "onDeleted: not an AbstractItem {0}, skipping.", item);
        }
        
        @Override
        public void saveItem(Item item) {
            LOG.log(Level.FINEST, "onUpdate: not an AbstractItem {0}, skipping.", item);
        }

    }
}
