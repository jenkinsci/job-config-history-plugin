package hudson.plugins.jobConfigHistory;

import hudson.Extension;
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

    public JobConfigHistoryJobListener() {
        super();
    }

    @Override
    public void onCreated(Item item) {
        try {
            ConfigHistoryListenerHelper.CREATED.createNewHistoryEntry(item);
        } catch (Exception e) {
            Logger.getLogger("Config History Exception: " + e.getMessage());
        }

    }

    /** {@inheritDoc} */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        try {
            ConfigHistoryListenerHelper.RENAMED.createNewHistoryEntry(item);
        } catch (Exception e) {
            Logger.getLogger("Config History Exception: " + e.getMessage());
        }
    }
}
