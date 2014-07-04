package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.Api;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Exported;

/**
 * @author Stefan Brausch
 */
@ExportedBean(defaultVisibility = -1)
public class JobConfigHistoryProjectAction extends JobConfigHistoryBaseAction {

    /** The project. */
    private final transient AbstractItem project;

    /**
     * @param project
     *            for which configurations should be returned.
     */
    public JobConfigHistoryProjectAction(AbstractItem project) {
        super();
        this.project = project;
    }

    /**
     * For testing only.
     *
     * @param hudson instance
     * @param project
     *            for which configurations should be returned.
     */
    JobConfigHistoryProjectAction(Hudson hudson, AbstractItem project) {
        super(hudson);
        this.project = project;
    }
    /**
     * {@inheritDoc}
     *
     * Make method final, as we always want the same icon file. Returns
     * {@literal null} to hide the icon if the user is not allowed to configure
     * jobs.
     */
    public final String getIconFileName() {
        if (!hasReadPermission()) {
            return null;
        }
        if (!getPlugin().getSaveModuleConfiguration() && project instanceof MavenModule) {
            return null;
        }

        return JobConfigHistoryConsts.ICONFILENAME;
    }

    /**
     * Returns the configuration history entries for one {@link AbstractItem}.
     *
     * @return history list for one {@link AbstractItem}.
     * @throws IOException
     *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be read or the path might not be urlencoded.
     */
    public final List<ConfigInfo> getJobConfigs() throws IOException {
        checkReadPermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final ArrayList<HistoryDescr> values = new ArrayList<HistoryDescr>(
                getHistoryDao().getRevisions(project.getConfigFile()).values());
        final String maxEntriesPerPageAsString = getPlugin().getMaxEntriesPerPage();
        final int maxEntriesPerPage;
        if (maxEntriesPerPageAsString != null && !maxEntriesPerPageAsString.isEmpty()) {
            maxEntriesPerPage = Math.min(values.size(), Integer.parseInt(maxEntriesPerPageAsString));
        } else {
            maxEntriesPerPage = values.size();
        }
        for (final HistoryDescr historyDescr : values.subList(0, maxEntriesPerPage)) {
            final String timestamp = historyDescr.getTimestamp();
            final XmlFile oldRevision = getHistoryDao().getOldRevision(project, timestamp);
            if (oldRevision.getFile() != null) {
                configs.add(ConfigInfo.create(
                        project.getFullName(),
                        true,
                        historyDescr,
                        true));
            } else if ("Deleted".equals(historyDescr.getOperation())) {
                configs.add(ConfigInfo.create(
                        project.getFullName(),
                        false,
                        historyDescr,
                        true));
            }
        }
        Collections.sort(configs, ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Returns the configuration history entries for one {@link AbstractItem} for the REST API.
     *
     * @return history list for one {@link AbstractItem}, or an empty list if not authorized.
     * @throws IOException
     *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be read or the path might not be urlencoded.
     */
    @Exported(name = "jobConfigHistory", visibility = 1)
    public final List<ConfigInfo> getJobConfigsREST() throws IOException {
        List<ConfigInfo> configs = null;
        try {
            configs = getJobConfigs();
        } catch (org.acegisecurity.AccessDeniedException e) {
            configs = new ArrayList<ConfigInfo>();
        }
        return configs;
    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as
     * String.
     *
     * @return content of the {@literal config.xml} found in directory given by the
     *         request parameter {@literal file}.
     * @throws IOException
     *             if the config file could not be read or converted to an xml
     *             string.
     */
    public final String getFile() throws IOException {
        checkReadPermission();
        final String timestamp = getRequestParameter("timestamp");
        final XmlFile xmlFile = getOldConfigXml(timestamp);
        return xmlFile.asString();
    }

    /**
     * Returns the project for which we want to see the config history, the config files or the diff.
     *
     * @return project
     */
    public final AbstractItem getProject() {
        return project;
    }

    /**
     * {@inheritDoc} Returns the project.
     */
    @Override
    protected AccessControlled getAccessControlledObject() {
        return project;
    }

    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Item.CONFIGURE);
    }

    @Override
    public boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(Item.CONFIGURE);
    }

    @Override
    protected void checkReadPermission() {
        getAccessControlledObject().checkPermission(Item.READ);
    }

    @Override
    public boolean hasReadPermission() {
        return getAccessControlledObject().hasPermission(Item.READ);
    }

    /**
     * Used in the Difference jelly only. Returns one of the two timestamps that
     * have been passed to the Difference page as parameter. timestampNumber
     * must be 1 or 2.
     * 
     * @param timestampNumber
     *            1 for timestamp1 and 2 for timestamp2
     * @return the timestamp as String.
     */
    public final String getTimestamp(int timestampNumber) {
        checkReadPermission();
        return this.getRequestParameter("timestamp" + timestampNumber);
    }

    /**
     * Used in the Difference jelly only. Returns the user that made the change
     * in one of the Files shown in the Difference view(A or B). timestampNumber
     * decides between File A and File B.
     * 
     * @param timestampNumber
     *            1 for File A and 2 for File B
     * @return the user as String.
     */
    public final String getUser(int timestampNumber) {
        checkReadPermission();
        return getHistoryDao().getRevisions(this.project.getConfigFile())
                .get(getTimestamp(timestampNumber)).getUser();
    }

    /**
     * Used in the Difference jelly only. Returns the operation made on one of
     * the two Files A and B. timestampNumber decides which file exactly.
     * 
     * @param timestampNumber
     *            1 for File A, 2 for File B
     * @return the operation as String.
     */
    public final String getOperation(int timestampNumber) {
        checkReadPermission();
        return getHistoryDao().getRevisions(this.project.getConfigFile())
                .get(getTimestamp(timestampNumber)).getOperation();
    }
    
    /**
     * Used in the Difference jelly only. Returns the next timestamp of
     * the next entry of the two Files A and B. timestampNumber decides which 
     * file exactly.
     * 
     * @param timestampNumber
     *            1 for File A, 2 for File B
     * @return the timestamp of the next entry as String.
     */
    public final String getNextTimestamp(int timestampNumber) {
        checkReadPermission();
        final String timestamp = this.getRequestParameter("timestamp" + timestampNumber);
        final SortedMap<String, HistoryDescr> revisions = getHistoryDao().getRevisions(this.project.getConfigFile());
        final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getValue().getTimestamp().equals((String) timestamp) && itr.hasNext()) {
                return itr.next().getValue().getTimestamp();
            }
        }
        //no next entry found
        return timestamp;
    }
    
    /**
     * Used in the Difference jelly only. Returns the previous timestamp of
     * the next entry of the two Files A and B. timestampNumber decides which 
     * file exactly.
     * 
     * @param timestampNumber
     *            1 for File A, 2 for File B
     * @return the timestamp of the preious entry as String.
     */
    public final String getPrevTimestamp(int timestampNumber) {
        checkReadPermission();
        final String timestamp = this.getRequestParameter("timestamp" + timestampNumber);
        final SortedMap<String, HistoryDescr> revisions = getHistoryDao().getRevisions(this.project.getConfigFile());
        final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet().iterator();
        String prevTimestamp = timestamp;
        while (itr.hasNext()) {
            final String checkTimestamp = itr.next().getValue().getTimestamp();
            if (checkTimestamp.equals((String) timestamp)) {
                return prevTimestamp;
            } else {
                prevTimestamp = checkTimestamp;
            }
        }
        //no previous entry found
        return timestamp;
    }
            
    /**
     * Takes the two timestamp request parameters and returns the diff between the corresponding
     * config files of this project as a list of single lines.
     *
     * @return Differences between two config versions as list of lines.
     * @throws IOException If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines() throws IOException {
        checkReadPermission();
        final String timestamp1 = getRequestParameter("timestamp1");
        final String timestamp2 = getRequestParameter("timestamp2");

        final XmlFile configXml1 = getOldConfigXml(timestamp1);
        final String[] configXml1Lines = configXml1.asString().split("\\n");
        final XmlFile configXml2 = getOldConfigXml(timestamp2);
        final String[] configXml2Lines = configXml2.asString().split("\\n");

        final String diffAsString = getDiffAsString(configXml1.getFile(), configXml2.getFile(),
                configXml1Lines, configXml2Lines);

        final List<String> diffLines = Arrays.asList(diffAsString.split("\n"));
        return getDiffLines(diffLines);
    }

    /**
     * Gets the version of the config.xml that was saved at a certain time.
     *
     * @param timestamp The timestamp as String.
     * @return The config file as XmlFile.
     */
    private XmlFile getOldConfigXml(String timestamp) {
        checkReadPermission();
        final XmlFile oldRevision = getHistoryDao().getOldRevision(project, timestamp);
        if (oldRevision.getFile() != null) {
            return oldRevision;
        } else {
            throw new IllegalArgumentException("Non existent timestamp " + timestamp);
        }
    }

    /**
     * Action when 'restore' button is pressed: Replace current config file by older version.
     *
     * @param req Incoming StaplerRequest
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If something goes wrong
     */
    public final void doRestore(StaplerRequest req, StaplerResponse rsp) throws IOException {
        checkConfigurePermission();
        final String timestamp = req.getParameter("timestamp");

        final XmlFile xmlFile = getHistoryDao().getOldRevision(project, timestamp);
        final InputStream is = new ByteArrayInputStream(xmlFile.asString().getBytes("UTF-8"));

        project.updateByXml((Source) new StreamSource(is));
        project.save();
        rsp.sendRedirect(getHudson().getRootUrl() + project.getUrl());
    }

    /**
     * Action when 'restore' button in showDiffFiles.jelly is pressed.
     * Gets required parameter and forwards to restoreQuestion.jelly.

     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If XML file can't be read
     */
    public final void doForwardToRestoreQuestion(StaplerRequest req, StaplerResponse rsp)
        throws IOException {
        final String timestamp = req.getParameter("timestamp");
        rsp.sendRedirect("restoreQuestion?timestamp=" + timestamp);
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

    public Api getApi() {
        return new Api(this);
    }
}
