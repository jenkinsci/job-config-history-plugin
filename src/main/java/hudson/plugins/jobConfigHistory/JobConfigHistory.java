package hudson.plugins.jobConfigHistory;

import hudson.Plugin;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.Saveable;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Class supporting global configuration settings, along with methods
 * associated with the plugin itself.
 *
 * @author jborghi
 *
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

    /**
     * Flag to indicate we should save 'system' level configurations. A 'system' level configuration is defined as one stored
     * directly under the HUDSON_ROOT directory.
     *
     * @deprecated As of version 2.5 this configuration option is deprecated but left here to avoid unmarshalling problems with
     * older settings.
     */
    @Deprecated
    private transient boolean saveSystemConfiguration; //NOPMD

    /** Flag to indicate ItemGroups configuration is saved as well. */
    private boolean saveItemGroupConfiguration;

    /** Flag to indicate if we should save history when it
     *  is a duplication of the previous saved configuration.
     */
    private boolean skipDuplicateHistory = true;

    /** Regular expression pattern for 'system' configuration
     *  files to exclude from saving.
     */
    private String excludePattern;

    /** Compiled regular expression pattern. */
    private transient Pattern excludeRegexpPattern;

    /** Flag to indicate if we should save the config history of Maven modules. */
    private boolean saveModuleConfiguration = true;

    /**
     * Whether build badges should appear when the config of a job has changed since the last build.
     * Three possible settings: Never, always, only for users with config permission.
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
        saveItemGroupConfiguration = formData.getBoolean("saveItemGroupConfiguration");
        skipDuplicateHistory = formData.getBoolean("skipDuplicateHistory");
        excludePattern = formData.getString("excludePattern");
        saveModuleConfiguration = formData.getBoolean("saveModuleConfiguration");
        showBuildBadges = formData.getString("showBuildBadges");
        save();
        loadRegexpPatterns();
    }

    /**
     * @return The configured history root directory.
     */
    public String getHistoryRootDir() {
        return historyRootDir;
    }

    /**
     * @return The default history root directory.
     */
    public String getDefaultRootDir() {
        return JobConfigHistoryConsts.DEFAULT_HISTORY_DIR;
    }

    /**
     * @return The maximum number of history entries to keep.
     */
    public String getMaxHistoryEntries() {
        return maxHistoryEntries;
    }

    /**
     * Set the maximum number of history entries per item.
     * @param maxEntryInput
     *        The maximum number of history entries to keep
     */
    protected void setMaxHistoryEntries(String maxEntryInput) {
        if (maxEntryInput.isEmpty() || isPositiveInteger(maxEntryInput)) {
            maxHistoryEntries = maxEntryInput;
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
     * @param maxEntryInput
     *        The maximum number of history entries to show per site
     */
    protected void setMaxEntriesPerPage(String maxEntryInput) {
        if (maxEntryInput.isEmpty() || isPositiveInteger(maxEntryInput)) {
            maxEntriesPerPage = maxEntryInput;
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
     * @param maxDaysInput
     *        For how long history entries should be kept (in days)
     */
    void setMaxDaysToKeepEntries(final String maxDaysInput) {
        if (maxDaysInput.isEmpty() || isPositiveInteger(maxDaysInput)) {
            maxDaysToKeepEntries = maxDaysInput;
        }
    }

    /**
     * Checks if a string evaluates to a positive integer number.
     *
     * @param numberString The number in question (as String)
     * @return Whether the number is a positive integer
     *
     */
    boolean isPositiveInteger(String numberString) {
        try {
            final int number = Integer.parseInt(numberString);
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
     */
    public boolean getSaveItemGroupConfiguration() {
        return saveItemGroupConfiguration;
    }

    /**
     * @return true if we should skip saving history that duplicates the prior saved configuration.
     */
    public boolean getSkipDuplicateHistory() {
        return skipDuplicateHistory;
    }

    /**
     * @return The regular expression for 'system' file names to exclude from saving.
     */
    public String getExcludePattern() {
        return excludePattern;
    }

    /**
     * Used by the configuration page.
     * @return The default regular expression for 'system' file names to exclude from saving.
     */
    public String getDefaultExcludePattern() {
        return JobConfigHistoryConsts.DEFAULT_EXCLUDE;
    }

    /**
     * @return true if we should save 'system' configurations.
     */
    public boolean getSaveModuleConfiguration() {
        return saveModuleConfiguration;
    }

    /**
     * @return Whether build badges should appear always, never or only for users with config rights.
     */
    public String getShowBuildBadges() {
        return showBuildBadges;
    }

    /**
     * Used for testing only.
     * @param showBadges Never, always, userWithConfigPermission or adminUser.
     */
    public void setShowBuildBadges(String showBadges) {
        showBuildBadges = showBadges;
    }

    /**
     * Whether build badges should appear for the builds of this project for this user.
     *
     * @param project The project to which the build history belongs.
     * @return False if the option is set to 'never' or the user doesn't have the required permissions.
     */
    boolean showBuildBadges(AbstractProject<?, ?> project) {
        if ("always".equals(showBuildBadges)) {
            return true;
        } else if ("userWithConfigPermission".equals(showBuildBadges) && project.hasPermission(AbstractProject.CONFIGURE)) {
            return true;
        } else if ("adminUser".equals(showBuildBadges) && getJenkins().hasPermission(Jenkins.ADMINISTER)) {
            return true;
        }
        return false;
    }

    /**
     * Used for testing to verify invalid pattern not loaded.
     * @return The loaded regexp pattern, or null if pattern was invalid.
     */
    protected Pattern getExcludeRegexpPattern() {
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
     * @param patternString
     *            The string representing the regular expression.
     * @return The {@link Pattern} for the given expression, or null if
     *         the pattern cannot be loaded.
     */
    private Pattern loadRegex(final String patternString) {
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
     * Returns the File object representing the configured root history directory.
     *
     * @return The configured root history File object.
     */
    protected File getConfiguredHistoryRootDir() {
        File rootDir;
        final File jenkinsHome = getJenkinsHome();

        if (historyRootDir == null || historyRootDir.isEmpty()) {
            rootDir = new File(jenkinsHome, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
        } else {
            if (historyRootDir.matches("^(/|\\\\|[a-zA-Z]:).*")) {
                rootDir = new File(historyRootDir + "/" + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
            } else {
                rootDir = new File(jenkinsHome, historyRootDir + "/"
                            + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
            }
        }
        return rootDir;
    }

    /**
     * @see FileHistoryDao#getConfigFile(java.io.File).
     *
     * @param historyDir
     *            The history directory to look under.
     * @return The configuration file or null if no file is found.
     */
    protected File getConfigFile(final File historyDir) {
        return FileHistoryDao.getConfigFile(historyDir);
    }

    /**
     * Returns true if configuration for this item should be saved, based on the
     * plugin settings, the type of item and the configuration file specified.
     *
     * <p>
     * If the item is an instance of {@link AbstractProject} or the configuration
     * file is stored directly in HUDSON_ROOT, it is considered for saving.
     *
     * If the plugin is configured to skip saving duplicated history, we also evaluate
     * if this configuration duplicates the previous saved history (if such history exists).
     *
     * @param item
     *            The item whose configuration is under consideration.
     * @param xmlFile
     *            The configuration file for the above item.
     * @return true if the item configuration should be saved.
     */
    boolean isSaveable(final Saveable item, final XmlFile xmlFile) {
        boolean saveable = false;
        if (item instanceof AbstractProject<?, ?>) {
            saveable = true;
        } else if (xmlFile.getFile().getParentFile().equals(getJenkinsHome())) {
            saveable = checkRegex(xmlFile);
        } else if (saveItemGroupConfiguration && item instanceof ItemGroup) {
            saveable = true;
        }
        if (item instanceof MavenModule && !saveModuleConfiguration) {
            saveable = false;
        }
        return saveable;
    }

    /**
     * Check whether config file should not be saved because of regex pattern.
     * @param xmlFile The config file
     * @return True if it should be saved
     */
    private boolean checkRegex(final XmlFile xmlFile) {
        if (excludeRegexpPattern != null) {
            final Matcher matcher = excludeRegexpPattern.matcher(xmlFile.getFile().getName());
            return !matcher.find();
        } else {
            return true;
        }
    }



    /**
     * Validates the user entry for the maximum number of history items to keep.
     * Must be blank or a non-negative integer.
     * @param value
     *            The form input entered by the user.
     * @return ok if the entry is blank or a non-negative integer.
     */
    public FormValidation doCheckMaxHistoryEntries(@QueryParameter final String value) {
        try {
            if (StringUtils.isNotBlank(value) && Integer.parseInt(value) < 0) {
                throw new NumberFormatException();
            }
            return FormValidation.ok();
        } catch (NumberFormatException ex) {
            return FormValidation.error("Enter a valid positive integer");
        }
    }
    
    /**
     * Validates the user entry for the maximum number of history items to show per site.
     * Must be blank or a non-negative integer.
     * @param value
     *            The form input entered by the user.
     * @return ok if the entry is blank or a non-negative integer.
     */
    public FormValidation doCheckMaxEntriesPerSite(@QueryParameter final String value) {
        try {
            if (StringUtils.isNotBlank(value) && Integer.parseInt(value) < 0) {
                throw new NumberFormatException();
            }
            return FormValidation.ok();
        } catch (NumberFormatException ex) {
            return FormValidation.error("Enter a valid positive integer");
        }
    }

    /**
     * Validates the user entry for the maximum number of days to keep history items.
     * Must be blank or a non-negative integer.
     * @param value
     *            The form input entered by the user.
     * @return ok if the entry is blank or a non-negative integer.
     */
    public FormValidation doCheckMaxDaysToKeepEntries(@QueryParameter final String value) {
        try {
            if (StringUtils.isNotBlank(value) && Integer.parseInt(value) < 0) {
                throw new NumberFormatException();
            }
            return FormValidation.ok();
        } catch (NumberFormatException ex) {
            return FormValidation.error("Enter a valid positive integer");
        }
    }

    /**
     * Validates the user entry for the regular expression of system file names
     * to exclude from saving.
     * @param value
     *            The form input entered by the user.
     * @return ok if the entry is a valid regular expression.
     */
    public FormValidation doCheckExcludePattern(@QueryParameter final String value) {
        try {
            Pattern.compile(value);
            return FormValidation.ok();
        } catch (PatternSyntaxException e) {
            return FormValidation.error("Invalid regexp:\n" + e);
        }
    }

    /**
     * For tests.
     * @return the historyDao
     */
    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao(this);
    }

    /**
     * For tests.
     * @return JENKINS_HOME
     */
    File getJenkinsHome() {
        return Hudson.getInstance().root;
    }

    /**
     * For tests.
     * @return Jenkins instance.
     */
    Jenkins getJenkins() {
        return Hudson.getInstance();
    }

}
