/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import hudson.security.Permission;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.plugins.jobConfigHistory.SideBySideView.Line;
import hudson.security.AccessControlled;
import jenkins.model.Jenkins;

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
	 * @param jenkins
	 *            instance
	 * @param project
	 *            for which configurations should be returned.
	 */
	public JobConfigHistoryProjectAction(Jenkins jenkins,
			AbstractItem project) {
		super(jenkins);
		this.project = project;
	}
	/**
	 * {@inheritDoc}
	 *
	 * Make method final, as we always want the same icon file. Returns
	 * {@literal null} to hide the icon if the user is not allowed to configure
	 * jobs.
	 */
	@Override
	public final String getIconFileName() {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			return null;
		}
		if (project instanceof TopLevelItem) {
			return JobConfigHistoryConsts.ICONFILENAME;
		}
		if (getPlugin().getSaveModuleConfiguration()
				&& PluginUtils.isMavenPluginAvailable()
				&& project instanceof MavenModule) {
			return JobConfigHistoryConsts.ICONFILENAME;
		}
		return null;
	}

	/**
	 * Returns the configuration history entries for one {@link AbstractItem}.
	 *
	 * @return history list for one {@link AbstractItem}.
	 * @throws IOException
	 *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be
	 *             read or the path might not be urlencoded.
	 */
	public final List<ConfigInfo> getJobConfigs() throws IOException {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final ArrayList<HistoryDescr> values = new ArrayList<HistoryDescr>(
				getHistoryDao().getRevisions(project.getConfigFile()).values());
		final String maxEntriesPerPageAsString = getPlugin()
				.getMaxEntriesPerPage();
		final int maxEntriesPerPage;
		if (maxEntriesPerPageAsString != null
				&& !maxEntriesPerPageAsString.isEmpty()) {
			maxEntriesPerPage = Math.min(values.size(),
					Integer.parseInt(maxEntriesPerPageAsString));
		} else {
			maxEntriesPerPage = values.size();
		}

		final List<ConfigInfo> configs = toConfigInfoList(values, values.size() - maxEntriesPerPage, values.size());
		Collections.sort(configs, ParsedDateComparator.DESCENDING);
		return configs;
	}

	public final List<ConfigInfo> getJobConfigs(int from, int to) {
		if (from > to) throw new IllegalArgumentException("start index is greater than end index: (" + from + ", " + to + ")");
		final int revisionAmount = getRevisionAmount();
		if (from > revisionAmount) {
			throw new IllegalArgumentException("start index is greater than revision amount: (" + from + ", " + revisionAmount + ")");
			//todo do sth better
		}

		if (to > revisionAmount) {
			to = revisionAmount;
		}

		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return Collections.emptyList();
		}
		//load historydescrs lazily
		final SortedMap<String, HistoryDescr> historyDescrSortedMap = getHistoryDao().getRevisions(project.getConfigFile());
		//todo maybe implement a boolean parameter for ascending order

		//get them values in DESCENDING order (newest revision first)
		ArrayList<HistoryDescr> mapValues = new ArrayList<>(historyDescrSortedMap.values());
		Collections.reverse(mapValues);

		final List<HistoryDescr> cuttedHistoryDescrs = mapValues.subList(from, to);
		//only after selecting the entries to be displayed, the files are read (if the HistoryDao uses LazyHistoryDescr, of course).
		return toConfigInfoList(cuttedHistoryDescrs, 0, cuttedHistoryDescrs.size());
	}

	public int getRevisionAmount() { return  getHistoryDao().getRevisionAmount(project.getConfigFile()); }

	public int getMaxPageNum() {
		String entriesPerPageStr = getCurrentRequest().getParameter("entriesPerPage");
		//TODO magic number!
		int entriesPerPage = (entriesPerPageStr != null && !entriesPerPageStr.equals("")) ? Integer.parseInt(entriesPerPageStr) : 100;
		return getRevisionAmount()/ entriesPerPage;
	}

	public List<Integer> getRelevantPageNums(int currentPageNum) {
		final int maxPageNum = getMaxPageNum();
		//todo good epsilon?
		final int epsilon = 2;
		final HashSet<Integer> pageNumsSet = new HashSet<>();
		pageNumsSet.add(0);
		pageNumsSet.add(maxPageNum);

		if (maxPageNum > 10) {
			pageNumsSet.add(currentPageNum);
			//add everything in epsilon around current pageNum
			for (int i = currentPageNum; i <= Math.min(currentPageNum+epsilon, maxPageNum); i++) {
				pageNumsSet.add(i);
			}
			for (int i = currentPageNum; i >= Math.max(0, currentPageNum-epsilon); i--) {
				pageNumsSet.add(i);
			}
		} else {
			for (int i = 0; i <= maxPageNum; i++) {
				pageNumsSet.add(i);
			}
		}
		ArrayList<Integer> pageNumsList = new ArrayList<>(pageNumsSet);
		pageNumsList.sort(Comparator.naturalOrder());
		//add code for dots:
		int lastNumber = pageNumsList.get(0);
		for (int i = 1; i < pageNumsList.size(); i++) {
			int thisNumber = pageNumsList.get(i);
			System.out.println("it: (" + lastNumber + ", " + thisNumber + ")");
			if (lastNumber+1 != thisNumber) {
				//add dots before thisNumber. -1 stands for dots (easier than defining a special class etc)
				pageNumsList.add(i++, -1);
			}

			lastNumber = thisNumber;
		}
		return pageNumsList;
	}

	private List<ConfigInfo> toConfigInfoList(List<HistoryDescr> historyDescrs, int from, int to) {
		ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
		for (final HistoryDescr historyDescr : historyDescrs.subList(from, to)) {
			final String timestamp = historyDescr.getTimestamp();
			final XmlFile oldRevision = getHistoryDao().getOldRevision(project,
				timestamp);
			if (oldRevision.getFile() != null) {
				configs.add(ConfigInfo.create(project.getFullName(), true,
					historyDescr, true));
			} else if ("Deleted".equals(historyDescr.getOperation())) {
				configs.add(ConfigInfo.create(project.getFullName(), false,
					historyDescr, true));
			}
		}
		return configs;
	}

	/**
	 * Returns the configuration history entries for one {@link AbstractItem}
	 * for the REST API.
	 *
	 * @return history list for one {@link AbstractItem}, or an empty list if
	 *         not authorized.
	 * @throws IOException
	 *             if {@link JobConfigHistoryConsts#HISTORY_FILE} might not be
	 *             read or the path might not be urlencoded.
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
	 * Returns {@link JobConfigHistoryProjectAction#getOldConfigXml(String)} as
	 * String.
	 *
	 * @return content of the {@literal config.xml} found in directory given by
	 *         the request parameter {@literal file}.
	 * @throws IOException
	 *             if the config file could not be read or converted to an xml
	 *             string.
	 */
	public final String getFile() throws IOException {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final String timestamp = getRequestParameter("timestamp");
		final XmlFile xmlFile = getOldConfigXml(timestamp);
		return xmlFile.asString();
	}

	/**
	 * Returns the project for which we want to see the config history, the
	 * config files or the diff.
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
	public AccessControlled getAccessControlledObject() {
		return project;
	}

	@Override
	public void checkConfigurePermission() {
		getAccessControlledObject().checkPermission(Item.CONFIGURE);
	}

	@Override
	protected void checkDeleteEntryPermission() { getAccessControlledObject().checkPermission(JobConfigHistory.DELETEENTRY_PERMISSION); }

	@Override
	public boolean hasAdminPermission() { return getAccessControlledObject().hasPermission(Jenkins.ADMINISTER); }

	@Override
	public boolean hasDeleteEntryPermission() { return getAccessControlledObject().hasPermission(JobConfigHistory.DELETEENTRY_PERMISSION);}

	@Override
	public boolean hasConfigurePermission() {
		return getAccessControlledObject().hasPermission(Item.CONFIGURE);
	}

	public boolean hasReadExtensionPermission() {
		return getAccessControlledObject().hasPermission(Item.EXTENDED_READ);
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
		return getHistoryDao().getRevisions(this.project.getConfigFile())
				.get(getTimestamp(timestampNumber)).getUser();
	}

	public final String getUserID(int timestamp) {
		checkConfigurePermission();
		return getHistoryDao().getRevisions(this.project.getConfigFile())
			.get(getTimestamp(timestamp)).getUserID();
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
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		return getHistoryDao().getRevisions(this.project.getConfigFile())
				.get(getTimestamp(timestampNumber)).getOperation();
	}

	/**
	 * Used in the Difference jelly only. Returns the next timestamp of the next
	 * entry of the two Files A and B. timestampNumber decides which file
	 * exactly.
	 * 
	 * @param timestampNumber
	 *            1 for File A, 2 for File B
	 * @return the timestamp of the next entry as String.
	 */
	public final String getNextTimestamp(int timestampNumber) {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final String timestamp = this
				.getRequestParameter("timestamp" + timestampNumber);
		final SortedMap<String, HistoryDescr> revisions = getHistoryDao()
				.getRevisions(this.project.getConfigFile());
		final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet()
				.iterator();
		while (itr.hasNext()) {
			if (itr.next().getValue().getTimestamp().equals(timestamp)
					&& itr.hasNext()) {
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
	 * @param timestampNumber
	 *            1 for File A, 2 for File B
	 * @return the timestamp of the previous entry as String.
	 */
	public final String getPrevTimestamp(int timestampNumber) {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final String timestamp = this
				.getRequestParameter("timestamp" + timestampNumber);
		final SortedMap<String, HistoryDescr> revisions = getHistoryDao()
				.getRevisions(this.project.getConfigFile());
		final Iterator<Entry<String, HistoryDescr>> itr = revisions.entrySet()
				.iterator();
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
	 * Takes the two timestamp request parameters and returns the diff between
	 * the corresponding config files of this project as a list of single lines.
	 * Filters lines that match the <i>ignoredLinesPattern</i> if wanted.
	 * 
	 * @param hideVersionDiffs determines whether version diffs shall be shown or not.
	 * @return Differences between two config versions as list of lines.
	 * @throws IOException If diff doesn't work or xml files can't be read.
	 */
	public final List<Line> getLines(boolean hideVersionDiffs) throws IOException {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final String timestamp1 = getRequestParameter("timestamp1");
		final String timestamp2 = getRequestParameter("timestamp2");
		return getLines(getOldConfigXml(timestamp1), getOldConfigXml(timestamp2), hideVersionDiffs);
	}

	public XmlSyntaxChecker.Answer checkXmlSyntax(String timestamp) {
		return  XmlSyntaxChecker.check(getOldConfigXml(timestamp).getFile());
	}

	/**
	 * Gets the version of the config.xml that was saved at a certain time.
	 *
	 * @param timestamp
	 *            The timestamp as String.
	 * @return The config file as XmlFile.
	 */
	private XmlFile getOldConfigXml(String timestamp) {
		if (!hasConfigurePermission() && !hasReadExtensionPermission()) {
			checkConfigurePermission();
			return null;
		}
		final XmlFile oldRevision = getHistoryDao().getOldRevision(project,
				timestamp);
		if (oldRevision.getFile() != null) {
			return oldRevision;
		} else {
			throw new IllegalArgumentException(
					"Non existent timestamp " + timestamp);
		}
	}

	/**
	 * Action when 'restore' button is pressed: Replace current config file by
	 * older version.
	 *
	 * @param req
	 *            Incoming StaplerRequest
	 * @param rsp
	 *            Outgoing StaplerResponse
	 * @throws IOException
	 *             If something goes wrong
	 */
	public final void doRestore(StaplerRequest req, StaplerResponse rsp)
			throws IOException {
		checkConfigurePermission();
		final String timestamp = req.getParameter("timestamp");

		final XmlFile xmlFile = getHistoryDao().getOldRevision(project,
				timestamp);
		final InputStream is = new ByteArrayInputStream(
				xmlFile.asString().getBytes(StandardCharsets.UTF_8));

		project.updateByXml((Source) new StreamSource(is));
		project.save();
		rsp.sendRedirect(getJenkins().getRootUrl() + project.getUrl());
	}

	/**
	 * Action when 'restore' button in showDiffFiles.jelly is pressed. Gets
	 * required parameter and forwards to restoreQuestion.jelly.
	 * 
	 * @param req
	 *            StaplerRequest created by pressing the button
	 * @param rsp
	 *            Outgoing StaplerResponse
	 * @throws IOException
	 *             If XML file can't be read
	 */
	public final void doForwardToRestoreQuestion(StaplerRequest req,
			StaplerResponse rsp) throws IOException {
		final String timestamp = req.getParameter("timestamp");
		rsp.sendRedirect("restoreQuestion?timestamp=" + timestamp);
	}

	public final void doDeleteRevision(StaplerRequest req, StaplerResponse rsp) {
		checkDeleteEntryPermission();
		final String timestamp = req.getParameter("timestamp");
		PluginUtils.getHistoryDao().deleteRevision(this.getProject(), timestamp);
		//do nothing with the rsp
	}

	public boolean revisionEqualsCurrent(String timestamp) {
		return PluginUtils.getHistoryDao().revisionEqualsCurrent(this.getProject(), timestamp);
	}

	/**
	 * For tests.
	 *
	 * @return historyDao
	 */
	protected HistoryDao getHistoryDao() {
		return PluginUtils.getHistoryDao();
	}

	public Api getApi() {
		return new Api(this);
	}

	public int getLeadingWhitespace(String str) {
		return str == null ? 0 : str.indexOf(str.trim());
	}
}
