package hudson.plugins.jobConfigHistory;

/**
 * @author Stefan Brausch
 */
public class HistoryDescr {

    final private String user;
    final private String userId;
    final private String operation;
    final private String timestamp;

    public HistoryDescr(String user, String userId, String operation, String timestamp){
        this.user = user;
        this.userId = userId;
        this.operation = operation;
        this.timestamp = timestamp;

    }

    public String getUser() {
        return user;
    }

    public String getUserID() {
        return userId;
    }

    public String getOperation() {
        return operation;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
