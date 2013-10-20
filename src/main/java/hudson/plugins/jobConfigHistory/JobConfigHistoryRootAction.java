package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import hudson.model.AbstractProject;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.MultipartFormDataParser;
import java.util.Collection;

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
     * Constructor necessary for testing.
     */
    public JobConfigHistoryRootAction() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly, so prefix
     * {@link JobConfigHistoryConsts#URLNAME} with a slash.
     */
    @Override
    public final String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }

    /**
     * {@inheritDoc}
     *
     * Make method final, as we always want the same icon file. Returns
     * {@literal null} to hide the icon if the user is not allowed to configure
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
     * Returns the configuration history entries for either {@link AbstractItem}
     * s or system changes or deleted jobs or all of the above.
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

        Collections.sort(configs, ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Returns the configuration history entries for all system files in this
     * Hudson instance.
     *
     * @return List of config infos.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    protected List<ConfigInfo> getSystemConfigs() throws IOException {
        final List<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        if (!hasConfigurePermission()) {
            return configs;
        }

        final File[] itemDirs = getOverviewHistoryDao().getSystemConfigs();
        for (final File itemDir : itemDirs) {
            final String itemName = itemDir.getName();
            configs.addAll(
                    HistoryDescrToConfigInfo.convert(
                            itemName, true, getOverviewHistoryDao().getSystemHistory(itemName).values(), false));

        }
        return configs;
    }

    /**
     * Returns the configuration history entries for all jobs or deleted jobs in
     * this Hudson instance.
     *
     * @param type
     *            Whether we want to see all jobs or just the deleted jobs.
     * @return List of config infos.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    protected List<ConfigInfo> getJobConfigs(String type) throws IOException {
        if (!hasJobConfigurePermission()) {
            return Collections.EMPTY_LIST;
        } else {
            return new ConfigInfoCollector(type, getOverviewHistoryDao()).collect("");
        }
    }

    /**
     * Returns the configuration history entries for one group of system files
     * or deleted jobs.
     *
     * @param name
     *            of the item.
     * @return Configs list for one group of system configuration files.
     * @throws IOException
     *             if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getSingleConfigs(String name)
        throws IOException {
        final Collection<HistoryDescr> historyDescriptions;
        if (name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
            historyDescriptions = getOverviewHistoryDao().getJobHistory(name).values();
        } else {
            historyDescriptions = getOverviewHistoryDao().getSystemHistory(name).values();
        }
        final List<ConfigInfo> configs = HistoryDescrToConfigInfo.convert(name, true, historyDescriptions, false);
        Collections.sort(configs, ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as
     * String.
     *
     * @return content of the {@literal config.xml} found in directory given by
     *         the request parameter {@literal file}.
     * @throws IOException
     *             if the config file could not be read or converted to an xml
     *             string.
     */
    public final String getFile() throws IOException {
        final String name = getRequestParameter("name");
        if ((name.contains(JobConfigHistoryConsts.DELETED_MARKER) && hasJobConfigurePermission())
                || hasConfigurePermission()) {
            final String timestamp = getRequestParameter("timestamp");
            final XmlFile xmlFile = getOldConfigXml(name, timestamp);
            return xmlFile.asString();
        } else {
            return "No permission to view config files";
        }
    }

    /**
     * Creates links to the correct configOutput.jellys for job history vs.
     * system history and for xml vs. plain text.
     *
     * @param config
     *            ConfigInfo.
     * @param type
     *            Output type ('xml' or 'plain').
     * @return The link as String.
     */
    public final String createLinkToFiles(ConfigInfo config, String type) {
        String link = null;
        final String name = config.getJob();
        String timestamp = config.getDate();

        if (name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
            // last config.xml for deleted job usually doesn't exist
            try {
                if (getSingleConfigs(name).size() > 1) {
                    timestamp = getSingleConfigs(name).get(1).getDate();
                    link = "configOutput?type=" + type + "&name=" + name
                            + "&timestamp=" + timestamp;
                }
            } catch (IOException ex) {
                LOG.log(FINEST, "Unable to get config for {0}", name);
            }
        } else if (config.getIsJob()) {
            link = getHudson().getRootUrl() + "job/" + name + getUrlName()
                    + "/configOutput?type=" + type + "&timestamp=" + timestamp;
        } else {
            link = "configOutput?type=" + type + "&name=" + name
                    + "&timestamp=" + timestamp;
        }

        return link;
    }

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
     * Parses the incoming {@literal POST} request and redirects as
     * {@literal GET showDiffFiles}.
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
        rsp.sendRedirect("showDiffFiles?name=" + parser.get("name")
                + "&timestamp1=" + parser.get("timestamp1") + "&timestamp2="
                + parser.get("timestamp2"));
    }

    /**
     * Returns the diff between two config files as a list of single lines.
     * Takes the two timestamps and the name of the system property or the
     * deleted job from the url parameters.
     *
     * @return Differences between two config versions as list of lines.
     * @throws IOException
     *             If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines() throws IOException {
        final String name = getRequestParameter("name");
        if ((name.contains(JobConfigHistoryConsts.DELETED_MARKER) && hasJobConfigurePermission())
                || hasConfigurePermission()) {
            final String timestamp1 = getRequestParameter("timestamp1");
            final String timestamp2 = getRequestParameter("timestamp2");

            final XmlFile configXml1 = getOldConfigXml(name, timestamp1);
            final String[] configXml1Lines = configXml1.asString().split("\\n");
            final XmlFile configXml2 = getOldConfigXml(name, timestamp2);
            final String[] configXml2Lines = configXml2.asString().split("\\n");

            final String diffAsString = getDiffAsString(configXml1.getFile(),
                    configXml2.getFile(), configXml1Lines, configXml2Lines);

            final List<String> diffLines = Arrays.asList(diffAsString
                    .split("\n"));
            return getDiffLines(diffLines);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets the version of the config.xml that was saved at a certain time.
     *
     * @param name
     *            The name of the system property or deleted job.
     * @param timestamp
     *            The timestamp as String.
     * @return The config file as XmlFile.
     */
    protected XmlFile getOldConfigXml(String name, String timestamp) {
        if (checkParameters(name, timestamp)) {
            if (name.contains(JobConfigHistoryConsts.DELETED_MARKER)) {
                return getHistoryDao().getOldRevision("jobs/" + name, timestamp);
            } else {
                checkConfigurePermission();
                return getHistoryDao().getOldRevision(name, timestamp);
                
            }
        } else {
            throw new IllegalArgumentException("Unable to get history from: "
                    + name);
        }
    }

    /**
     * Checks the url parameters 'name' and 'timestamp' and returns true if they
     * are neither null nor suspicious.
     *
     * @param name
     *            Name of deleted job or system property.
     * @param timestamp
     *            Timestamp of config change.
     * @return True if parameters are okay.
     */
    boolean checkParameters(String name, String timestamp) {
        checkTimestamp(timestamp);
        if (name == null || "null".equals(name)) {
            return false;
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid directory name because of '..': " + name);
        }
        return true;
    }

    /**
     * Action when 'restore' button is pressed: Restore deleted project.
     *
     * @param req Incoming StaplerRequest
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If something goes wrong
     */
    public final void doRestore(StaplerRequest req, StaplerResponse rsp) throws IOException {
        getAccessControlledObject().checkPermission(AbstractProject.CONFIGURE);

        final String deletedName = req.getParameter("name");
        final String newName = deletedName.split("_deleted_") [0];

        final XmlFile configXml = getLastAvailableConfigXml(deletedName);

        final InputStream is = new ByteArrayInputStream(configXml.asString().getBytes("UTF-8"));
        final AbstractProject project = (AbstractProject) getHudson().createProjectFromXML(findNewName(newName), is);
        moveHistoryFiles(deletedName, newName);

        rsp.sendRedirect(getHudson().getRootUrl() + project.getUrl());
    }

    /**
     * Retrieves the last or second to last config.xml.
     * The latter is necessary when the last config.xml is missing
     * although the history entry exists, which happens when a project is deleted
     * while being disabled.
     *
     * @param name The name of the deleted project.
     * @return The last or second to last config as XmlFile or null.
     */
    public XmlFile getLastAvailableConfigXml(String name) {
        XmlFile configXml = null;
        final List<ConfigInfo> configInfos;
        try {
            configInfos = getSingleConfigs(name);
        } catch (IOException ex) {
            LOG.log(FINEST, "Unable to get config history for {0}", name);
            return configXml;
        }

        if (configInfos.size() > 1) {
            Collections.sort(configInfos, ParsedDateComparator.DESCENDING);
            final ConfigInfo lastChange = configInfos.get(1);
            configXml = getOldConfigXml(name, lastChange.getDate());
        }

        return configXml;
    }

    /**
     * Finds a name for the project to be restored.
     * If the old name is already in use by another project,
     * "_" plus a number is appended to the name until an unused name is found.
     *
     * @param name The old name as String.
     * @return the new name as String.
     */
    String findNewName(String name) {
        String newName = name;
        int i = 1;
        while (getHudson().getItem(newName) != null) {
            newName = name + "_" + String.valueOf(i);
            i++;
        }
        return newName;
    }

    /**
     * Moves the history files of a restored project from the old location (_deleted_)
     * to a directory with the new name.
     * @param oldName The old name of the project (containing "_deleted_")
     * @param newName The new name of the project
     */
    private void moveHistoryFiles(String oldName, String newName) {
        getHistoryDao().moveHistory(oldName, newName);
    }

    /**
     * Action when 'restore' button in history.jelly is pressed.
     * Gets required parameter and forwards to restoreQuestion.jelly.

     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If redirect goes wrong
     */
    public final void doForwardToRestoreQuestion(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        final String name = req.getParameter("name");
        rsp.sendRedirect("restoreQuestion?name=" + name);
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
    /**
     * For tests.
     *
     * @return historyDao
     */
    OverviewHistoryDao getOverviewHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
}
