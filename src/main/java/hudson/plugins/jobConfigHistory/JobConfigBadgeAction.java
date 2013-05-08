package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * This class adds a badge to the build history marking builds 
 * that occurred after the configuration was changed.
 * 
 * @author kstutz
 */
@Extension
public class JobConfigBadgeAction extends RunListener<AbstractBuild> implements BuildBadgeAction {

    /**The logger.*/
    private static final Logger LOG = Logger.getLogger(JobConfigBadgeAction.class.getName());
    
    /**The dates of the last two config changes as Strings.*/
    private String[] configDates;
    
    /**We need the build in order to get the project name.*/
    private AbstractBuild build;

    /**No arguments about a no-argument constructor (necessary because of annotation).*/
    public JobConfigBadgeAction() { }
    
    /**
     * Creates a new JobConfigBadgeAction.
     * @param configDates The dates of the last two config changes
     * @param build The respective build
     */
    public JobConfigBadgeAction(String[] configDates, AbstractBuild build) {
        super(AbstractBuild.class);
        this.configDates = configDates.clone();
        this.build = build;
    }

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
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = Hudson.getInstance().getPlugin(JobConfigHistory.class).getHistoryDir(project.getConfigFile());
        if (historyRootDir.exists()) {
            try {
                for (final File historyDir : historyRootDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(project, historyDir, histDescr);
                    configs.add(config);
                }
            } catch (IOException ex) {
                LOG.finest("Could not parse history files: " + ex);
            }
        }
        
        if (configs.size() > 1) {
            Collections.sort(configs, ConfigInfoComparator.INSTANCE);
            final ConfigInfo lastChange = Collections.min(configs, ConfigInfoComparator.INSTANCE);
            final Date lastConfigChange = parseDate(lastChange);

            if (lastBuildDate != null && lastConfigChange.after(lastBuildDate)) {
                final String[] dates = {lastChange.getDate(), findLastRelevantConfigChangeDate(configs, lastBuildDate)};
                build.addAction(new JobConfigBadgeAction(dates, build));
            }
        }

        super.onStarted(build, listener);
    }
    
    /**
     * Finds the date of the last config change that happened before the last build.
     * This is needed for the link in the build history that shows the difference between the current
     * configuration and the version that was in place when the last build happened. 
     * 
     * @param configs An ArrayList full of ConfigInfos.
     * @param lastBuildDate The date of the lastBuild (as Date).
     * @return The date of the last relevant config change (as String).
     */
    private String findLastRelevantConfigChangeDate(ArrayList<ConfigInfo> configs, Date lastBuildDate) {
        for (int i = 1; i < configs.size(); i++) {
            final ConfigInfo oldConfigChange = configs.get(i);
            final Date changeDate = parseDate(oldConfigChange);
            if (changeDate != null && changeDate.before(lastBuildDate)) {
                return oldConfigChange.getDate();
            }
        }
        return configs.get(1).getDate();
    }
    
    /**
     * Parses the date from a config info into a java.util.Date.
     * 
     * @param config A ConfigInfo.
     * @return The parsed date as a java.util.Date.
     */
    private Date parseDate(ConfigInfo config) {
        Date date = null;
        try {
            date = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).parse(config.getDate());
        } catch (ParseException ex) {
            LOG.finest("Could not parse Date: " + ex);
        }
        return date;
    }
    
    /**
     * Returns true if the config change build badges should appear
     * (depending on plugin settings and user permissions).
     * Called from badge.jelly.
     * 
     * @return True if badges should appear.
     */
    public boolean showBadge() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class).showBuildBadges(build.getProject());
    }
    
    /**
     * Check if the config history files that are attached to the build still exist.
     * 
     * @return True if both files exist.
     */
    public boolean oldConfigsExist() {
        final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);
        
        for (String timestamp : configDates) {
            final String path = plugin.getJobHistoryRootDir() + "/" + build.getProject().getFullName().replace("/", "/jobs/") + "/" + timestamp;
            final File historyDir = new File(path);
            if (!historyDir.exists() || !new File(historyDir, "config.xml").exists()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creates the target for the link to the showDiffFiles page.
     * @return Link target as String.
     */
    public String createLink() {
        return Hudson.getInstance().getRootUrl() + build.getProject().getUrl()
                + JobConfigHistoryConsts.URLNAME + "/showDiffFiles?timestamp1=" + configDates[1]
                + "&timestamp2=" + configDates[0];
    }
    
    /**
     * Returns tooltip so users know what our nice little icon stands for.
     * @return Explanatory text as string
     */
    public String getTooltip() {
        return Messages.JobConfigBadgeAction_ToolTip();
    }

    /**
     * Returns the path to our nice little icon.
     * @return Icon path as string
     */
    public String getIcon() {
        return "/plugin/jobConfigHistory/img/buildbadge.png";
    }

    /**
     * Non-use interface method.
     * {@inheritDoc}
     */
    public String getIconFileName() {
        return null;
    }

    /**
     * Non-use interface method.
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return null;
    }

    /**
     * Non-use interface method.
     * {@inheritDoc}
     */
    public String getUrlName() {
        return "";
    }
}
