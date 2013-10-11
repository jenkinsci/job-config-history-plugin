package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Holder object for displaying information.
 *
 *
 * @author Stefan Brausch
 */
@ExportedBean(defaultVisibility = 999)
public class ConfigInfo implements ParsedDate {

    /** The display name of the user. */
    private final String user;

    /** The id of the user. */
    private final String userID;

    /** The date of the change. */
    private final String date;

    /** Does the configuration exist?. */
    private final boolean configExists;

    /** The name of the job or file. */
    private final String job;

    /** One of created, changed, renamed or deleted. */
    private final String operation;

    /** true if this information is for a Hudson job,
     *  as opposed to information for a system configuration file.
     */
    private boolean isJob;

    /**
     * Returns a new ConfigInfo object for a system configuration file.
     * @param name
     *            Name of the configuration entity we are saving.
     * @param file
     *            The file with configuration data.
     * @param histDescr
     *            metadata of the change.
     * @param isJob
     *            whether it is a job's config info or not.
     * @return a new ConfigInfo object.
     */
    public static ConfigInfo create(final String name, final boolean configExists, final HistoryDescr histDescr, final boolean isJob) {
        return new ConfigInfo(
                name,
                configExists,
                histDescr.getTimestamp(),
                histDescr.getUser(),
                histDescr.getOperation(),
                histDescr.getUserID(),
                isJob);
    }

    /**
     * @param job see {@link ConfigInfo#job}.
     * @param file see {@link ConfigInfo#file}.
     * @param date see {@link ConfigInfo#date}
     * @param user see {@link ConfigInfo#user}
     * @param operation see {@link ConfigInfo#operation}
     * @param userID see {@link ConfigInfo#userID}
     * @param isJob see {@link ConfigInfo#isJob}
     */
    ConfigInfo(String job, boolean configExists, String date, String user, String operation, String userID, boolean isJob) {
        this.job = job;
        this.configExists = configExists;
        this.date = date;
        this.user = user;
        this.operation = operation;
        this.userID = userID;
        this.isJob = isJob;

    }

    /**
     * Returns the display name of the user.
     *
     * @return display name
     */
    @Exported
    public String getUser() {
        return user;
    }

    /**
     * Returns the id of the user.
     *
     * @return user id
     */
    @Exported
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the date of the change.
     *
     * @return timestamp in the format of {@link JobConfigHistoryConsts#ID_FORMATTER}
     */
    @Exported
    public String getDate() {
        return date;
    }

    /**
     * Does the configuration of the file exist?
     *
     * @return URL encoded filename
     */
    @Exported
    public boolean hasConfig() {
        return true;
    }

    /**
     * Returns the name of the job.
     *
     * @return name of the job
     */
    @Exported
    public String getJob() {
        return job;
    }

    /**
     * Returns the type of the operation.
     *
     * @return name of the operation
     */
    @Exported
    public String getOperation() {
        return operation;
    }

    /**
     * Returns true if this object represents a Hudson job
     * as opposed to representing a system configuration.
     * @return true if this object stores a Hudson job configuration
     */
    public boolean getIsJob() {
        return isJob;
    }

    @Override
    public String toString() {
        return operation + " on " + job + " @" + date;
    }

    /**
     * Converts give file to encode URL string.
     *
     * @param file to convert
     * @return encoded url
     */
    private static String createEncodedUrl(final File file) {
        String encodedURL = null;
        if (file != null) {
            try {
                encodedURL = URLEncoder.encode(file.getAbsolutePath(), "utf-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Could not encode " + file.getAbsolutePath(), ex);
            }
        }
        return encodedURL;
    }

    /**
     * Returns a {@link Date}.
     *
     * @return The parsed date as a java.util.Date.
     */
    @Override
    public Date parsedDate() {
        return PluginUtils.parsedDate(getDate());
    }
}
