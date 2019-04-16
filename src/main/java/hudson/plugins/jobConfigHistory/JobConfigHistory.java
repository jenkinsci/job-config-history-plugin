/*
 * The MIT License
 *
 * Copyright 2013 John Borghi.
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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletException;
import hudson.Plugin;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Class supporting global configuration settings, along with methods associated
 * with the plugin itself.
 *
 * @author John Borghi
 */
public class JobConfigHistory extends Plugin {

	/** Root directory for storing histories. */
	private String historyRootDir;

	/** Maximum number of configuration history entries to keep. */
	private String maxHistoryEntries;

	/** Maximum number of history entries per site to show. */
	private String maxEntriesPerPage;

	/** Maximum number of days to keep entries. */
	private String maxDaysToKeepEntries;

	private String excludedUsers;

	/**
	 * Flag to indicate if we should save history when it is a duplication of
	 * the previous saved configuration.
	 */
	private boolean skipDuplicateHistory = true;

	/**
	 * Regular expression pattern for 'system' configuration files to exclude
	 * from saving.
	 */
	private String excludePattern;

	/** Compiled regular expression pattern. */
	private transient Pattern excludeRegexpPattern;

	/**
	 * Flag to indicate if we should save the config history of Maven modules.
	 */
	private boolean saveModuleConfiguration = false;

	/**
	 * Whether build badges should appear when the config of a job has changed
	 * since the last build. Three possible settings: Never, always, only for
	 * users with config permission.
	 */
	private String showBuildBadges = "always";

	/** our logger. */
	private static final Logger LOG = Logger.getLogger(JobConfigHistory.class.getName());

	@Override
	public void start() throws Exception {
		load();
		loadRegexpPatterns();
	}

	@Override
	public void configure(StaplerRequest req, JSONObject formData)
			throws IOException, ServletException, FormException {

		historyRootDir = formData.getString("historyRootDir").trim();
		setMaxHistoryEntries(formData.getString("maxHistoryEntries").trim());
		setMaxDaysToKeepEntries(formData.getString("maxDaysToKeepEntries").trim());
		setMaxEntriesPerPage(formData.getString("maxEntriesPerPage").trim());
		skipDuplicateHistory = formData.getBoolean("skipDuplicateHistory");
		excludePattern = formData.getString("excludePattern");
		saveModuleConfiguration = formData.getBoolean("saveModuleConfiguration");
		showBuildBadges = formData.getString("showBuildBadges");
		excludedUsers = formData.getString("excludedUsers");
		save();
		loadRegexpPatterns();
	}

	/**
	 * @return The default history root directory.
	 */
	public String getDefaultRootDir() {
		return JobConfigHistoryConsts.DEFAULT_HISTORY_DIR;
	}

	/**
	 * @return The configured history root directory.
	 */
	public String getHistoryRootDir() {
		return historyRootDir;
	}

	/**
	 * @return The maximum number of history entries to keep.
	 */
	public String getMaxHistoryEntries() {
		return maxHistoryEntries;
	}

	/**
	 * Set the maximum number of history entries per item.
	 *
	 * @param maxEntryInput
	 *            The maximum number of history entries to keep
	 */
	public void setMaxHistoryEntries(String maxEntryInput) {
		String trimmedValue = StringUtils.trimToNull(maxEntryInput);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			maxHistoryEntries = trimmedValue;
		}
	}

	/**
	 * @return The maximum number of history entries to show per site.
	 */
	public String getMaxEntriesPerPage() {
		return maxEntriesPerPage;
	}

	/**
	 * Set the maximum number of history entries to show per site.
	 *
	 * @param maxEntryInput
	 *            The maximum number of history entries to show per site
	 */
	public void setMaxEntriesPerPage(String maxEntryInput) {
		String trimmedValue = StringUtils.trimToNull(maxEntryInput);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			maxEntriesPerPage = trimmedValue;
		}
	}

	/**
	 * @return The maximum number of days to keep history entries.
	 */
	public String getMaxDaysToKeepEntries() {
		return maxDaysToKeepEntries;
	}

	/**
	 * Set allowed age of history entries.
	 *
	 * @param maxDaysInput
	 *            For how long history entries should be kept (in days)
	 */
	public void setMaxDaysToKeepEntries(String maxDaysInput) {
		String trimmedValue = StringUtils.trimToNull(maxDaysInput);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			maxDaysToKeepEntries = trimmedValue;
		}
	}

	/**
	 * Checks if a string evaluates to a positive integer number.
	 *
	 * @param numberString
	 *            The number in question (as String)
	 * @return Whether the number is a positive integer
	 */
	public boolean isPositiveInteger(String numberString) {
		try {
			int number = Integer.parseInt(numberString);
			if (number < 0) {
				throw new NumberFormatException();
			}
			return true;
		} catch (NumberFormatException e) {
			LOG.log(Level.WARNING, "No positive integer: {0}", numberString);
		}
		return false;
	}

	/**
	 * @return True if item group configurations should be saved.
	 * @deprecated since version 2.9
	 */
	@Deprecated
	public boolean getSaveItemGroupConfiguration() {
		return true;
	}

	/**
	 * @return true if we should skip saving history that duplicates the prior
	 *         saved configuration.
	 */
	public boolean getSkipDuplicateHistory() {
		return skipDuplicateHistory;
	}

	/**
	 * Used by the configuration page.
	 *
	 * @return The default regular expression for 'system' file names to exclude
	 *         from saving.
	 */
	public String getDefaultExcludePattern() {
		return JobConfigHistoryConsts.DEFAULT_EXCLUDE;
	}

	/**
	 * @return The regular expression for 'system' file names to exclude from
	 *         saving.
	 */
	public String getExcludePattern() {
		return excludePattern;
	}

	/**
	 * @return true if we should save 'system' configurations.
	 */
	public boolean getSaveModuleConfiguration() {
		return saveModuleConfiguration;
	}

	/**
	 * @return Whether build badges should appear always, never or only for
	 *         users with config rights.
	 */
	public String getShowBuildBadges() {
		return showBuildBadges;
	}

	/**
	 * Used for testing only.
	 *
	 * @param showBuildBadges
	 *            possible values: "never", "always", "userWithConfigPermission" or "adminUser".
	 */
	public void setShowBuildBadges(String showBuildBadges) {
		this.showBuildBadges = showBuildBadges;
	}

	/**
	 * Whether build badges should appear for the builds of this project for
	 * this user.
	 *
	 * @param project
	 *            The project to which the build history belongs.
	 * @return False if the option is set to 'never' or the user doesn't have
	 *         the required permissions.
	 */
	public boolean showBuildBadges(Job<?, ?> project) {
		if ("always".equals(showBuildBadges)) {
			return true;
		}
		if ("userWithConfigPermission".equals(showBuildBadges) && project.hasPermission(Item.CONFIGURE)) {
			return true;
		}
		if ("adminUser".equals(showBuildBadges) && getJenkins().hasPermission(Jenkins.ADMINISTER)) {
			return true;
		}
		return false;
	}

	/**
	 * Used for testing to verify invalid pattern not loaded.
	 *
	 * @return The loaded regexp pattern, or null if pattern was invalid.
	 */
	public Pattern getExcludeRegexpPattern() {
		return excludeRegexpPattern;
	}

	/**
	 * Loads regular expression patterns used by this class.
	 */
	private void loadRegexpPatterns() {
		excludeRegexpPattern = loadRegex(excludePattern);
	}

	/**
	 * Loads a regular expression pattern for the given string.
	 *
	 * @param patternString
	 *            The string representing the regular expression.
	 * @return The {@link Pattern} for the given expression, or null if the
	 *         pattern cannot be loaded.
	 */
	private Pattern loadRegex(String patternString) {
		if (patternString != null) {
			try {
				return Pattern.compile(patternString);
			} catch (PatternSyntaxException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Returns the File object representing the configured root history
	 * directory.
	 *
	 * @return The configured root history File object. from the URI.
	 */
	public File getConfiguredHistoryRootDir() {
		File rootDir;
		File jenkinsHome = getJenkinsHome();

		if (historyRootDir == null) {
			rootDir = new File(jenkinsHome,
					JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
		} else {
			if (historyRootDir.matches("^(/|\\\\|[a-zA-Z]:).*")) {
				rootDir = new File(historyRootDir + "/"
						+ JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
			} else {
				rootDir = new File(jenkinsHome, historyRootDir + "/"
						+ JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
			}
		}
		return rootDir;
	}

	/**
	 * @see FileHistoryDao#getConfigFile(java.io.File)
	 *
	 * @param historyDir
	 *            The history directory to look under.
	 * @return The configuration file or null if no file is found.
	 */
	public File getConfigFile(File historyDir) {
		// TODO: refactor away from 'File'
		return FileHistoryDao.getConfigFile(historyDir);
	}

	/**
	 *
	 * @return comma separated list of usernames, whose changes should not get
	 *         detected.
	 */
	public String getExcludedUsers() {
		return excludedUsers;
	}

	/**
	 * Returns true if configuration for this item should be saved, based on the
	 * plugin settings, the type of item and the configuration file specified.
	 * <p>
	 * If the item is an instance of {@link AbstractProject} or the
	 * configuration file is stored directly in JENKINS_ROOT, it is considered
	 * for saving.
	 *
	 * If the plugin is configured to skip saving duplicated history, we also
	 * evaluate if this configuration duplicates the previous saved history (if
	 * such history exists).
	 *
	 * @param item
	 *            The item whose configuration is under consideration.
	 * @param xmlFile
	 *            The configuration file for the above item.
	 * @return true if the item configuration should be saved.
	 */
	public boolean isSaveable(Saveable item, XmlFile xmlFile) {
		boolean canSave = checkRegex(xmlFile);
		if (!canSave) {
			LOG.log(Level.FINE, "skipped recording change history for job {0}", xmlFile.getFile().getAbsolutePath());
			return false;
		}
		if (item instanceof TopLevelItem) {
			// including FreeStyleProject, WorkflowJob
			return true;
		}
		if (PluginUtils.isMavenPluginAvailable() && item instanceof MavenModule
				&& saveModuleConfiguration) {
			return true;
		}

		return false;
	}

	/**
	 * Check whether config file should not be saved because of regex pattern.
	 *
	 * @param xmlFile
	 *            The config file
	 * @return True if it should be saved
	 */
	private boolean checkRegex(XmlFile xmlFile) {
		if (excludeRegexpPattern != null) {
			String fullPath = xmlFile.getFile().getAbsolutePath();
			Matcher matcher = excludeRegexpPattern.matcher(fullPath);
			return !matcher.find();
		} else {
			return true;
		}
	}

	/**
	 * Validates the user entry for the maximum number of history items to keep.
	 * Must be blank or a non-negative integer.
	 *
	 * @param value
	 *            The form input entered by the user.
	 * @return ok if the entry is blank or a non-negative integer.
	 */
	public FormValidation doCheckMaxHistoryEntries(@QueryParameter String value) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			return FormValidation.ok();
		} else {
			return FormValidation.error("Enter a valid positive integer");
		}
	}

	/**
	 * Validates the user entry for the maximum number of history items to show
	 * per site. Must be blank or a non-negative integer.
	 *
	 * @param value
	 *            The form input entered by the user.
	 * @return ok if the entry is blank or a non-negative integer.
	 */
	public FormValidation doCheckMaxEntriesPerPage(@QueryParameter String value) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			return FormValidation.ok();
		} else {
			return FormValidation.error("Enter a valid positive integer");
		}
	}

	/**
	 * Validates the user entry for the maximum number of days to keep history
	 * items. Must be blank or a non-negative integer.
	 *
	 * @param value
	 *            The form input entered by the user.
	 * @return ok if the entry is blank or a non-negative integer.
	 */
	public FormValidation doCheckMaxDaysToKeepEntries(@QueryParameter String value) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
			return FormValidation.ok();
		} else {
			return FormValidation.error("Enter a valid positive integer");
		}
	}

	/**
	 * Validates the user entry for the regular expression of system file names
	 * to exclude from saving.
	 *
	 * @param value
	 *            The form input entered by the user.
	 * @return ok if the entry is a valid regular expression.
	 */
	public FormValidation doCheckExcludePattern(@QueryParameter String value) {
		try {
			Pattern.compile(value);
			return FormValidation.ok();
		} catch (PatternSyntaxException e) {
			return FormValidation.error("Enter a valid regular expression");
		}
	}

	/**
	 * For tests.
	 *
	 * @return the historyDao
	 */
	protected HistoryDao getHistoryDao() {
		return PluginUtils.getHistoryDao(this);
	}

	/**
	 * For tests.
	 *
	 * @return JENKINS_HOME
	 */
	protected File getJenkinsHome() {
		Jenkins jenkins = Jenkins.getInstance();
		return jenkins.getRootDir();
	}

	/**
	 * For tests.
	 *
	 * @return Jenkins instance.
	 */
	@Deprecated
	public Jenkins getJenkins() {
		return Jenkins.getInstance();
	}
}
