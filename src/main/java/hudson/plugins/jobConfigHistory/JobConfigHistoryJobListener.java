package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.util.logging.Logger;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public final class JobConfigHistoryJobListener extends ItemListener {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryJobListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onCreated(Item item) {
        LOG.finest("In onCreated for " + item);
        if (item instanceof AbstractProject<?, ?>) {
            ConfigHistoryListenerHelper.CREATED.createNewHistoryEntry((AbstractProject<?, ?>) item);
        }
        LOG.finest("onCreated for " + item + " done.");
    }

    /** {@inheritDoc} */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        LOG.finest("In onRenamed for " + item + " oldName=" + oldName + ", newName=" + newName);
        if (item instanceof AbstractProject<?, ?>) {
            ConfigHistoryListenerHelper.RENAMED.createNewHistoryEntry((AbstractProject<?, ?>) item);
        }
        LOG.finest("onRename for " + item + " done.");
    }
}
