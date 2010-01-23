package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.RootAction;
import hudson.util.RunList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * @author Stefan Brausch, mfriedenhagen
 */

@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction
        implements RootAction {

    /** Our logger. */
    private static final Logger LOG = Logger
            .getLogger(JobConfigHistoryRootAction.class.getName());

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly, so prefix
     * {@link JsConsts} with a slash.
     */
    @Override
    public String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Returns some or all known runs of this hudson instance, depending on
     * parameter count.
     *
     * @param request
     *            evalutes parameter <tt>count</tt>
     * @return runlist
     */
    public RunList getRunList(StaplerRequest request) {
        final RunList allRuns = new RunList(Hudson.getInstance()
                .getPrimaryView());
        final String countParameter = request.getParameter("count");
        if (countParameter == null) {
            return allRuns;
        } else {
            final int count = Integer.valueOf(countParameter);
            if (count > allRuns.size()) {
                return allRuns;
            } else {
                final RunList runList = new RunList();
                for (int i = 0; i < count; i++) {
                    runList.add(allRuns.get(i));
                }
                return runList;
            }
        }
    }


    @Exported
    public List<ConfigInfo> getConfigs() {
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        @SuppressWarnings("unchecked")
        final List<Project> projects = Hudson.getInstance().getProjects();
        for (@SuppressWarnings("unchecked") final Project project : projects) {
            LOG.finest("getConfigs: Getting configs for " + project.getName());
            final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(project);
            final List<ConfigInfo> jobConfigs = action.getConfigs();
            LOG.finest("getConfigs: " + project.getName() + " has " + jobConfigs.size() + " history items");
            configs.addAll(jobConfigs);
        }
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }

    /**
     * See {@link JobConfigHistoryBaseAction#getConfigFileContent()}.
     *
     * @return content of the file.
     */
    @Exported
    public String getFile() {
        return getConfigFileContent();
    }

    @Exported
    public String getType() {
        return Stapler.getCurrentRequest().getParameter("type");
    }

}
