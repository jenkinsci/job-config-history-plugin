package hudson.plugins.jobConfigHistory;

import static hudson.init.InitMilestone.COMPLETED;
import static java.util.logging.Level.FINEST;
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
public class JobConfigHistorySaveableListener extends SaveableListener {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistorySaveableListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        final JobConfigHistory plugin = getPlugin();
        LOG.log(FINEST, "In onChange for {0}", o);
        if (plugin.isSaveable(o, file)) {
            final HistoryDao configHistoryListenerHelper = getHistoryDao(plugin);
            configHistoryListenerHelper.saveItem(file);
        }
        LOG.log(FINEST, "onChange for {0} done.", o);
    }

    /**
     * For tests only.
     *
     * @return plugin
     */
    JobConfigHistory getPlugin() {
        return PluginUtils.getPlugin();
    }

    /**
     * For tests only.
     *
     * @return helper.
     */
    @Deprecated
    HistoryDao getHistoryDao() {
        return getHistoryDao(PluginUtils.getPlugin());
    }

    /**
     * Return the helper, making sure its anonymous while Jenkins is still
     * initializing.
     * @return helper
     */
    HistoryDao getHistoryDao(JobConfigHistory plugin) {
        return (COMPLETED == Hudson.getInstance().getInitLevel())
                ? PluginUtils.getHistoryDao(plugin)
                : PluginUtils.getAnonymousHistoryDao(plugin);
    }
}
