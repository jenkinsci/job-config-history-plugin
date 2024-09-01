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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class supporting global configuration settings, along with methods associated
 * with the plugin itself.
 *
 * @author John Borghi
 */
@Symbol("jobConfigHistory")
@Extension
public class JobConfigHistory extends GlobalConfiguration {

    private static final PermissionGroup PERMISSION_GROUP = new PermissionGroup(JobConfigHistory.class, Messages._displayName());
    protected static final Permission DELETEENTRY_PERMISSION =
            new Permission(
                    PERMISSION_GROUP,
                    Messages.JobConfigHistory_deleteEntryPermission(),
                    Messages._JobConfigHistory_deleteEntryPermissionDescription(),
                    Jenkins.ADMINISTER,
                    true,
                    new PermissionScope[]{PermissionScope.ITEM, PermissionScope.COMPUTER});
    /**
     * our logger.
     */
    private static final Logger LOG = Logger.getLogger(JobConfigHistory.class.getName());
    /**
     * Root directory for storing histories.
     */
    private String historyRootDir;
    /**
     * Maximum number of history entries to keep.
     */
    private String maxHistoryEntries;
    /**
     * Maximum number of history entries to show per page.
     */
    private String maxEntriesPerPage;
    /**
     * Maximum number of days to keep history entries.
     */
    private String maxDaysToKeepEntries;
    /**
     * Comma separated list of usernames whose changes should not get detected.
     */
    private String excludedUsers;
    /**
     * Whether to skip saving history when it is a duplication of the previous saved configuration.
     */
    private boolean skipDuplicateHistory = true;
    /**
     * Regular expression pattern for 'system' configuration files to exclude from saving.
     */
    private String excludePattern = JobConfigHistoryConsts.DEFAULT_EXCLUDE;
    /**
     * Compiled regular expression pattern.
     */
    private transient Pattern excludeRegexpPattern;
    /**
     * Whether to save the config history of Maven modules.
     */
    private boolean saveModuleConfiguration = false;
    /**
     * Whether build badges should appear when the config of a job has changed since the last build.
     * Possible values: "never", "always", "userWithConfigPermission", "adminUser"
     */
    private String showBuildBadges = "always";
    /**
     * Whether a change reason comment window should be shown on a jobs' configure page.
     */
    private boolean showChangeReasonCommentWindow = true;

    @DataBoundConstructor
    @Restricted(NoExternalUse.class)
    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "The Descriptor#load documentation states that \"The constructor of the derived class must call this method\".")
    public JobConfigHistory() {
        load();
        loadRegexpPatterns();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        try (BulkChange bc = new BulkChange(this)) {
            req.bindJSON(this, formData);
            bc.commit();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save " + getConfigFile(), e);
        }
        return true;
    }
    
    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(),"jobConfigHistory.xml"));
    }

    /**
     * Gets the default root directory for storing histories.
     *
     * @return The default root directory.
     */
    public String getDefaultRootDir() {
        return JobConfigHistoryConsts.DEFAULT_HISTORY_DIR;
    }

    /**
     * Gets the root directory for storing histories.
     *
     * @return The root directory.
     */
    public String getHistoryRootDir() {
        return historyRootDir;
    }
    
    /**
     * Sets the root directory for storing histories.
     *
     * @param historyRootDir The root directory.
     */
    @DataBoundSetter
    public void setHistoryRootDir(String historyRootDir) {
        this.historyRootDir = historyRootDir;
        save();
    }

    /**
     * Gets the maximum number of history entries to keep.
     *
     * @return The maximum number of history entries to keep.
     */
    public String getMaxHistoryEntries() {
        return maxHistoryEntries;
    }

    /**
     * Sets the maximum number of history entries to keep.
     *
     * @param maxEntryInput The maximum number of history entries to keep.
     */
    @DataBoundSetter
    public void setMaxHistoryEntries(String maxEntryInput) {
        String trimmedValue = StringUtils.trimToNull(maxEntryInput);
        if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
            maxHistoryEntries = trimmedValue;
        }
        save();
    }

    /**
     * Gets the maximum number of history entries to show per page.
     *
     * @return The maximum number of history entries to show per page.
     */
    public String getMaxEntriesPerPage() {
        return maxEntriesPerPage;
    }

    /**
     * Sets the maximum number of history entries to show per page.
     *
     * @param maxEntryInput The maximum number of history entries to show per page.
     */
    @DataBoundSetter
    public void setMaxEntriesPerPage(String maxEntryInput) {
        String trimmedValue = StringUtils.trimToNull(maxEntryInput);
        if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
            maxEntriesPerPage = trimmedValue;
        }
        save();
    }

    /**
     * Gets the maximum number of days to keep history entries.
     *
     * @return The maximum number of days to keep history entries.
     */
    public String getMaxDaysToKeepEntries() {
        return maxDaysToKeepEntries;
    }

    /**
     * Sets the maximum number of days to keep history entries.
     *
     * @param maxDaysInput The maximum number of days to keep history entries.
     */
    @DataBoundSetter
    public void setMaxDaysToKeepEntries(String maxDaysInput) {
        String trimmedValue = StringUtils.trimToNull(maxDaysInput);
        if (trimmedValue == null || isPositiveInteger(trimmedValue)) {
            maxDaysToKeepEntries = trimmedValue;
        }
        save();
    }

    /**
     * Checks if a string evaluates to a positive integer number.
     *
     * @param numberString The number in question (as String)
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
     * Gets whether to skip saving history when it is a duplication of the previous saved configuration.
     *
     * @return Whether to skip saving history when it is a duplication of the previous saved configuration.
     */
    public boolean getSkipDuplicateHistory() {
        return skipDuplicateHistory;
    }

    /**
     * Sets whether to skip saving history when it is a duplication of the previous saved configuration.
     *
     * @param skipDuplicateHistory Whether to skip saving history when it is a duplication of the previous saved 
     *                             configuration.
     */
    @DataBoundSetter
    public void setSkipDuplicateHistory(boolean skipDuplicateHistory) {
        this.skipDuplicateHistory = skipDuplicateHistory;
        save();
    }

    /**
     * Gets the regular expression pattern for 'system' configuration files to exclude from saving.
     *
     * @return The regular expression pattern.
     */
    public String getExcludePattern() {
        return excludePattern;
    }
    
    /**
     * Sets the regular expression pattern for 'system' configuration files to exclude from saving.
     * 
     * @param excludePattern The regular expression pattern.
     */
    @DataBoundSetter
    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
        loadRegexpPatterns();
        save();
    }

    /**
     * Gets whether to save the config history of Maven modules.
     *
     * @return Whether to save the config history of Maven modules.
     */
    public boolean getSaveModuleConfiguration() {
        return saveModuleConfiguration;
    }

    /**
     * Sets whether to save the config history of Maven modules.
     * 
     * @param saveModuleConfiguration Whether to save the config history of Maven modules.
     */
    @DataBoundSetter
    public void setSaveModuleConfiguration(boolean saveModuleConfiguration) {
        this.saveModuleConfiguration = saveModuleConfiguration;
        save();
    }

    /**
     * Gets whether build badges should appear when the config of a job has changed since the last build.
     *
     * @return Possible value: "never", "always", "userWithConfigPermission", "adminUser"
     */
    public String getShowBuildBadges() {
        return showBuildBadges;
    }

    /**
     * Sets whether build badges should appear when the config of a job has changed since the last build.
     *
     * @param showBuildBadges Possible values: "never", "always", "userWithConfigPermission", "adminUser"
     */
    @DataBoundSetter
    public void setShowBuildBadges(String showBuildBadges) {
        this.showBuildBadges = showBuildBadges;
        save();
    }

    /**
     * Gets whether a change reason comment window should be shown on a jobs' configure page.
     *
     * @return Whether a comment window should be shown.
     */
    public boolean getShowChangeReasonCommentWindow() {
        return showChangeReasonCommentWindow;
    }
    
    /**
     * Sets whether a change reason comment window should be shown on a jobs' configure page.
     * 
     * @param showChangeReasonCommentWindow Whether a comment window should be shown.
     */
    @DataBoundSetter
    public void setShowChangeReasonCommentWindow(boolean showChangeReasonCommentWindow) {
        this.showChangeReasonCommentWindow = showChangeReasonCommentWindow;
        save();
    }

    /**
     * Whether build badges should appear for the builds of this project.
     *
     * @param project The project to which the build history belongs.
     * @return False if the option is set to 'never' or the user doesn't have
     * the required permissions.
     */
    public boolean showBuildBadges(Job<?, ?> project) {
        if ("always".equals(showBuildBadges)) {
            return true;
        }
        if ("userWithConfigPermission".equals(showBuildBadges) && project.hasPermission(Item.CONFIGURE)) {
            return true;
        }
        return "adminUser".equals(showBuildBadges) && getJenkins().hasPermission(Jenkins.ADMINISTER);
    }

    /**
     * Gets the regular expression pattern used by this class.
     *
     * @return The loaded regexp pattern, or null if pattern was invalid.
     */
    public Pattern getExcludeRegexpPattern() {
        return excludeRegexpPattern;
    }

    /**
     * Loads the regular expression pattern used by this class.
     */
    private void loadRegexpPatterns() {
        excludeRegexpPattern = loadRegex(excludePattern);
    }

    /**
     * Loads a regular expression pattern for the given string.
     *
     * @param patternString The string representing the regular expression.
     * @return The {@link Pattern} for the given expression, or null if the
     * pattern cannot be loaded.
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
     * @param historyDir The history directory to look under.
     * @return The configuration file or null if no file is found.
     * @see FileHistoryDao#getConfigFile(java.io.File)
     */
    public File getConfigFile(File historyDir) {
        // TODO: refactor away from 'File'
        return FileHistoryDao.getConfigFile(historyDir);
    }

    /**
     * Gets usernames whose changes should not get detected.
     *
     * @return Comma separated list of usernames.
     */
    public String getExcludedUsers() {
        return excludedUsers;
    }
    
    /**
     * Sets usernames whose changes should not get detected.
     *
     * @param excludedUsers Comma separated list of usernames.
     */
    @DataBoundSetter
    public void setExcludedUsers(String excludedUsers) {
        this.excludedUsers = excludedUsers;
        save();
    }

    /**
     * Returns true if configuration for this item should be saved, based on the
     * plugin settings, the type of item and the configuration file specified.
     * <p>
     * If the item is an instance of {@link AbstractProject} or the
     * configuration file is stored directly in JENKINS_ROOT, it is considered
     * for saving.
     * <p>
     * If the plugin is configured to skip saving duplicated history, we also
     * evaluate if this configuration duplicates the previous saved history (if
     * such history exists).
     *
     * @param item    The item whose configuration is under consideration.
     * @param xmlFile The configuration file for the above item.
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
        if (xmlFile.getFile().getParentFile().equals(getJenkinsHome())) {
            // system configs
            return canSave;
        }
        return PluginUtils.isMavenPluginAvailable() && item instanceof MavenModule
                && saveModuleConfiguration;
    }

    /**
     * Checks whether the config file should not be saved because of regex pattern.
     *
     * @param xmlFile The config file.
     * @return True if it should be saved.
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
     * @param value The form input entered by the user.
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
     * per page. Must be blank or a non-negative integer.
     *
     * @param value The form input entered by the user.
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
     * @param value The form input entered by the user.
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
     * @param value The form input entered by the user.
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
     * @return The history DAO.
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
        return Jenkins.get().getRootDir();
    }

    /**
     * For tests.
     *
     * @return The Jenkins instance.
     */
    @Deprecated
    public Jenkins getJenkins() {
        return Jenkins.get();
    }
}
