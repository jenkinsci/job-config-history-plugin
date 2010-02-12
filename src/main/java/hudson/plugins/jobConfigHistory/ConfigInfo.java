package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractProject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Holder object for displaying information.
 *
 * @author Stefan Brausch
 */
@ExportedBean(defaultVisibility = 999)
public class ConfigInfo {

    /** The display name of the user. */
    private final String user;

    /** The id of the user. */
    private final String userID;

    /** The date of the change. */
    private final String date;

    /** The urlencoded path to the config file of the job. */
    private final String file;

    /** The name of the job. */
    private final String job;

    /** One of created, changed or renamed. */
    private final String operation;

    /**
     * Returns a new ConfigInfo object.
     *
     * @param job
     *            a project
     * @param file
     *            pointing to {@code config.xml}
     * @param histDescr
     *            metadata of the change
     * @throws UnsupportedEncodingException
     *             if UTF-8 is not available (probably a serious error).
     */
    public static ConfigInfo create(final AbstractProject<?, ?> job, final File file, final HistoryDescr histDescr) throws UnsupportedEncodingException {
        return new ConfigInfo(
                job.getName(),
                URLEncoder.encode(file.getAbsolutePath(), "utf-8"),
                histDescr.getTimestamp(),
                histDescr.getUser(),
                histDescr.getOperation(),
                histDescr.getUserID());
    }

    /**
     * @param job see {@link ConfigInfo#job}.
     * @param file see {@link ConfigInfo#file}.
     * @param date see {@link ConfigInfo#date}
     * @param user see {@link ConfigInfo#user}
     * @param operation see {@link ConfigInfo#operation}
     * @param userID see {@link ConfigInfo#userID}
     */
    ConfigInfo(String job, String file, String date, String user, String operation, String userID) {
        this.job = job;
        this.file = file;
        this.date = date;
        this.user = user;
        this.operation = operation;
        this.userID = userID;

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
     * @return timestamp in the format of {@link ConfigHistoryListenerHelper#ID_FORMATTER}
     */
    @Exported
    public String getDate() {
        return date;
    }

    /**
     * Returns the URL encoded absolute name of the file.
     *
     * @return URL encoded filename
     */
    @Exported
    public String getFile() {
        return file;
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
}
