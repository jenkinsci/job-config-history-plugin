package hudson.plugins.jobConfigHistory;

/**
 * Holder for information about an altering operation saved to {@code history.xml}.
 *
 * @author Stefan Brausch
 */
public class HistoryDescr {

    /** Display name of the user doing the operation. */
    private final String user;

    /** Id of the user doing the operation. */
    private final String userId;

    /** Name of the operation. */
    private final String operation;

    /** Timestamp of the operation, see {@link ConfigHistoryListenerHelper#ID_FORMATTER}. */
    private final String timestamp;

    /**
     * @param user
     *            display name of the user doing the operation
     * @param userId
     *            id of the user doing the operation
     * @param operation
     *            name of the operation
     * @param timestamp
     *            timestamp of the operation
     */
    public HistoryDescr(String user, String userId, String operation, String timestamp) {
        this.user = user;
        this.userId = userId;
        this.operation = operation;
        this.timestamp = timestamp;

    }

    /**
     * Returns display name of the user doing the operation.
     *
     * @return display name of the user
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns id of the user doing the operation.
     *
     * @return id of the user
     */
    public String getUserID() {
        return userId;
    }

    /**
     * Returns name of the operation.
     *
     * @return name of the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns timestamp of the operation.
     *
     * @return timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

}
