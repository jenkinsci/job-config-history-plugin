package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public final class JobConfigHistoryJobListener extends ItemListener {

    private static final Logger LOG = Logger.getLogger(JobConfigHistoryJobListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onCreated(Item item) {
        LOG.finest("In onCreated for " + item);
        if (item instanceof AbstractProject<?, ?>) {
            try {
                ConfigHistoryListenerHelper.CREATED.createNewHistoryEntry((AbstractProject<?, ?>) item);
            } catch (IOException e) {
                throw new RuntimeException("Saving creation of " + item + " did not succeed", e);
            }
        }
        LOG.finest("onCreated for " + item + " done.");
    }

    /** {@inheritDoc} */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        LOG.finest("In onRenamed for " + item + " oldName=" + oldName + ", newName=" + newName);
        if (item instanceof AbstractProject<?, ?>) {
            try {
                ConfigHistoryListenerHelper.RENAMED.createNewHistoryEntry((AbstractProject<?, ?>) item);
            } catch (IOException e) {
                throw new RuntimeException("Rrenaming of " + item + " from " + oldName + " to " + newName
                        + " did not succeed", e);
            }
        }
        LOG.finest("onRename for " + item + " done.");
    }
}
