/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch, Mirko Friedenhagen.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.RootAction;
import hudson.model.TopLevelItem;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.MultipartFormDataParser;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;

/**
 * @author Stefan Brausch
 * @author Mirko Friedenhagen
 */

@ExportedBean(defaultVisibility = -1)
@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction
        implements
        RootAction {

    /**
     * Our logger.
     */
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
     * <p>
     * This actions always starts from the context directly, so prefix
     * {@link JobConfigHistoryConsts#URLNAME} with a slash.
     */
    @Override
    public final String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make method final, as we always want the same icon file. Returns
     * {@literal null} to hide the icon if the user is not allowed to configure
     * jobs.
     */
    public final String getIconFileName() {
        if (hasConfigurePermission() || hasJobConfigurePermission()
                || hasReadExtensionPermission()) {
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
     * @throws IOException if one of the history entries might not be read.
     */
    @Exported(visibility = 1)
    public final List<ConfigInfo> getConfigs() throws IOException {
        final String filter = getRequestParameter("filter");
        List<ConfigInfo> configs;

        if (filter == null || "system".equals(filter)) {
            configs = getSystemConfigs();
        } else if ("all".equals(filter)) {
            configs = getJobConfigs("jobs");
            configs.addAll(getJobConfigs("deleted"));
            configs.addAll(getSystemConfigs());
        } else {
            configs = getJobConfigs(filter);
        }

        configs.sort(ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Calculates a list containing the .subList(from, to) of the newest-first list of job config revision entries.
     * Does not read the history.xmls unless it is inevitable.
     * Since filter=created requires to read the history.xml of each revision which makes paging pointless from a performance and usability perspective,
     * all configs are returned in that case.
     *
     * @param from the first revision to display
     * @param to   the first revision not to display anymore
     * @return a list equivalent to getJobConfigs().subList(from, to) (except for filter=created), but more efficiently calculated.
     */
    public final List<ConfigInfo> getConfigs(int from, int to) throws IOException {

        if (from > to)
            throw new IllegalArgumentException("start index is greater than end index: (" + from + ", " + to + ")");
        final int revisionAmount = getRevisionAmount();
        if (from > revisionAmount) {
            LOG.log(FINEST, "Unexpected arguments while generating overview page: start index ({0}) is greater than total revision amount ({1})!",
                    new Object[]{from, revisionAmount});
            return Collections.emptyList();
        }

        if (to > revisionAmount) {
            to = revisionAmount;
        }
        final String filter = getRequestParameter("filter");

        HashMap<String, ConfigType> timestampNameToConfigTypeMap = new HashMap<>();

        SortedMap<String, Pair<String, HistoryDescr>> historyDescrSortedMap;
        if (filter == null || filter.equals("") || filter.equals("system")) {
            historyDescrSortedMap = getSystemConfigsHistoryDescr();
            historyDescrSortedMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.SYSTEM));
        } else if (filter.equals("all")) {
            historyDescrSortedMap = getJobConfigsHistoryDescr("jobs");
            historyDescrSortedMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.JOB_UNKNOWN));

            final SortedMap<String, Pair<String, HistoryDescr>> deletedJobsMap = getJobConfigsHistoryDescr("deleted");
            historyDescrSortedMap.putAll(deletedJobsMap);
            deletedJobsMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.JOB_DELETED));

            final SortedMap<String, Pair<String, HistoryDescr>> sysConfigsMap = getSystemConfigsHistoryDescr();
            historyDescrSortedMap.putAll(sysConfigsMap);
            sysConfigsMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.SYSTEM));
        } else if (filter.equals("deleted")) {
            historyDescrSortedMap = getJobConfigsHistoryDescr("deleted");
            historyDescrSortedMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.JOB_DELETED));
        } else if (filter.equals("jobs")) {
            historyDescrSortedMap = getJobConfigsHistoryDescr("jobs");
            historyDescrSortedMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.JOB_UNKNOWN));

            final SortedMap<String, Pair<String, HistoryDescr>> deletedJobsMap = getJobConfigsHistoryDescr("deleted");
            historyDescrSortedMap.putAll(deletedJobsMap);
            deletedJobsMap.keySet().forEach(timestampAndName -> timestampNameToConfigTypeMap.put(timestampAndName, ConfigType.JOB_DELETED));
        } else {
            //hand this over to getConfigs(). The case "created" or "changed" can't be handled since that would require reading the history.xml.
            return getConfigs();
        }


        //apply the from-to filter
        final List<ConfigInfo> configs = new ArrayList<>();
        final ArrayList<String> keyList = new ArrayList<>(historyDescrSortedMap.keySet());
        Collections.reverse(keyList);
        for (String timestampAndName : keyList.subList(from, to)) {
            Pair<String, HistoryDescr> entry = historyDescrSortedMap.get(timestampAndName);
            //create configs
            //differ between job and not job
            if (!timestampNameToConfigTypeMap.containsKey(timestampAndName)) {
                throw new IllegalStateException("this shouldn happen.");
            }

            ConfigType configType = timestampNameToConfigTypeMap.get(timestampAndName);
            switch (configType) {
                case JOB:
                    configs.add(ConfigInfo.create(entry.first, true, entry.second, true));
                    break;
                case JOB_DELETED:
                    if (entry.second.getOperation().equalsIgnoreCase("deleted"))
                        configs.add(ConfigInfo.create(entry.first, false, entry.second, false));
                    break;
                case JOB_UNKNOWN:
                    HistoryDescr historyDescr = entry.second;
                    configs.add(ConfigInfo.create(entry.first, !historyDescr.getOperation().equalsIgnoreCase("deleted"), entry.second, true));
                    break;
                case SYSTEM:
                    configs.add(ConfigInfo.create(entry.first, true, entry.second, false));
                    break;
                default:
                    throw new IllegalStateException("Unexpected config type: " + configType);
            }

        }
        return configs;
    }

    /**
     * Returns the configuration history entries for all system files in this
     * Jenkins instance.
     *
     * @return List of config infos.
     */
    public List<ConfigInfo> getSystemConfigs() {
        final List<ConfigInfo> configs = new ArrayList<>();
        if (!hasConfigurePermission()) {
            return configs;
        }

        final File[] itemDirs = getOverviewHistoryDao().getSystemConfigs();
        for (final File itemDir : itemDirs) {
            final String itemName = itemDir.getName();
            configs.addAll(HistoryDescrToConfigInfo.convert(itemName, true,
                    getOverviewHistoryDao().getSystemHistory(itemName).values(),
                    false));

        }
        return configs;
    }

    /*
    Maps "Timestamp#ProjectName" to <ProjectName, HistoryDescr>
     */
    private SortedMap<String, Pair<String, HistoryDescr>> getSystemConfigsHistoryDescr() {
        SortedMap configsMap = new TreeMap<String, Pair<String, HistoryDescr>>();
        if (!hasConfigurePermission()) {
            return Collections.emptySortedMap();
        }

        final File[] itemDirs = getOverviewHistoryDao().getSystemConfigs();
        for (final File itemDir : itemDirs) {
            final String itemName = itemDir.getName();
            configsMap.putAll(getOverviewHistoryDao().getSystemHistory(itemName).entrySet().stream()
                    .map(entry -> {
                        String timestamp = entry.getKey();
                        HistoryDescr historyDescr = entry.getValue();
                        return new Pair(timestamp + "#" + itemName, new Pair(itemName, historyDescr));
                    }).collect(Collectors.toMap((pair) -> pair.first, (pair) -> pair.second)));
        }
        return configsMap;
    }

    /**
     * Returns the configuration history entries for all jobs or deleted jobs in
     * this Jenkins instance.
     *
     * @param type Whether we want to see all jobs or just the deleted jobs.
     * @return List of config infos.
     * @throws IOException if one of the history entries might not be read.
     */
    public List<ConfigInfo> getJobConfigs(String type) throws IOException {
        if (!hasJobConfigurePermission() && !hasReadExtensionPermission()) {
            return Collections.emptyList();
        } else {
            return new ConfigInfoCollector(type, getOverviewHistoryDao())
                    .collect();
        }
    }

    /**
     * @param type only all or deleted!
     */
    private SortedMap<String, Pair<String, HistoryDescr>> getJobConfigsHistoryDescr(String type) {

        SortedMap<String, Pair<String, HistoryDescr>> configsMap = new TreeMap<>();
        if (!hasConfigurePermission()) {
            return Collections.emptySortedMap();
        }

        ArrayList<File> itemDirs = new ArrayList<>();
        if (type.equals("deleted")) {
            itemDirs.addAll(Arrays.asList(getOverviewHistoryDao().getDeletedJobs()));
        } else if (type.equals("jobs")) {
            itemDirs.addAll(Arrays.asList(getOverviewHistoryDao().getJobs()));
            itemDirs.addAll(Arrays.asList(getOverviewHistoryDao().getDeletedJobs()));
        }

        for (final File itemDir : itemDirs) {
            final String itemName =
                    new File(this.getPlugin().getConfiguredHistoryRootDir(), JobConfigHistoryConsts.JOBS_HISTORY_DIR)
                            .toPath()
                            .relativize(itemDir.toPath()).toString();
            configsMap.putAll(getOverviewHistoryDao().getJobHistory(itemName).entrySet().stream()
                    .map(entry -> {
                        String timestamp = entry.getKey();
                        HistoryDescr historyDescr = entry.getValue();
                        return new Pair<>(timestamp + "#" + itemName, new Pair<>(itemName, historyDescr));
                    }).collect(Collectors.toMap((pair) -> pair.first, (pair) -> pair.second)));
        }

        return configsMap;
    }

    /**
     * Returns the configuration history entries for one group of system files
     * or deleted jobs.
     *
     * @param name of the item.
     * @return Configs list for one group of system configuration files.
     * @throws IOException if one of the history entries might not be read.
     */
    public final List<ConfigInfo> getSingleConfigs(String name)
            throws IOException {
        final Collection<HistoryDescr> historyDescriptions;
        if (name.contains(DeletedFileFilter.DELETED_MARKER)) {
            historyDescriptions = getOverviewHistoryDao().getJobHistory(name)
                    .values();
        } else {
            historyDescriptions = getOverviewHistoryDao().getSystemHistory(name)
                    .values();
        }
        final List<ConfigInfo> configs = HistoryDescrToConfigInfo.convert(name,
                true, historyDescriptions, false);
        configs.sort(ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * @param name of the item
     * @param from the first revision to display
     * @param to   the first revision not to display anymore
     * @return the same as {@link #getSingleConfigs(String)}, but cut for paging.
     */
    public final List<ConfigInfo> getSingleConfigs(String name, int from, int to) {
        if (from > to)
            throw new IllegalArgumentException("start index is greater than end index: (" + from + ", " + to + ")");
        final int revisionAmount = getRevisionAmount();
        if (from > revisionAmount) {
            LOG.log(FINEST, "Unexpected arguments while generating overview page: start index ({0}) is greater than total revision amount ({1})!",
                    new Object[]{from, revisionAmount});
            return Collections.emptyList();
        }

        if (to > revisionAmount) {
            to = revisionAmount;
        }

        final ArrayList<HistoryDescr> historyDescriptions;
        if (name.contains(DeletedFileFilter.DELETED_MARKER)) {
            historyDescriptions = new ArrayList<>(getOverviewHistoryDao().getJobHistory(name).values());
        } else {
            historyDescriptions = new ArrayList<>(getOverviewHistoryDao().getSystemHistory(name).values());
        }
        Collections.reverse(historyDescriptions);

//        return toConfigInfoList(historyDescriptions, name.contains(DeletedFileFilter.DELETED_MARKER), name, from, to);
        final List<HistoryDescr> cuttedHistoryDescrs = historyDescriptions.subList(from, to);

        final List<ConfigInfo> configs = HistoryDescrToConfigInfo.convert(name, true, cuttedHistoryDescrs, false);
        configs.sort(ParsedDateComparator.DESCENDING);
        return configs;
    }

    @Override
    public int getRevisionAmount() {
        final String filter = getRequestParameter("filter");
        final String historyRequestURI = JobConfigHistoryConsts.URLNAME + "/history";
        if (getCurrentRequest().getRequestURI().endsWith(historyRequestURI)) {
            //history diff page handling
            final String name = getRequestParameter("name");
            if (name != null) {
                return (name.contains(DeletedFileFilter.DELETED_MARKER))
                        ? getOverviewHistoryDao().getJobRevisionAmount(name)
                        : getOverviewHistoryDao().getSystemRevisionAmount(name);
            } else {
                LOG.log(FINEST, "system config not given.");
                return -1;
            }
        }

        //overview page handling
        if (filter == null || filter.equals("") || filter.equals("system")) {
            return getOverviewHistoryDao().getSystemRevisionAmount();
        } else if (filter.equals("jobs")) {
            return getOverviewHistoryDao().getJobRevisionAmount();
        } else if (filter.equals("all")) {
            return getOverviewHistoryDao().getTotalRevisionAmount();
        } else if (filter.equals("deleted")) {
            return getOverviewHistoryDao().getDeletedJobAmount();
        } else return -1;
    }

    /**
     * Returns {@link JobConfigHistoryRootAction#getOldConfigXml(String, String)} as
     * String.
     *
     * @return content of the {@literal config.xml} found in directory given by
     * the request parameter {@literal file}.
     * @throws IOException if the config file could not be read or converted to an xml
     *                     string.
     */
    public final String getFile() throws IOException {
        final String name = getRequestParameter("name");
        if ((name.contains(DeletedFileFilter.DELETED_MARKER)
                && hasJobConfigurePermission()) || hasConfigurePermission()) {
            final String timestamp = getRequestParameter("timestamp");
            final XmlFile xmlFile = getOldConfigXml(name, timestamp);
            return xmlFile.asString();
        } else {
            return "No permission to view config files";
        }
    }

    /**
     * Creates links to the correct configOutput.jelly for job history vs.
     * system history and for xml vs. plain text.
     *
     * @param config ConfigInfo.
     * @param type   Output type ('xml' or 'plain').
     * @return The link as String.
     */
    public final String createLinkToFiles(ConfigInfo config, String type) {
        String link = null;
        final String name = config.getJob();
        String timestamp = config.getDate();

        if (name.contains(DeletedFileFilter.DELETED_MARKER)) {
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
            link = Jenkins.get().getRootUrl() + "job/" + name + getUrlName()
                    + "/configOutput?type=" + type + "&timestamp=" + timestamp;
        } else {
            link = "configOutput?type=" + type + "&name=" + name + "&timestamp="
                    + timestamp;
        }

        return link;
    }

    @Override
    public AccessControlled getAccessControlledObject() {
        return Jenkins.get();
    }

    @Override
    public void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Permission.CONFIGURE);
    }

    @Override
    protected void checkDeleteEntryPermission() {
        getAccessControlledObject().checkPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
    }

    @Override
    public boolean hasAdminPermission() {
        return getAccessControlledObject().hasPermission(Jenkins.ADMINISTER);
    }

    @Override
    public boolean hasDeleteEntryPermission() {
        return getAccessControlledObject().hasPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
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
     * Returns whether the current user may read configure jobs.
     *
     * @return true if the current user may read configure jobs.
     */
    public boolean hasReadExtensionPermission() {
        return getAccessControlledObject().hasPermission(Item.EXTENDED_READ);
    }

    /**
     * Parses the incoming {@literal POST} request and redirects as
     * {@literal GET showDiffFiles}.
     *
     * @param req incoming request
     * @param rsp outgoing response
     * @throws ServletException when parsing the request as {@link MultipartFormDataParser}
     *                          does not succeed.
     * @throws IOException      when the redirection does not succeed.
     */
    @Override
    public final void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        final MultipartFormDataParser parser = new MultipartFormDataParser(req);
        rsp.sendRedirect("showDiffFiles?name=" + parser.get("name")
                + "&timestamp1=" + parser.get("timestamp1") + "&timestamp2="
                + parser.get("timestamp2"));
    }

    /**
     * Takes the two timestamp request parameters and returns the diff between
     * the corresponding config files of this project as a list of single lines.
     * Filters lines that contain plugin version information.
     *
     * @param hideVersionDiffs determines whether lines that match the
     *                         <i>ignoredLinesPattern</i> shall be hidden or not.
     * @return Differences between two config versions as list of lines.
     * @throws IOException If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines(boolean hideVersionDiffs) throws IOException {
        final String name = getRequestParameter("name");
        if ((name.contains(DeletedFileFilter.DELETED_MARKER)
                && hasJobConfigurePermission()) || hasConfigurePermission()) {
            final String timestamp1 = getRequestParameter("timestamp1");
            final String timestamp2 = getRequestParameter("timestamp2");

            /*final XmlFile configXml1 = getOldConfigXml(name, timestamp1);
            final String[] configXml1Lines = configXml1.asString().split("\\n");
            final XmlFile configXml2 = getOldConfigXml(name, timestamp2);
            final String[] configXml2Lines = configXml2.asString().split("\\n");

            //compute the diff with respect to ignoredLinesPattern if hideVersionDiffs == true
            final String diffAsString = getDiffAsString(configXml1.getFile(),
                    configXml2.getFile(), configXml1Lines, configXml2Lines, hideVersionDiffs);

            final List<String> diffLines = Arrays
                    .asList(diffAsString.split("\n"));
            return getDiffLines(diffLines);*/
            return getLines(getOldConfigXml(name, timestamp1), getOldConfigXml(name, timestamp2), hideVersionDiffs);
        } else {
            return Collections.emptyList();
        }
    }

    public XmlSyntaxChecker.Answer checkXmlSyntax(String name, String timestamp) {
        return XmlSyntaxChecker.check(getOldConfigXml(name, timestamp).getFile());
    }

    /**
     * Used in the Difference jelly only. Returns one of the two timestamps that
     * have been passed to the Difference page as parameter. timestampNumber
     * must be 1 or 2.
     *
     * @param timestampNumber 1 for timestamp1 and 2 for timestamp2
     * @return the timestamp as String.
     */
    public final String getTimestamp(int timestampNumber) {
        if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
            checkConfigurePermission();
            return null;
        }
        String timeStamp = this
                .getRequestParameter("timestamp" + timestampNumber);
        SimpleDateFormat format = new java.text.SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss");

        try {
            format.setLenient(false);
            format.parse(timeStamp);
            return timeStamp;
        } catch (ParseException e) {
            return null;
        }

    }

    /**
     * Gets the version of the config.xml that was saved at a certain time.
     *
     * @param name      The name of the system property or deleted job.
     * @param timestamp The timestamp as String.
     * @return The config file as XmlFile.
     */
    public XmlFile getOldConfigXml(String name, String timestamp) {
        if (checkParameters(name, timestamp)) {
            if (name.contains(DeletedFileFilter.DELETED_MARKER)) {
                return getHistoryDao().getOldRevision("jobs/" + name,
                        timestamp);
            } else {
                if (!hasConfigurePermission() && !hasReadExtensionPermission()
                        && !hasJobConfigurePermission()) {
                    checkConfigurePermission();
                    return null;
                }
                return getHistoryDao().getOldRevision(name, timestamp);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unable to get history from: " + name);
        }
    }

    /**
     * Checks the url parameters 'name' and 'timestamp' and returns true if they
     * are neither null nor suspicious.
     *
     * @param name      Name of deleted job or system property.
     * @param timestamp Timestamp of config change.
     * @return True if parameters are okay.
     */
    public boolean checkParameters(String name, String timestamp) {
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
    @POST
    public final void doRestore(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        getAccessControlledObject().checkPermission(Item.CONFIGURE);

        final String deletedName = req.getParameter("name");
        final String newName = deletedName.split("_deleted_")[0];

        final XmlFile configXml = getLastAvailableConfigXml(deletedName);

        final InputStream is = new ByteArrayInputStream(
                configXml.asString().getBytes(StandardCharsets.UTF_8));
        final String calculatedNewName = findNewName(newName);

        //TODO problem: this only creates Items with Jenkins.get() as parent ItemGroup, which breaks the restoration of folders.
        final TopLevelItem project = Jenkins.get().createProjectFromXML(calculatedNewName, is);
        // TODO: Casting here should be removed.
        ((FileHistoryDao) getHistoryDao()).copyHistoryAndDelete(deletedName,
                calculatedNewName);

        rsp.sendRedirect(Jenkins.get().getRootUrl() + project.getUrl());
    }

    /**
     * Retrieves the last or second to last config.xml. The latter is necessary
     * when the last config.xml is missing although the history entry exists,
     * which happens when a project is deleted while being disabled.
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
            configInfos.sort(ParsedDateComparator.DESCENDING);
            final ConfigInfo lastChange = configInfos.get(1);
            configXml = getOldConfigXml(name, lastChange.getDate());
        }

        return configXml;
    }

    /**
     * Finds a name for the project to be restored. If the old name is already
     * in use by another project, "_" plus a number is appended to the name
     * until an unused name is found.
     *
     * @param name The old name as String.
     * @return the new name as String.
     */
    public String findNewName(String name) {
        String newName = name;
        int i = 1;
        while (Jenkins.get().getItem(newName) != null) {
            newName = name + "_" + i;
            i++;
        }
        return newName;
    }

    /**
     * Action when 'restore' button in history.jelly is pressed. Gets required
     * parameter and forwards to restoreQuestion.jelly.
     *
     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If redirect goes wrong
     */
    public final void doForwardToRestoreQuestion(StaplerRequest req,
                                                 StaplerResponse rsp) throws IOException {
        final String name = req.getParameter("name");
        rsp.sendRedirect("restoreQuestion?name=" + name);
    }

    @POST
    public final void doDeleteRevision(StaplerRequest req, StaplerResponse rsp) {
        checkDeleteEntryPermission();
        final String timestamp = req.getParameter("timestamp");
        final String name = req.getParameter("name");
        final File[] candidatesArray = (name.contains(DeletedFileFilter.DELETED_MARKER)) ? getOverviewHistoryDao().getDeletedJobs() : getOverviewHistoryDao().getSystemConfigs();
        final List<File> candidates = Arrays.stream(candidatesArray).filter(file -> file.getName().equals(name)).collect(Collectors.toList());

        if (candidates.size() == 1) {
            getHistoryDao().deleteRevision(candidates.get(0), timestamp);
        } else {
            LOG.log(WARNING, "there should be only one entry for \"{0}\". Instead there are {1}",
                    new Object[]{name, candidates.size()}
            );
        }

    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    protected HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    protected OverviewHistoryDao getOverviewHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

    public Api getApi() {
        return new Api(this);
    }

    public int getLeadingWhitespace(String str) {
        return str == null ? 0 : str.indexOf(str.trim());
    }

    /**
     * Used to map a timestamp#name identifier to its related config type.
     */
    private enum ConfigType {
        SYSTEM, JOB, JOB_DELETED, JOB_UNKNOWN
    }
}
