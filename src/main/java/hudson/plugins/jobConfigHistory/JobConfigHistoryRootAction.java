package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.RootAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Stefan Brausch, mfriedenhagen
 */

@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction implements RootAction {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryRootAction.class.getName());

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly, so prefix {@link JobConfigHistoryConsts#URLNAME} with a
     * slash.
     */
    @Override
    public final String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Returns the configuration history entries for all {@link AbstractProject}s in this Hudson instance.
     *
     * @return list for all {@link AbstractProject}s.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        @SuppressWarnings("unchecked")
        final List<AbstractProject> projects = Hudson.getInstance().getItems(AbstractProject.class);
        for (final AbstractProject<?, ?> project : projects) {
            LOG.finest("getConfigs: Getting configs for " + project.getName());
            final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(project);
            final List<ConfigInfo> jobConfigs = action.getConfigs();
            LOG.finest("getConfigs: " + project.getName() + " has " + jobConfigs.size() + " history items");
            configs.addAll(jobConfigs);
        }
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }

}
