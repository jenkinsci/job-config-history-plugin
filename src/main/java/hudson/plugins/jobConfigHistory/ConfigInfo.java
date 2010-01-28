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
    private final String userId;

    /** The date of the change. */
    private final String date;

    /** The urlencoded path to the config file of the job. */
    private final String file;

    /** The name of the job. */
    private final String job;

    /** One of created, changed or renamed. */
    private final String operation;

    /**
     * @throws UnsupportedEncodingException
     */
    public ConfigInfo(final AbstractProject<?,?> job, final File file, final HistoryDescr histDescr) throws UnsupportedEncodingException {
        this.job = job.getName();
        this.file = URLEncoder.encode(file.getAbsolutePath(), "utf-8");
        this.date = histDescr.getTimestamp();
        this.user = histDescr.getUser();
        this.operation = histDescr.getOperation();
        this.userId = histDescr.getUserID();
    }

    /** Returns the display name of the user. */
    @Exported
    public String getUser() {
        return user;
    }

    /** Returns the id of the user. */
    @Exported
    public String getUserId() {
        return userId;
    }

    /** Returns the date of the change. */
    @Exported
    public String getDate() {
        return date;
    }

    /** Returns the name of the file. */
    @Exported
    public String getFile() {
        return file;
    }

    /** Returns the name of the job. */
    @Exported
    public String getJob() {
        return job;
    }

    /** Returns the type of the operation. */
    @Exported
    public String getOperation() {
        return operation;
    }
}
