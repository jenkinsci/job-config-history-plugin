/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import hudson.util.MultipartFormDataParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 *
 * @author Lucie Votypkova
 */
public class ComputerConfigHistoryAction extends JobConfigHistoryBaseAction {
    
    /**
     * The slave.
     */
    private Slave slave;
    
    /**
     * The hudson instance.
     */
    private final Hudson hudson;
   
    /**
     * Standard constructor using instance.
     * 
     * * @param slave Slave.
     */
    public ComputerConfigHistoryAction(Slave slave) {
        this.slave = slave;
        hudson = Hudson.getInstance();
    }

    @Override
    public final String getDisplayName() {
        return Messages.slaveDisplayName();
    }

    @Override
    public String getUrlName() {
        return JobConfigHistoryConsts.URLNAME;
    }
    
    /**
     * Returns the slave.
     *
     * @return the slave.
     */
    
    public Slave getSlave() {
        return slave;
    }

    @Override
    protected AccessControlled getAccessControlledObject() {
        return slave;
    }

    @Override
    protected void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Computer.CONFIGURE);
    }

    @Override
    public boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(Computer.CONFIGURE);
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
     * @throws IOException
     *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be read or the path might not be urlencoded.
     */
    public final List<ConfigInfo> getSlaveConfigs() throws IOException {
        checkConfigurePermission();
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final ArrayList<HistoryDescr> values = new ArrayList<HistoryDescr>(
                getHistoryDao().getRevisions(slave).values());
        for (final HistoryDescr historyDescr : values) {
            final String timestamp = historyDescr.getTimestamp();
            final XmlFile oldRevision = getHistoryDao().getOldRevision(slave, timestamp);
            if (oldRevision.getFile() != null) {
                configs.add(ConfigInfo.create(
                        slave.getNodeName(),
                        true,
                        historyDescr,
                        true));
            } else if ("Deleted".equals(historyDescr.getOperation())) {
                configs.add(ConfigInfo.create(
                        slave.getNodeName(),
                        false,
                        historyDescr,
                        true));
            }
        }
        Collections.sort(configs, ParsedDateComparator.DESCENDING);
        return configs;
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
        checkConfigurePermission();
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
        checkConfigurePermission();
        return getHistoryDao().getRevisions(this.slave)
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
        checkConfigurePermission();
        return getHistoryDao().getRevisions(this.slave)
                .get(getTimestamp(timestampNumber)).getOperation();
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
        checkConfigurePermission();
        final String timestamp = getRequestParameter("timestamp");
        final XmlFile xmlFile = getOldConfigXml(timestamp);
        return xmlFile.asString();
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
        rsp.sendRedirect("showDiffFiles?timestamp1=" + parser.get("timestamp1")
                + "&timestamp2=" + parser.get("timestamp2"));
    }


    /**
     * Takes the two timestamp request parameters and returns the diff between the corresponding
     * config files of this slave as a list of single lines.
     *
     * @return Differences between two config versions as list of lines.
     * @throws IOException If diff doesn't work or xml files can't be read.
     */
    public final List<Line> getLines() throws IOException {
        checkConfigurePermission();
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
        checkConfigurePermission();
        final XmlFile oldRevision = getHistoryDao().getOldRevision(slave, timestamp);
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

        final XmlFile xmlFile = getHistoryDao().getOldRevision(slave, timestamp);
        final Slave newSlave = (Slave) Jenkins.XSTREAM2.fromXML(xmlFile.getFile());
        final List<Node> nodes = new ArrayList<Node>();
        nodes.addAll(hudson.getNodes());
        nodes.remove(slave);
        nodes.add(newSlave);
        slave = newSlave;
        hudson.setNodes(nodes);
        rsp.sendRedirect(getHudson().getRootUrl() + slave.toComputer().getUrl());
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

}
