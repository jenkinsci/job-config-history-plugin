package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.RootAction;
import hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.MultipartFormDataParser;

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
     * {@inheritDoc}
     * 
     * Make method final, as we always want the same icon file. Returns
     * {@code null} to hide the icon if the user is not allowed to configure
     * jobs.
     */
    public final String getIconFileName() {
        if (hasConfigurePermission() || hasJobConfigurePermission()) {
            return JobConfigHistoryConsts.ICONFILENAME;
        } else {
            return null;
        }
    }

    /**
     * Returns the configuration history entries 
     * for either {@link AbstractItem}s or system changes or deleted jobs 
     * or all of the above.
     *
     * @return list of configuration histories (as ConfigInfo)
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getConfigs() throws IOException {
        final String filter = getRequestParameter("filter");
        List<ConfigInfo> configs = null;

        if (filter == null || "system".equals(filter)) {
            configs = getSystemConfigs();
        } else if ("all".equals(filter)) {
            configs = getJobConfigs("jobs");
            configs.addAll(getJobConfigs("deleted"));
            configs.addAll(getSystemConfigs());
        } else {
            configs = getJobConfigs(filter);
        }
        
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }

    /**
     * Returns the configuration history entries for all system files
     * in this Hudson instance.
     * 
     * @return List of config infos.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    protected List<ConfigInfo> getSystemConfigs() throws IOException {
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = getPlugin().getConfiguredHistoryRootDir();
        
        if (!hasConfigurePermission()) {
            return configs;
        }

        if (!historyRootDir.isDirectory()) {
            LOG.fine(historyRootDir + " is not a directory, assuming that no history exists yet.");
        } else {
            final File[] itemDirs = historyRootDir.listFiles();
            for (final File itemDir : itemDirs) {
                //skip the "jobs" directory since we're looking for system changes
                if (itemDir.getName().equals(JobConfigHistoryConsts.JOBS_HISTORY_DIR)) {
                    continue;
                }
                for (final File historyDir : itemDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, false);
                    configs.add(config);
                }
            }
        }
        return configs; 
    }

    /**
     * Returns the configuration history entries for all jobs 
     * or deleted jobs in this Hudson instance.
     * 
     * @param type Whether we want to see all jobs or just the deleted jobs.
     * @return List of config infos.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    protected List<ConfigInfo> getJobConfigs(String type) throws IOException {
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = getPlugin().getJobHistoryRootDir();
        
        if (!hasJobConfigurePermission()) {
            return configs;
        }

        if (!historyRootDir.isDirectory()) {
            LOG.fine(historyRootDir + " is not a directory, assuming that no history exists yet.");
        } else {
            final File[] itemDirs;
            if ("deleted".equals(type)) {
                itemDirs = historyRootDir.listFiles(JobConfigHistory.DELETED_FILTER);
            } else {
                itemDirs = historyRootDir.listFiles();
            }
            for (final File itemDir : itemDirs) {
                for (final File historyDir : itemDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config;
                    if ("jobs".equals(type) && !itemDir.getName().contains(JobConfigHistoryConsts.DELETED_MARKER)) {
                        config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, true);
                    } else {
                        config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, false);
                    }
                    if (!("deleted".equals(type) && !"Deleted".equals(config.getOperation()))) {
                        configs.add(config);
                    }
                }
            }
        }
        return configs; 
    }

    
    /**
     * Returns the configuration history entries for one group of system files.
     * 
     * @param req The incoming StaplerRequest
     * @return Configs list for one group of system configuration files.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getSingleConfigs(StaplerRequest req) throws IOException {
        final String name = req.getParameter("name");
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir;
        if (name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
            historyRootDir = getPlugin().getJobHistoryRootDir();
        } else {
            historyRootDir = getPlugin().getConfiguredHistoryRootDir();
        }

        for (final File itemDir : historyRootDir.listFiles()) {
            if (itemDir.getName().equals(name)) {
                for (final File historyDir : itemDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(itemDir.getName(), historyDir, histDescr, false);
                    configs.add(config);
                }
            }
        }
        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        return configs;
    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as
     * String.
     * 
     * @return content of the {@code config.xml} found in directory given by the
     *         request parameter {@code file}.
     * @throws IOException
     *             if the config file could not be read or converted to an xml
     *             string.
     */
    public final String getFile() throws IOException {
        checkConfigurePermission();
        final String name = getRequestParameter("name");
        final String timestamp = getRequestParameter("timestamp");
        final XmlFile xmlFile = getOldConfigXml(name, timestamp);
        return xmlFile.asString();
    }

    /**
     * Creates links to the correct configOutput.jellys for job history vs. system history
     * and for xml vs. plain text.
     * 
     * @param config ConfigInfo.
     * @param type Output type ('xml' or 'plain').
     * @return The link as String.
     */
    public final String createLinkToJobFiles(ConfigInfo config, String type) {
        final String link;
        if (config.getIsJob() && !config.getJob().contains(JobConfigHistoryConsts.DELETED_MARKER)) {
            link = getHudson().getRootUrl() + "job/" + config.getJob() + getUrlName() 
                    + "/configOutput?type=" + type + "&timestamp=" + config.getDate();
        } else {
            link = "configOutput?type=" + type + "&name=" + config.getJob() + "&timestamp=" + config.getDate();
        }
        return link;
    }
    
    /**
     * {@inheritDoc}
     *
     * Returns the hudson instance.
     */
    @Override
    protected AccessControlled getAccessControlledObject() {
        return getHudson();
    }
    
    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Permission.CONFIGURE);
    }

    @Override
    public boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(Permission.CONFIGURE);
    }
    
    /**
     * Returns whether the current user may configure jobs.
     * 
     * @return true if the current user may configure jobs.
     */
    public boolean hasJobConfigurePermission() {
        return getAccessControlledObject().hasPermission(Item.CONFIGURE);
    }
    
    /**
     * Parses the incoming {@code POST} request and redirects as
     * {@code GET showDiffFiles}.
     * 
     * @param req
     *            incoming request
     * @param rsp
     *            outgoing response
     * @throws ServletException
     *             when parsing the request as {@link MultipartFormDataParser}
     *             does not succeed.
     * @throws IOException
     *             when the redirection does not succeed.
     */
    public final void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
        throws ServletException, IOException {
        final MultipartFormDataParser parser = new MultipartFormDataParser(req);
        rsp.sendRedirect("showDiffFiles?name=" + parser.get("name") + "&timestamp1=" + parser.get("timestamp1")
                + "&timestamp2=" + parser.get("timestamp2"));
    }
    
    /**
     * Returns the diff between two config files as a list of single lines.
     * Takes the two timestamps and the name of the system property 
     * or the deleted job from the url parameters.
     * 
     * @return Differences between two config versions as list of lines.
     * @throws IOException If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines() throws IOException {
        checkConfigurePermission();
        final String name = getRequestParameter("name");
        final String timestamp1 = getRequestParameter("timestamp1");
        final String timestamp2 = getRequestParameter("timestamp2");

        final XmlFile configXml1 = getOldConfigXml(name, timestamp1);
        final String[] configXml1Lines = configXml1.asString().split("\\n");
        final XmlFile configXml2 = getOldConfigXml(name, timestamp2);
        final String[] configXml2Lines = configXml2.asString().split("\\n");
        
        final String diffAsString = getDiffAsString(configXml1.getFile(), configXml2.getFile(),
                configXml1Lines, configXml2Lines);
        
        final List<String> diffLines = Arrays.asList(diffAsString.split("\n"));
        return getDiffLines(diffLines);
    }
    
    /**
     * Gets the version of the config.xml that was saved at a certain time.
     * 
     * @param name The name of the system property or deleted job.
     * @param timestamp The timestamp as String.
     * @return The config file as XmlFile.
     */
    private XmlFile getOldConfigXml(String name, String timestamp) {
        final JobConfigHistory plugin = getPlugin();
        final String rootDir;
        File configFile = null;
        String path = null;

        if (checkParameters(name, timestamp)) {
            if (name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
                rootDir = plugin.getJobHistoryRootDir().getPath();
            } else {
                rootDir = plugin.getConfiguredHistoryRootDir().getPath();
                checkConfigurePermission();
            }
            path = rootDir + "/" + name + "/" + timestamp;
            configFile = plugin.getConfigFile(new File(path));
        }

        if (configFile == null) {
            throw new IllegalArgumentException("Unable to get history from: "
                    + path);
        } else {
            return new XmlFile(configFile);
        }
    }
}
