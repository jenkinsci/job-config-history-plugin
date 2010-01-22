package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import java.util.logging.Logger;

/**
 * Saves the job configuration at {@link SaveableListener#onChange(Saveable, XmlFile)}.
 *
 * @author Stefan Brausch
 */
@Extension
public final class JobConfigHistorySaveableListener extends SaveableListener {

    /** {@inheritDoc} */
    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        try {
            ConfigHistoryListenerHelper.CHANGED.createNewHistoryEntry((Item) o);
        } catch (Exception e) {
            Logger.getLogger("Config History Exception: " + e.getMessage());
        }
        super.onChange(o, file);
    }
}
