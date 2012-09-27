package hudson.plugins.jobConfigHistory;

/**
 * Holder for constants.
 *
 * @author Stefan Brausch
 */
public final class JobConfigHistoryConsts {

    /**
     * Holder for constants.
     */
    private JobConfigHistoryConsts() {
        // Holder for constants
    }

    /** Path to the jobConfigHistory base. */
    public static final String URLNAME = "jobConfigHistory";

    /** Path to the icon. */
    public static final String ICONFILENAME = "/plugin/jobConfigHistory/img/confighistory.png";

    /** Default directory for storing history. */
    public static final String DEFAULT_HISTORY_DIR = "config-history";
    
    /** Default directory for storing history. */
    public static final String SYSTEM_HISTORY_DIR = "system-history";

    /** name of history xml file. */
    public static final String HISTORY_FILE = "history.xml";

    /** name of history xml file. */
    public static final String DELETED_MARKER = "_deleted_";

    /** Default regexp pattern of configuration files not to save. */
    public static final String DEFAULT_EXCLUDE = "queue|nodeMonitors|UpdateCenter|global-build-stats";

    /** Format for timestamped dirs. */
    public static final String ID_FORMATTER = "yyyy-MM-dd_HH-mm-ss";
}
