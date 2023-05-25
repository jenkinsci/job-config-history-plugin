/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import java.util.Date;

/**
 * Holder for information about an altering operation saved to
 * {@link JobConfigHistoryConsts#HISTORY_FILE}.
 *
 * @author Stefan Brausch
 */
public class HistoryDescr implements ParsedDate {

    public static final HistoryDescr EMPTY_HISTORY_DESCR = new HistoryDescr(
            null, null, null, null, null, null);

    /**
     * Display name of the user doing the operation.
     */
    private final String user;

    /**
     * Id of the user doing the operation.
     */
    private final String userId;

    /**
     * Name of the operation.
     */
    private final String operation;

    /**
     * Timestamp of the operation, see
     * {@link JobConfigHistoryConsts#ID_FORMATTER}.
     */
    private final String timestamp;

    /**
     * Current name of the job after renaming.
     */
    private final String currentName;

    /**
     * Old name of the job before renaming.
     */
    private final String oldName;

    /* The reason for this configuration change*/
    private final String changeReasonComment;

    /**
     * @param user        display name of the user doing the operation
     * @param userId      id of the user doing the operation
     * @param operation   name of the operation
     * @param timestamp   timestamp of the operation
     * @param currentName the current name after renaming
     * @param oldName     the name before renaming
     */
    public HistoryDescr(String user, String userId, String operation,
                        String timestamp, String currentName, String oldName) {
        this.user = user;
        this.userId = userId;
        this.operation = operation;
        this.timestamp = timestamp;
        this.currentName = currentName;
        this.oldName = oldName;
        this.changeReasonComment = null;
    }

    /**
     * @param user        display name of the user doing the operation
     * @param userId      id of the user doing the operation
     * @param operation   name of the operation
     * @param timestamp   timestamp of the operation
     * @param currentName the current name after renaming
     * @param oldName     the name before renaming
     */
    public HistoryDescr(String user, String userId, String operation,
                        String timestamp, String currentName, String oldName, String changeReasonComment) {
        this.user = user;
        this.userId = userId;
        this.operation = operation;
        this.timestamp = timestamp;
        this.currentName = currentName;
        this.oldName = oldName;
        this.changeReasonComment = changeReasonComment;
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

    /**
     * Returns a {@link Date}.
     *
     * @return The parsed date as a java.util.Date.
     */
    @Override
    public Date parsedDate() {
        return PluginUtils.parsedDate(getTimestamp());
    }

    /**
     * Returns the current job name after renaming.
     *
     * @return the current job name
     */
    public String getCurrentName() {
        return currentName;
    }

    /**
     * Returns the old job name before renaming.
     *
     * @return the old job name
     */
    public String getOldName() {
        return oldName;
    }

    @Override
    public String toString() {
        return "HistoryDescr{" +
                "user='" + user + '\'' +
                ", userId='" + userId + '\'' +
                ", operation='" + operation + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", currentName='" + currentName + '\'' +
                ", oldName='" + oldName + '\'' +
                ", changeReasonComment='" + changeReasonComment + '\'' +
                '}';
    }


    /**
     * @return the comment on why the config has been changed. null, if not given.
     */
    public String getChangeReasonComment() {
        return changeReasonComment;
    }
}
