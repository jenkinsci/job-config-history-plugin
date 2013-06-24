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
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
    
    /** Maximum number of days to keep entries. */
    private String maxDaysToKeepEntries;
    
    /** Flag to indicate we should save 'system' level configurations
     *  A 'system' level configuration is defined as one stored directly
     *  under the HUDSON_ROOT directory.
     */
    private boolean saveSystemConfiguration;

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

    /**
     * A filter to return only those directories of a file listing
     * that represent configuration history directories.
     */
    public static final FileFilter HISTORY_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return isHistoryDir(file);
        }
    };

    /**
     * A filter to return only those directories of a file listing
     * that represent deleted jobs history directories.
     */
    public static final FileFilter DELETED_FILTER = new FileFilter() {
        public boolean accept(File file) {
            return (file.getName().contains(JobConfigHistoryConsts.DELETED_MARKER));
        }
    };

   
    @Override 
    public void start() throws Exception {
        load();
        loadRegexpPatterns();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData)
        throws IOException, ServletException, FormException {

        historyRootDir = formData.getString("historyRootDir").trim();
        maxHistoryEntries = formData.getString("maxHistoryEntries").trim();
        maxDaysToKeepEntries = formData.getString("maxDaysToKeepEntries").trim();
        saveSystemConfiguration = formData.getBoolean("saveSystemConfiguration");
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
     * This method is for convenience in testing.
     * @param maxHistoryEntries
     *        The maximum number of history entries to keep
     */
    protected void setMaxHistoryEntries(final String maxHistoryEntries) {
        this.maxHistoryEntries = maxHistoryEntries;
    }
    
    /**
     * @return The maximum number of days to keep history entries.
     */
    public String getMaxDaysToKeepEntries() {
        return maxDaysToKeepEntries;
    }

    /**
     * This method is for convenience in testing.
     * @param maxDays
     *        For how long history entries should be kept (in days)
     */
    protected void setMaxDaysToKeepEntries(final String maxDays) {
        this.maxDaysToKeepEntries = maxDays;
    }
    
    /**
     * @return true if we should save 'system' configurations.
     */
    public boolean getSaveSystemConfiguration() {
        return saveSystemConfiguration;
    }

    /**
     * Used for testing only.
     * @param bool True if system configuration should be saved.
     */
    public void setSaveSystemConfiguration(boolean bool) {
        saveSystemConfiguration = bool;
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
        } else if ("adminUser".equals(showBuildBadges) && Hudson.getInstance().hasPermission(Jenkins.ADMINISTER)) {
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
     * Returns the File object representing the job history directory,
     * which is for reasons of backwards compatibility either a sibling or child
     * of the configured history root dir.
     *
     * @return The job history File object.
     */
    protected File getJobHistoryRootDir() {
        File rootDir;
 
        if (historyRootDir == null || historyRootDir.isEmpty()) {
            //ROOT/config-history/jobs
            rootDir = new File(getConfiguredHistoryRootDir() + "/" + JobConfigHistoryConsts.JOBS_HISTORY_DIR);
        } else {
            //ROOT/MYNAME/jobs -> backwards compatibility
            rootDir = new File(getConfiguredHistoryRootDir().getParent() + "/" + JobConfigHistoryConsts.JOBS_HISTORY_DIR);
        }
        return rootDir;
    }

    
    /**
     * Returns the File object representing the configured root history directory.
     *
     * @return The configured root history File object.
     */
    protected File getConfiguredHistoryRootDir() {
        File rootDir;
 
        if (historyRootDir == null || historyRootDir.isEmpty()) {
            rootDir = new File(Hudson.getInstance().root.getPath() + "/" + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
        } else {
            if (historyRootDir.matches("^(/|\\\\|[a-zA-Z]:).*")) {
                rootDir = new File(historyRootDir + "/" + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
            } else {
                rootDir = new File(Hudson.getInstance().root.getPath() + "/" + historyRootDir + "/" 
                            + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
            }
        }
        return rootDir;
    }

    /**
     * Returns the configuration history directory for the given configuration file.
     *
     * @param xmlFile
     *            The configuration file whose content we are saving.
     * @return The base directory where to store the history, 
     *         or null if the file is not a valid Hudson configuration file.
     */
    protected File getHistoryDir(final XmlFile xmlFile) {
        final String configRootDir = xmlFile.getFile().getParent();
        final String hudsonRootDir = Hudson.getInstance().root.getPath();

        if (!configRootDir.startsWith(hudsonRootDir)) {
            LOG.warning("Trying to get history dir for object outside of HUDSON: " + xmlFile);
            return null;
        }

        //if the file is stored directly under HUDSON_ROOT, it's a system config 
        //so create a distinct directory
        String underRootDir = null;
        if (configRootDir.equals(hudsonRootDir)) {
            final String xmlFileName = xmlFile.getFile().getName();
            underRootDir = xmlFileName.substring(0, xmlFileName.lastIndexOf('.'));
        }
         
        File historyDir;
        if (underRootDir == null) {
            final String remainingPath = configRootDir.substring(hudsonRootDir.length() 
                                        + JobConfigHistoryConsts.JOBS_HISTORY_DIR.length() + 1);
            historyDir = new File(getJobHistoryRootDir(), remainingPath);
        } else {
            historyDir = new File(getConfiguredHistoryRootDir(), underRootDir);
        }

        return historyDir;
    }

    /**
     * Returns the configuration data file stored in the specified history directory.
     * It looks for a file with an 'xml' extension that is not named 
     * {@link JobConfigHistoryConsts#HISTORY_FILE}.
     * <p>
     * Relies on the assumption that random '.xml' files
     * will not appear in the history directories.
     * <p>
     * Checks that we are in an actual 'history directory' to prevent use for
     * getting random xml files.
     * @param historyDir
     *            The history directory to look under.
     * @return The configuration file or null if no file is found.
     */
    protected File getConfigFile(final File historyDir) {
        File configFile = null;
        if (historyDir.exists() && isHistoryDir(historyDir)) {
            // get the *.xml file that is not the JobConfigHistoryConsts.HISTORY_FILE
            // assumes random .xml files won't appear in the history directory
            final File[] listing = historyDir.listFiles();
            for (final File file : listing) {
                if (!file.getName().equals(JobConfigHistoryConsts.HISTORY_FILE) && file.getName().matches(".*\\.xml$")) {
                    configFile = file;
                    break;
                }
            }
        }
        return configFile;
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
    protected boolean isSaveable(final Saveable item, final XmlFile xmlFile) {
        boolean saveable = false;
        if (item instanceof AbstractProject<?, ?>) {
            saveable = true;
        } else if (saveSystemConfiguration && xmlFile.getFile().getParentFile().equals(Hudson.getInstance().root)) {
            saveable = checkRegex(xmlFile);
        } else if (saveItemGroupConfiguration && item instanceof ItemGroup) {
            saveable = true;
        }
        if (item instanceof MavenModule && !saveModuleConfiguration) {
            saveable = false;
        }
        if (saveable) {
            saveable = checkDuplicate(xmlFile);
        }
        
        return saveable;
    }

    /**
     * Checks whether the configuration file should not be saved because it's a duplicate.
     * @param xmlFile The config file
     * @return True if it should be saved
     */
    private boolean checkDuplicate(final XmlFile xmlFile) {
        if (skipDuplicateHistory && hasDuplicateHistory(xmlFile)) {
            LOG.log(Level.FINE, "found duplicate history, skipping save of {0}", xmlFile);
            return false;
        } else {
            return true;
        }
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
     * Determines if the {@link XmlFile} contains a duplicate of
     * the last saved information, if there is previous history.
     *
     * @param xmlFile
     *           The {@link XmlFile} configuration file under consideration.
     * @return true if previous history is accessible, and the file duplicates the previously saved information.
     */
    private boolean hasDuplicateHistory(XmlFile xmlFile) {
        boolean isDuplicated = false;

        final File[] historyDirs = getHistoryDir(xmlFile).listFiles(HISTORY_FILTER);
        if (historyDirs != null && historyDirs.length != 0) {
            Arrays.sort(historyDirs, Collections.reverseOrder());
            final File lastFile = new File(historyDirs[0], xmlFile.getFile().getName());
            if (lastFile.exists()) {
                final XmlFile lastXmlFile = new XmlFile(lastFile);
                try {
                    if (xmlFile.asString().equals(lastXmlFile.asString())) {
                        isDuplicated = true;
                    }
                } catch (IOException e) {
                    LOG.warning("unable to check for duplicate previous history file: " + lastXmlFile + "\n" + e);
                }
            }
        }
        return isDuplicated;
    }

    /**
     * Checks if we should purge old history entries under the specified root
     * using the {@code maxHistoryEntries} value as the criteria, and if required
     * calls the appropriate method to perform the purge.
     *
     * @param itemHistoryRoot
     *            The directory to consider purging history under.
     */
    protected void checkForPurgeByQuantity(final File itemHistoryRoot) {
        int maxEntries = 0;
        if (StringUtils.isNotEmpty(maxHistoryEntries)) {
            try {
                maxEntries = new Integer(getMaxHistoryEntries());
                maxEntries = Integer.parseInt(getMaxHistoryEntries());
                if (maxEntries < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                LOG.warning("maximum number of history entries not formatted properly, unable to purge: " + maxHistoryEntries);
            }
        }
        if (maxEntries > 0) {
            LOG.fine("checking for history files to purge (" + maxHistoryEntries + " max allowed)");
            purgeHistoryByQuantity(itemHistoryRoot, maxEntries);
        }
    }

    /**
     * Performs the actual purge of history entries.
     * @param historyRoot
     *            The directory to purge entries from.
     * @param maxEntries
     *            The maximum number of history entries to keep.
     */
    private void purgeHistoryByQuantity(final File historyRoot, final int maxEntries) {
        // we are about to create a new history entry, so 
        // subtract 1 from the maximum configured to save.
        final int entriesToLeave = maxEntries - 1;
        final File[] historyDirs = historyRoot.listFiles(HISTORY_FILTER);
        if (historyDirs != null && historyDirs.length >= entriesToLeave) {
            Arrays.sort(historyDirs, Collections.reverseOrder());
            for (int i = entriesToLeave; i < historyDirs.length; i++) {
                LOG.fine("purging old directory from history logs: " + historyDirs[i]);
                for (File file : historyDirs[i].listFiles()) {
                    if (!file.delete()) {
                        LOG.warning("problem deleting history file: " + file);
                    }
                }
                if (!historyDirs[i].delete()) {
                    LOG.warning("problem deleting history directory: " + historyDirs[i]);
                }
            }
        }
    }

    /**
     * Determines if the specified {@code dir} stores history information.
     *
     * @param dir
     *            The directory under consideration.
     * @return true if this directory contains a {@link JobConfigHistoryConsts#HISTORY_FILE} file.
     */
    private static boolean  isHistoryDir(File dir) {
        return (new File(dir, JobConfigHistoryConsts.HISTORY_FILE)).exists();
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
}
