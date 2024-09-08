/*
 * The MIT License
 *
 * Copyright 2013 Lucie Votypkova.
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

import hudson.XmlFile;
import hudson.model.Api;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.verb.POST;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Lucie Votypkova
 */
@ExportedBean(defaultVisibility = -1)
public class ComputerConfigHistoryAction extends JobConfigHistoryBaseAction {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ComputerConfigHistoryAction.class.getName());

    /**
     * The agent.
     */
    private Slave agent;

    /**
     * Standard constructor using instance.
     *
     * @param agent agent.
     */
    public ComputerConfigHistoryAction(Slave agent) {
        this.agent = agent;
    }

    @Override
    public final String getDisplayName() {
        return Messages.agentDisplayName();
    }

    @Override
    public String getUrlName() {
        return JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Returns the agent.
     *
     * @return the agent.
     * @deprecated Use {@link #getAgent()} instead. This method is subject to removal in future releases, to comply
     * with Jenkins' terminology updates.
     */
    @Deprecated
    public Slave getSlave() {
        return agent;
    }

    /**
     * Returns the agent.
     *
     * @return the agent.
     */
    public Slave getAgent() {
        return agent;
    }

    @Override
    protected AccessControlled getAccessControlledObject() {
        return agent;
    }

    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Computer.CONFIGURE);
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
        return getAccessControlledObject().hasPermission(Computer.CONFIGURE);
    }

    @Override
    public int getRevisionAmount() {
        LOG.log(Level.INFO, "Paging is not implemented for agents yet!");
        return -1;
    }

    @Override
    public final String getIconFileName() {
        if (!hasConfigurePermission()) {
            return null;
        }
        return JobConfigHistoryConsts.ICONFILENAME;
    }

    /**
     * Returns the configuration history entries for one {@link Slave}.
     *
     * @return history list for one {@link Slave}.
     * @deprecated Use {@link #getAgentConfigs()} instead. This method is subject to removal in future releases,
     * to comply with Jenkins' terminology updates.
     */
    @Deprecated
    public final List<ConfigInfo> getSlaveConfigs() {
        if (!hasConfigurePermission()) {
            return Collections.emptyList();
        }
        final ArrayList<ConfigInfo> configs = new ArrayList<>();
        final ArrayList<HistoryDescr> values = new ArrayList<>(getHistoryDao().getRevisions(agent).values());
        for (final HistoryDescr historyDescr : values) {
            final String timestamp = historyDescr.getTimestamp();
            final XmlFile oldRevision = getHistoryDao().getOldRevision(agent, timestamp);
            if (oldRevision.getFile() != null) {
                configs.add(ConfigInfo.create(agent.getNodeName(), true, historyDescr, true));
            } else if ("Deleted".equals(historyDescr.getOperation())) {
                configs.add(ConfigInfo.create(agent.getNodeName(), false, historyDescr, true));
            }
        }
        configs.sort(ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Returns the configuration history entries for one {@link Slave}.
     *
     * @return history list for one {@link Slave}.
     */
    public final List<ConfigInfo> getAgentConfigs() {
        if (!hasConfigurePermission()) {
            return Collections.emptyList();
        }
        final ArrayList<ConfigInfo> configs = new ArrayList<>();
        final ArrayList<HistoryDescr> values = new ArrayList<>(getHistoryDao().getRevisions(agent).values());
        for (final HistoryDescr historyDescr : values) {
            final String timestamp = historyDescr.getTimestamp();
            final XmlFile oldRevision = getHistoryDao().getOldRevision(agent, timestamp);
            if (oldRevision.getFile() != null) {
                configs.add(ConfigInfo.create(agent.getNodeName(), true, historyDescr, true));
            } else if ("Deleted".equals(historyDescr.getOperation())) {
                configs.add(ConfigInfo.create(agent.getNodeName(), false, historyDescr, true));
            }
        }
        configs.sort(ParsedDateComparator.DESCENDING);
        return configs;
    }

    /**
     * Returns the configuration history entries for one {@link Slave} for the
     * REST API.
     *
     * @return history list for one {@link Slave}, or an empty list if not
     * authorized.
     * @throws IOException if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be
     *                     read or the path might not be urlencoded.
     * @deprecated Use {@link #getAgentConfigs()} instead. This method is subject to removal in future releases, to comply
     * with Jenkins' terminology updates.
     */
    @Deprecated
    @Exported(name = "jobConfigHistory", visibility = 1)
    public final List<ConfigInfo> getSlaveConfigsREST() throws IOException {
        return getSlaveConfigs();
    }

    /**
     * Returns the configuration history entries for one {@link Slave} for the
     * REST API.
     *
     * @return history list for one {@link Slave}, or an empty list if not
     * authorized.
     * @throws IOException if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be
     *                     read or the path might not be urlencoded.
     */
    @Exported(name = "jobConfigHistory", visibility = 1)
    public final List<ConfigInfo> getAgentConfigsREST() throws IOException {
        return getAgentConfigs();
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
        checkConfigurePermission();
        String timeStamp = this.getRequestParameter("timestamp" + timestampNumber);
        SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        try {
            format.setLenient(false);
            format.parse(timeStamp);
            return timeStamp;
        } catch (ParseException e) {
            return null;
        }

    }

    /**
     * Used in the Difference jelly only. Returns the user that made the change
     * in one of the Files shown in the Difference view(A or B). timestampNumber
     * decides between File A and File B.
     *
     * @param timestampNumber 1 for File A and 2 for File B
     * @return the user as String.
     */
    public final String getUser(int timestampNumber) {
        checkConfigurePermission();
        return getHistoryDao().getRevisions(this.agent).get(getTimestamp(timestampNumber)).getUser();
    }

    public final String getUserID(int timestamp) {
        checkConfigurePermission();
        return getHistoryDao().getRevisions(this.agent).get(getTimestamp(timestamp)).getUserID();
    }

    /**
     * Used in the Difference jelly only. Returns the operation made on one of
     * the two Files A and B. timestampNumber decides which file exactly.
     *
     * @param timestampNumber 1 for File A, 2 for File B
     * @return the operation as String.
     */
    public final String getOperation(int timestampNumber) {
        checkConfigurePermission();
        return getHistoryDao().getRevisions(this.agent).get(getTimestamp(timestampNumber)).getOperation();
    }

    /**
     * Used in the Difference jelly only. Returns the next timestamp of the next
     * entry of the two Files A and B. timestampNumber decides which file
     * exactly.
     *
     * @param timestampNumber 1 for File A, 2 for File B
     * @return the timestamp of the next entry as String.
     */
    public final String getNextTimestamp(int timestampNumber) {
        checkConfigurePermission();
        final String timestamp = this.getRequestParameter("timestamp" + timestampNumber);
        final SortedMap<String, HistoryDescr> revisions = getHistoryDao().getRevisions(this.agent);
        final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getValue().getTimestamp().equals(timestamp) && itr.hasNext()) {
                return itr.next().getValue().getTimestamp();
            }
        }
        // no next entry found
        return timestamp;
    }

    /**
     * Used in the Difference jelly only. Returns the previous timestamp of the
     * next entry of the two Files A and B. timestampNumber decides which file
     * exactly.
     *
     * @param timestampNumber 1 for File A, 2 for File B
     * @return the timestamp of the previous entry as String.
     */
    public final String getPrevTimestamp(int timestampNumber) {
        checkConfigurePermission();
        final String timestamp = this.getRequestParameter("timestamp" + timestampNumber);
        final SortedMap<String, HistoryDescr> revisions = getHistoryDao().getRevisions(this.agent);
        final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet().iterator();
        String prevTimestamp = timestamp;
        while (itr.hasNext()) {
            final String checkTimestamp = itr.next().getValue().getTimestamp();
            if (checkTimestamp.equals(timestamp)) {
                return prevTimestamp;
            } else {
                prevTimestamp = checkTimestamp;
            }
        }
        // no previous entry found
        return timestamp;
    }

    /**
     * Returns {@link ComputerConfigHistoryAction#getOldConfigXml(String)} as
     * String.
     *
     * @return content of the {@literal config.xml} found in directory given by
     * the request parameter {@literal file}.
     * @throws IOException if the config file could not be read or converted to an xml
     *                     string.
     */
    public final String getFile() throws IOException {
        checkConfigurePermission();
        final String timestamp = getRequestParameter("timestamp");
        final XmlFile xmlFile = getOldConfigXml(timestamp);
        return xmlFile.asString();
    }

    public final List<Line> getLines(boolean hideVersionDiffs) throws IOException {
        checkConfigurePermission();
        final String timestamp1 = getRequestParameter("timestamp1");
        final String timestamp2 = getRequestParameter("timestamp2");
        return getLines(getOldConfigXml(timestamp1), getOldConfigXml(timestamp2), hideVersionDiffs);
    }

    public XmlSyntaxChecker.Answer checkXmlSyntax(String timestamp) {
        return XmlSyntaxChecker.check(getOldConfigXml(timestamp).getFile());
    }

    /**
     * Gets the version of the config.xml that was saved at a certain time.
     *
     * @param timestamp The timestamp as String.
     * @return The config file as XmlFile.
     */
    private XmlFile getOldConfigXml(String timestamp) {
        checkConfigurePermission();
        final XmlFile oldRevision = getHistoryDao().getOldRevision(agent, timestamp);
        if (oldRevision.getFile() != null) {
            return oldRevision;
        } else {
            throw new IllegalArgumentException("Non existent timestamp " + timestamp);
        }
    }

    /**
     * Action when 'restore' button is pressed: Replace current config file by
     * older version.
     *
     * @param req Incoming StaplerRequest
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If something goes wrong
     */
    @POST
    public final void doRestore(StaplerRequest req, StaplerResponse rsp) throws IOException {
        checkConfigurePermission();
        final String timestamp = req.getParameter("timestamp");

        final XmlFile xmlFile = getHistoryDao().getOldRevision(agent, timestamp);
        final Slave newAgent = (Slave) Jenkins.XSTREAM2.fromXML(xmlFile.getFile());
        final List<Node> nodes = new ArrayList<>(Jenkins.get().getNodes());
        nodes.remove(agent);
        nodes.add(newAgent);
        agent = newAgent;
        Jenkins.get().setNodes(nodes);
        final Computer computer = agent.toComputer();
        if (computer == null) {
            LOG.log(Level.WARNING, "Failed to redirect to agent url of agent: " + agent.getNodeName());
        }
        else {
            rsp.sendRedirect(Jenkins.get().getRootUrl() + computer.getUrl());
        }
    }

    /**
     * Action when 'restore' button in showDiffFiles.jelly is pressed. Gets
     * required parameter and forwards to restoreQuestion.jelly.
     *
     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If XML file can't be read
     */
    public final void doForwardToRestoreQuestion(StaplerRequest req, StaplerResponse rsp) throws IOException {
        final String timestamp = req.getParameter("timestamp");
        rsp.sendRedirect("restoreQuestion?timestamp=" + timestamp);
    }

    @POST
    public final void doDeleteRevision(StaplerRequest req) {
        checkDeleteEntryPermission();
        final String timestamp = req.getParameter("timestamp");
        PluginUtils.getHistoryDao().deleteRevision(this.getAgent(), timestamp);
        //do nothing with the rsp
    }

    public boolean revisionEqualsCurrent(String timestamp) {
        //going over Jenkins.get().getNode(..) is necessary because this.getAgent() returns an old version of the node.
        return PluginUtils.getHistoryDao().revisionEqualsCurrent(Jenkins.get().getNode(this.getAgent().getNodeName()), timestamp);
    }

    /**
     * Action when 'Show / hide Version Changes' button in showDiffFiles.jelly is pressed:
     * Reloads the page with "showVersionDiffs" parameter inversed.
     *
     * @param req StaplerRequest created by pressing the button
     * @param rsp Outgoing StaplerResponse
     * @throws IOException If XML file can't be read
     */
    public final void doToggleShowHideVersionDiffs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        //simply reload current page.
        final String timestamp1 = req.getParameter("timestamp1");
        final String timestamp2 = req.getParameter("timestamp2");
        final String showVersionDiffs = Boolean.toString(!Boolean.parseBoolean(req.getParameter("showVersionDiffs")));
        rsp.sendRedirect("showDiffFiles?" + "timestamp1=" + timestamp1 + "&timestamp2=" + timestamp2 + "&showVersionDiffs=" + showVersionDiffs);
    }

    public Api getApi() {
        return new Api(this);
    }

    public int getLeadingWhitespace(String str) {
        return str == null ? 0 : str.indexOf(str.trim());
    }
}
