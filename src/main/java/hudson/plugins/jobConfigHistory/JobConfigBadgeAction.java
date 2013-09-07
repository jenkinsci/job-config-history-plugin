package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.SortedMap;
import jenkins.model.RunAction2;

/**
 * This class adds a badge to the build history marking builds that occurred after the configuration was changed.
 *
 * @author kstutz
 */
public final class JobConfigBadgeAction implements BuildBadgeAction, RunAction2 {

    /**
     * The dates of the last two config changes as Strings.
     */
    private String[] configDates;

    /**
     * We need the build in order to get the project name.
     */
    private transient AbstractBuild build;

    /**
     * Creates a new JobConfigBadgeAction.
     *
     * @param configDates The dates of the last two config changes
     */
    private JobConfigBadgeAction(String[] configDates) {
        this.configDates = configDates.clone();
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        build = (AbstractBuild) r;

    }

    @Override
    public void onLoad(Run<?, ?> r) {
        build = (AbstractBuild) r;
    }

    /**
     * Listener.
     */
    @Extension
    public static final class Listener extends RunListener<AbstractBuild> {

        @Override
        public void onStarted(AbstractBuild build, TaskListener listener) {
            final AbstractProject<?, ?> project = build.getProject();
            if (project.getNextBuildNumber() <= 2) {
                super.onStarted(build, listener);
                return;
            }

            Date lastBuildDate = null;
            if (project.getLastBuild().getPreviousBuild() != null) {
                lastBuildDate = project.getLastBuild().getPreviousBuild().getTime();
            }

            //get timestamp of config-change
            final HistoryDao historyDao = getHistoryDao();
            final ArrayList<HistoryDescr> historyDescriptions = new ArrayList<HistoryDescr>(
                    historyDao.getRevisions(project).values());
            if (historyDescriptions.size() > 1) {
                Collections.sort(historyDescriptions, ParsedDateComparator.INSTANCE);
                final HistoryDescr lastChange = Collections.min(historyDescriptions, ParsedDateComparator.INSTANCE);
                final Date lastConfigChange = lastChange.parsedDate();

                if (lastBuildDate != null && lastConfigChange.after(lastBuildDate)) {
                    final String[] dates = {lastChange.getTimestamp(),
                        findLastRelevantConfigChangeDate(historyDescriptions, lastBuildDate)};
                    build.addAction(new JobConfigBadgeAction(dates));
                }
            }

            super.onStarted(build, listener);
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
        private String findLastRelevantConfigChangeDate(ArrayList<HistoryDescr> historyDescriptions, Date lastBuildDate) {
            for (int i = 1; i < historyDescriptions.size(); i++) {
                final HistoryDescr oldConfigChange = historyDescriptions.get(i);
                final Date changeDate = oldConfigChange.parsedDate();
                if (changeDate != null && changeDate.before(lastBuildDate)) {
                    return oldConfigChange.getTimestamp();
                }
            }
            return historyDescriptions.get(1).getTimestamp();
        }

        /**
         * For tests.
         *
         * @return listener
         */

        HistoryDao getHistoryDao() {
            return PluginUtils.getHistoryDao();
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
        for (String timestamp : configDates) {
            final XmlFile oldRevision = historyDao.getOldRevision(build.getProject(), timestamp);
            if (!oldRevision.getFile().exists()) {
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
        return Hudson.getInstance().getRootUrl() + build.getProject().getUrl()
                + JobConfigHistoryConsts.URLNAME + "/showDiffFiles?timestamp1=" + configDates[1]
                + "&timestamp2=" + configDates[0];
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
