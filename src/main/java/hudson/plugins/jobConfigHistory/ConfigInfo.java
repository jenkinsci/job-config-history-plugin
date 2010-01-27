package hudson.plugins.jobConfigHistory;

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
    private String user;

    /** The id of the user. */
    private String userId;

    /** The date of the change. */
    private String date;

    private String file;

    /** The name of the job. */
    private String job;

    /** One of created, changed or renamed. */
    private String operation;

    /**
     * Empty constructor for the bean.
     */
    public ConfigInfo() {

    }

    /** Returns the display name of the user. */
    @Exported
    public String getUser() {
        return user;
    }

    /** Sets the display name of the user. */
    public void setUser(String userId) {
        this.userId = userId;
    }

    /** Returns the id of the user. */
    @Exported
    public String getUserId() {
        return userId;
    }

    /** Sets the id of the user. */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** Returns the date of the change. */
    @Exported
    public String getDate() {
        return date;
    }

    /** Sets the date of the change. */
    public void setDate(String date) {
        this.date = date;
    }

    /** Returns the name of the file. */
    @Exported
    public String getFile() {
        return file;
    }

    /** Sets the name of the file. */
    public void setFile(String file) {
        this.file = file;
    }

    /** Returns the name of the job. */
    @Exported
    public String getJob() {
        return job;
    }

    /** Sets the name of the job. */
    public void setJob(String job) {
        this.job = job;
    }

    /** Returns the type of the operation. */
    @Exported
    public String getOperation() {
        return operation;
    }

    /** Sets the type of the operation. */
    public void setOperation(String operation) {
        this.operation = operation;
    }

}
