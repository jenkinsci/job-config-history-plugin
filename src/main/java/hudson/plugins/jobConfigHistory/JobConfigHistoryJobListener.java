package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.listeners.ItemListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
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
            final HistoryDao configHistoryListenerHelper = getConfigHistoryListenerHelper(
                    Messages.ConfigHistoryListenerHelper_CREATED());
            configHistoryListenerHelper.createNewHistoryEntry(((AbstractItem) item).getConfigFile());
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
            final JobConfigHistory plugin = getPlugin();

            // move history items from previous name, if the directory exists
            // only applies if using a custom root directory for saving history
            if (plugin.getConfiguredHistoryRootDir() != null) {
                final File currentHistoryDir = plugin.getHistoryDir(((AbstractItem) item).getConfigFile());
                final File historyParentDir = currentHistoryDir.getParentFile();
                final File oldHistoryDir = new File(historyParentDir, oldName);
                if (oldHistoryDir.exists()) {
                    final FilePath fp = new FilePath(oldHistoryDir);
                    // catch all exceptions so Hudson can continue with other rename tasks.
                    try {
                        fp.copyRecursiveTo(new FilePath(currentHistoryDir));
                        fp.deleteRecursive();
                        LOG.log(FINEST, "completed move of old history files on rename.{0}", onRenameDesc);
                    } catch (IOException e) {
                        final String ioExceptionStr = "unable to move old history on rename." + onRenameDesc;
                        LOG.log(Level.SEVERE, ioExceptionStr, e);
                    } catch (InterruptedException e) {
                        final String irExceptionStr = "interrupted while moving old history on rename." + onRenameDesc;
                        LOG.log(Level.WARNING, irExceptionStr, e);
                    }
                }
            }
            // Must do this after moving old history, in case a CHANGED was fired during the same second under the old name.
            final HistoryDao configHistoryListenerHelper = getConfigHistoryListenerHelper(
                    Messages.ConfigHistoryListenerHelper_RENAMED());
            configHistoryListenerHelper.createNewHistoryEntry(((AbstractItem) item).getConfigFile());
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
            final HistoryDao configHistoryListenerHelper = getConfigHistoryListenerHelper(
                    Messages.ConfigHistoryListenerHelper_DELETED());
            configHistoryListenerHelper.createNewHistoryEntry(((AbstractItem) item).getConfigFile());
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
     * @return plugin
     */
    JobConfigHistory getPlugin() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class);
    }

    /**
     * For tests.
     * @param operationName name of the operation.
     * @return listener
     */
    HistoryDao getConfigHistoryListenerHelper(final String operationName) {
        return new FileHistoryDao(operationName);
    }
}
