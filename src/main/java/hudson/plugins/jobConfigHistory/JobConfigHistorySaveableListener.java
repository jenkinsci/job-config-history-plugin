package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Hudson;
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

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistorySaveableListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);

        LOG.finest("In onChange for " + o);
        if (plugin.isSaveable(o, file)) {
            ConfigHistoryListenerHelper.CHANGED.createNewHistoryEntry(file);
        }
        LOG.finest("onChange for " + o + " done.");
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }
}
