/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

import static hudson.init.InitMilestone.COMPLETED;
import static java.util.logging.Level.FINEST;

/**
 * Saves the job configuration at
 * {@link SaveableListener#onChange(Saveable, XmlFile)}.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistorySaveableListener extends SaveableListener {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger
            .getLogger(JobConfigHistorySaveableListener.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        final JobConfigHistory plugin = getPlugin();
        LOG.log(FINEST, "In onChange for {0}", o);
        if (plugin.isSaveable(o, file) && !PluginUtils.isUserExcluded(plugin)) {
            final HistoryDao configHistoryListenerHelper = getHistoryDao(
                    plugin);
            configHistoryListenerHelper.saveItem(file);
        }
        LOG.log(FINEST, "onChange for {0} done.", o);
    }

    /**
     * For tests only.
     *
     * @return plugin
     */
    @NonNull JobConfigHistory getPlugin() {
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
     *
     * @return helper
     */
    HistoryDao getHistoryDao(JobConfigHistory plugin) {
        return (COMPLETED == Jenkins.get().getInitLevel())
                ? PluginUtils.getHistoryDao(plugin)
                : PluginUtils.getSystemHistoryDao(plugin);
    }
}
