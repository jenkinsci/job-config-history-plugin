package hudson.plugins.jobConfigHistory;

import hudson.Plugin;
import hudson.XmlFile;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

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
    /** Root directory for storing configuration history. */
    private String historyRootDir;

    /** Maximum number of configuration history entries to keep. */
    private String maxHistoryEntries;

    /** Flag to indicate we should save 'system' level configurations
     *  A 'system' level configuration is defined as one stored directly
     *  under the HUDSON_ROOT directory.
     */
    private boolean saveSystemConfiguration;

    /** Flag to indicated ItemGroups configuration is saved as well */
    private boolean saveItemGroupConfiguration;

    /** Flag to indicate if we should save history when it 
     *  is a duplication of the previous saved configuration.
     */
    private boolean skipDuplicateHistory;

    /** Regular expression pattern for 'system' configuration
     *  files to exclude from saving.
     */
    private String excludePattern;

    /** Compiled regular expression pattern. */
    private transient Pattern excludeRegexpPattern;

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
        saveSystemConfiguration = formData.getBoolean("saveSystemConfiguration");
        saveItemGroupConfiguration = formData.getBoolean("saveItemGroupConfiguration");
        skipDuplicateHistory = formData.getBoolean("skipDuplicateHistory");
        excludePattern = formData.getString("excludePattern");
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
     * @return true if we should save 'system' configurations.
     */
    public boolean getSaveSystemConfiguration() {
        return saveSystemConfiguration;
    }

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
     * @return The configured root history File object, or null if this configuration has not been set.
     */
    protected File getConfiguredHistoryRootDir() {
        File rootFile = null;
        if (StringUtils.isNotBlank(historyRootDir)) {
            if (historyRootDir.matches("^(/|\\\\|[a-zA-Z]:).*")) {
                rootFile = new File(historyRootDir);
            } else {
                rootFile = new File(Hudson.getInstance().root.getPath() + "/" + historyRootDir);
            }
        }
        return rootFile;
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

        // if the file is stored directly under HUDSON_ROOT, create a distinct directory
        String underRootDir = null;
        if (configRootDir.equals(hudsonRootDir)) {
            final String xmlFileName = xmlFile.getFile().getName();
            underRootDir = JobConfigHistoryConsts.DEFAULT_HISTORY_DIR + "/" 
                + xmlFileName.substring(0, xmlFileName.lastIndexOf('.'));
        }

        File historyDir;
        final File actualHistoryRoot = getConfiguredHistoryRootDir();
        if (actualHistoryRoot == null) {
            if (underRootDir == null) {
                historyDir = new File(configRootDir, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
            } else {
                historyDir = new File(configRootDir, underRootDir);
            }
        } else {
            if (underRootDir == null) {
                final String remainingPath = configRootDir.substring(hudsonRootDir.length() + 1);
                historyDir = new File(actualHistoryRoot, remainingPath);
            } else {
                historyDir = new File(actualHistoryRoot, underRootDir);
            }
        }
        return historyDir;
    }

    /**
     * Returns the directory for storing system configurations.
     *
     * @return The directory used for storing system configurations.
     */

    protected File getSystemHistoryDir() {
        final File actualHistoryRoot = getConfiguredHistoryRootDir();
        if (actualHistoryRoot != null) {
            return new File(actualHistoryRoot, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
        } else {
            return new File(Hudson.getInstance().root, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR);
        }
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
            if (excludeRegexpPattern != null) {
                final Matcher matcher = excludeRegexpPattern.matcher(xmlFile.getFile().getName());
                saveable = !matcher.find();
            } else {
                saveable = true;
            }
        } else if (saveItemGroupConfiguration && item instanceof ItemGroup) {
            saveable = true;
        }
        if (saveable && skipDuplicateHistory && hasDuplicateHistory(xmlFile)) {
            LOG.fine("found duplicate history, skipping save of " + xmlFile);
            saveable = false;
        }
        return saveable;
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
     * Determines if the specified {@code dir} stores
     * history information.  This is needed as Hudson creates
     * 'modules' directories under the 'job' folder, and these will
     * be mixed in with the timestamped history configurations.
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
