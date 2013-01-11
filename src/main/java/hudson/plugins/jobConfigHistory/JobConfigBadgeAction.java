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
    
    /**The project to which the build belongs.*/
    private AbstractProject<?, ?> project;

    /**No arguments about a no-argument constructor (necessary because of annotation).*/
    public JobConfigBadgeAction() { }
    
    /**
     * Creates a new JobConfigBadgeAction.
     * @param configDates The dates of the last two config changes
     * @param project The respective project
     */
    public JobConfigBadgeAction(String[] configDates, AbstractProject<?, ?> project) {
        super(AbstractBuild.class);
        this.configDates = configDates.clone();
        this.project = project;
    }

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        final AbstractProject<?, ?> job = (AbstractProject<?, ?>) build.getProject();
        if (job.getNextBuildNumber() == 2) {
            super.onStarted(build, listener);
            return;
        }
        final Date lastBuildDate = job.getLastBuild().getPreviousBuild().getTime();
        
        //get timestamp of config-change
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = Hudson.getInstance().getPlugin(JobConfigHistory.class).getHistoryDir(job.getConfigFile());
        if (historyRootDir.exists()) {
            try {
                for (final File historyDir : historyRootDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(job, historyDir, histDescr);
                    configs.add(config);
                }
            } catch (IOException ex) {
                LOG.finest("Could not parse history files: " + ex);
            }
        }

        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        final ConfigInfo lastChange = Collections.min(configs, ConfigInfoComparator.INSTANCE);
        final ConfigInfo penultimateChange = configs.get(1);
        
        try {
            final Date lastConfigChange = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).parse(lastChange.getDate());
            if (lastConfigChange.after(lastBuildDate)) {
                final String[] dates = {lastChange.getDate(), penultimateChange.getDate()};
                build.addAction(new JobConfigBadgeAction(dates, job));
            }
        } catch (ParseException e) {
            LOG.finest("Could not parse Date: " + e);
        }

        super.onStarted(build, listener);
    }
    
    /**
     * Creates the target for the link to the showDiffFiles page.
     * @return link target as string
     */
    public String createLink() {
        return Hudson.getInstance().getRootUrl() + "job/" + project.getName() + "/"
                + JobConfigHistoryConsts.URLNAME + "/showDiffFiles?timestamp1=" + configDates[1]
                + "&timestamp2=" + configDates[0] + "&name=" + project.getName() + "&isJob=true";
    }
    
    /**
     * Returns tooltip so users know what our nice little icon stands for.
     * @return Explanatory text as string
     */
    public String getTooltip() {
        return "Config changed since last build.";
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
