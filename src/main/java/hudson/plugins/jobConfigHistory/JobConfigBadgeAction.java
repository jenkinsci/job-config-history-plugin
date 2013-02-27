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
        
        final AbstractProject<?, ?> project = (AbstractProject<?, ?>) build.getProject();
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
            final ConfigInfo penultimateChange = configs.get(1);
            
            try {
                final Date lastConfigChange = new SimpleDateFormat(
                        JobConfigHistoryConsts.ID_FORMATTER).parse(lastChange.getDate());
                if (lastBuildDate != null && lastConfigChange.after(lastBuildDate)) {
                    final String[] dates = {lastChange.getDate(), penultimateChange.getDate()};
                    build.addAction(new JobConfigBadgeAction(dates, build));
                }
            } catch (ParseException ex) {
                LOG.finest("Could not parse Date: " + ex);
            }
        }

        super.onStarted(build, listener);
    }
    
    private void findNextChangeDate(ArrayList<ConfigInfo> configs, Date lastBuildDate) {
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        final ConfigInfo lastChange = Collections.min(configs, ConfigInfoComparator.INSTANCE);
        final Date lastConfigChange = parseDate(lastChange);

        if (lastBuildDate != null && lastConfigChange.after(lastBuildDate)) {
            ConfigInfo olderConfigChange = configs.get(1);
            for (int i=2; i<configs.size(); i++) {
                Date olderChangeDate = parseDate(configs.get(i));
                if (olderChangeDate != null && olderChangeDate.after(lastBuildDate)) {
                    olderConfigChange = configs.get(i);
                } else {
                    break;
                }
            }

            final String[] dates = {lastChange.getDate(), olderConfigChange.getDate()};
            build.addAction(new JobConfigBadgeAction(dates, build));
        }
    }
    
    private Date parseDate (ConfigInfo config) {
        Date date = null;
        try {
            date = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).parse(config.getDate());
        } catch (ParseException ex) {
            LOG.finest("Could not parse Date: " + ex);
        }
        return date;
    }
    
    public boolean showBadge() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class).showBuildBadges(build.getProject());
    }
    
    /**
     * Creates the target for the link to the showDiffFiles page.
     * @return Link target as String.
     */
    public String createLink() {
        return Hudson.getInstance().getRootUrl() + "job/" + build.getProject().getName() + "/"
                + JobConfigHistoryConsts.URLNAME + "/showDiffFiles?timestamp1=" + configDates[1]
                + "&timestamp2=" + configDates[0] + "&name=" + build.getProject().getName() + "&isJob=true";
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
