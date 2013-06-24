package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;
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

        LOG.log(FINEST, "In onChange for {0}", o);
        if (plugin.isSaveable(o, file)) {
            ConfigHistoryListenerHelper.CHANGED.createNewHistoryEntry(file);
        }
        LOG.log(FINEST, "onChange for {0} done.", o);
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }
}
