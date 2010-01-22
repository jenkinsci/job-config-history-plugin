package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Saves the job configuration at {@link SaveableListener#onChange(Saveable, XmlFile)}.
 *
 * @author Stefan Brausch
 */
@Extension
public final class JobConfigHistorySaveableListener extends SaveableListener {

    private static final Logger LOG = Logger.getLogger(JobConfigHistorySaveableListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        LOG.finest("In onChange for " + o);
        if (o instanceof AbstractProject<?, ?>) {
            try {
                ConfigHistoryListenerHelper.CHANGED.createNewHistoryEntry((AbstractProject<?, ?>) o);
            } catch (IOException e) {
                throw new RuntimeException("Saving " + o + " did not succeed", e);
            }
        }
        LOG.finest("onChange for " + o + " done.");
    }
}
