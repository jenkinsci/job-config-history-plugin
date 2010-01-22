package hudson.plugins.jobConfigHistory;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author Stefan Brausch
 */
 
@ExportedBean(defaultVisibility=999)
public class ConfigInfo {


    private String user;
    private String userId;
    private String date;
    private String file;
    private String job;
    private String operation;

    public ConfigInfo() {

    }
    @Exported
    public String getUser() {
        return user;
    }
    public void setUser(String userId) {
        this.userId = userId;
    }
    @Exported
    public String getUserId() {
        return userId;
    }
    public void setUserId(String user) {
        this.user = user;
    }
    @Exported
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    @Exported
    public String getFile() {
        return file;
    }
    public void setFile(String file) {
        this.file = file;
    }

    @Exported
    public String getJob() {
        return job;
    }
    public void setJob(String job) {
        this.job = job;
    }
    @Exported
    public String getOperation() {
        return operation;
    }
    public void setOperation(String operation) {
        this.operation = operation;
    }

}
