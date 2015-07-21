package hudson.plugins.jobConfigHistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.List;
import jenkins.model.RunAction2;

/**
 * This class adds a badge to the build history marking builds that occurred after the configuration was changed.
 *
 * @author kstutz
 */
public class JobConfigBadgeAction implements BuildBadgeAction, RunAction2 {

    /**
     * The dates of the last two config changes as Strings.
     */
    private String[] configDates;

    /**
     * We need the build in order to get the project name.
     */
    private transient AbstractBuild<?, ?> build;
    
    /**
     * A transient field, which allows to workaround the JENKINS-20511 issue 
     * (missing targetType field).
     * @deprecated This field is not required in the config-history logic
     * @see https://issues.jenkins-ci.org/browse/JENKINS-20511
     */
    private transient String targetType;

    /**
     * Creates a new JobConfigBadgeAction.
     *
     * @param configDates The dates of the last two config changes
     */
    JobConfigBadgeAction(String[] configDates) {
        this.configDates = configDates.clone();
        this.build = null;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        build = (AbstractBuild<?, ?>) r;

    }

    @Override
    public void onLoad(Run<?, ?> r) {
        build = (AbstractBuild<?, ?>) r;
    }

    /**
     * Listener.
     */
    @Extension
    public static class Listener extends RunListener<AbstractBuild<?, ?>> {

        @Override
        public void onStarted(AbstractBuild<?, ?> build, TaskListener listener) {
            final AbstractProject<?, ?> project = build.getProject();
            if (project.getNextBuildNumber() <= 2) {
                super.onStarted(build, listener);
                return;
            }

            Date lastBuildDate = null;
            final AbstractBuild lastBuild = project.getLastBuild();
            if (lastBuild != null && lastBuild.getPreviousBuild() != null) {
                lastBuildDate = lastBuild.getPreviousBuild().getTime();
            }
            final List<HistoryDescr> historyDescriptions = getRevisions(project);
            if (historyDescriptions.size() > 1) {
                Collections.sort(historyDescriptions, ParsedDateComparator.DESCENDING);
                final HistoryDescr lastChange = Collections.min(historyDescriptions, ParsedDateComparator.DESCENDING);
                final Date lastConfigChange = lastChange.parsedDate();

                if (lastBuildDate != null && lastConfigChange.after(lastBuildDate)) {
                    final String[] dates = {lastChange.getTimestamp(),
                        findLastRelevantConfigChangeDate(historyDescriptions, lastBuildDate), };
                    build.addAction(new JobConfigBadgeAction(dates));
                }
            }

            super.onStarted(build, listener);
        }

        /**
         * For tests.
         * @param project to inspect.
         * @return list of revisions
         */
        List<HistoryDescr> getRevisions(final AbstractProject<?, ?> project) {
            final HistoryDao historyDao = PluginUtils.getHistoryDao();
            final ArrayList<HistoryDescr> historyDescriptions = new ArrayList<HistoryDescr>(
                    historyDao.getRevisions(project.getConfigFile()).values());
            return historyDescriptions;
        }

        /**
         * Finds the date of the last config change that happened before the last build. This is needed for the link in the build
         * history that shows the difference between the current configuration and the version that was in place when the last
         * build happened.
         *
         * @param historyDescriptions An ArrayList full of HistoryDescr.
         * @param lastBuildDate The date of the lastBuild (as Date).
         * @return The date of the last relevant config change (as String).
         */
        private String findLastRelevantConfigChangeDate(List<HistoryDescr> historyDescriptions, Date lastBuildDate) {
            for (HistoryDescr oldConfigChange : historyDescriptions.subList(1, historyDescriptions.size())) {
                final Date changeDate = oldConfigChange.parsedDate();
                if (changeDate != null && changeDate.before(lastBuildDate)) {
                    return oldConfigChange.getTimestamp();
                }
            }
            return historyDescriptions.get(1).getTimestamp();
        }
    } // end Listener

    /**
     * Returns true if the config change build badges should appear (depending on plugin settings and user permissions). Called
     * from badge.jelly.
     *
     * @return True if badges should appear.
     */
    public boolean showBadge() {
        return getPlugin().showBuildBadges(build.getProject());
    }

    /**
     * Check if the config history files that are attached to the build still exist.
     *
     * @return True if both files exist.
     */
    public boolean oldConfigsExist() {
        final HistoryDao historyDao = getHistoryDao();
        final AbstractProject<?, ?> project = build.getProject();
        for (String timestamp : configDates) {
            if (!historyDao.hasOldRevision(project.getConfigFile(), timestamp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the target for the link to the showDiffFiles page.
     *
     * @return Link target as String.
     */
    public String createLink() {
        return getRootUrl() + build.getProject().getUrl()
                + JobConfigHistoryConsts.URLNAME + "/showDiffFiles?timestamp1=" + configDates[1]
                + "&timestamp2=" + configDates[0];
    }

    /**
     * For tests.
     *
     * @return root-URL of Jenkins.
     */
    String getRootUrl() {
        return Hudson.getInstance().getRootUrl();
    }

    /**
     * Returns tooltip so users know what our nice little icon stands for.
     *
     * @return Explanatory text as string
     */
    public String getTooltip() {
        return Messages.JobConfigBadgeAction_ToolTip();
    }

    /**
     * Returns the path to our nice little icon.
     *
     * @return Icon path as string
     */
    public String getIcon() {
        return "/plugin/jobConfigHistory/img/buildbadge.png";
    }

    /**
     * Non-use interface method. {@inheritDoc}
     */
    public String getIconFileName() {
        return null;
    }

    /**
     * Non-use interface method. {@inheritDoc}
     */
    public String getDisplayName() {
        return null;
    }

    /**
     * Non-use interface method. {@inheritDoc}
     */
    public String getUrlName() {
        return "";
    }
    /**
     * Returns the plugin for tests.
     *
     * @return plugin
     */
    JobConfigHistory getPlugin() {
        return PluginUtils.getPlugin();
    }

    /**
     * For tests.
     *
     * @return listener
     */

    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
}
