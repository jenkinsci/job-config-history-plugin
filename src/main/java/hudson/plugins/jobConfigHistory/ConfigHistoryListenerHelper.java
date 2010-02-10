/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener} and
 * {@link JobConfigHistorySaveableListener}.
 *
 * @author mfriedenhagen
 */
public enum ConfigHistoryListenerHelper {

    /**
     * Helper for job creation.
     */
    CREATED("Created"),

    /**
     * Helper for job rename.
     */
    RENAMED("Renamed"),

    /**
     * Helper for job change.
     */
    CHANGED("Changed");

    /**
     * Format for timestamped dirs.
     */
    static final String ID_FORMATTER = "yyyy-MM-dd_HH-mm-ss";

    /**
     * Name of the operation.
     */
    private final String operation;

    /**
     *
     * @param operation
     *            the operation we handle.
     */
    ConfigHistoryListenerHelper(final String operation) {
        this.operation = operation;
    }

    /**
     * Returns the configuration history directory for the given {@link Item}.
     *
     * @param item
     *            for which we want to save the configuration.
     * @return base directory where to store the history.
     */
    private File getConfigsDir(Item item) {
        return new File(item.getRootDir(), "config-history");
    }

    /**
     * Creates a timestamped directory to save the job configuration beneath.
     *
     * @param item
     *            for which we want to save the configuration.
     * @param timestamp
     *            time of operation.
     * @return timestamped directory where to store one history entry.
     */
    private File getRootDir(Item item, Calendar timestamp) {
        final File f = new File(getConfigsDir(item), getIdFormatter().format(timestamp.getTime()));
        // mkdirs sometimes fails although the directory exists afterwards,
        // so check for existence as well and just be happy if it does.
        if (!(f.mkdirs() || f.exists())) {
            throw new RuntimeException("Could not create rootDir " + f);
        }
        return f;
    }

    /**
     * Creates a new backup of the job configuration.
     *
     * @param project
     *            for which we want to save the configuration.
     */
    public final void createNewHistoryEntry(final AbstractProject<?, ?> project) {
        try {
            final Calendar timestamp = new GregorianCalendar();
            final File timestampedDir = getRootDir(project, timestamp);
            copyConfigFile(project, timestampedDir);
            createHistoryXmlFile(timestamp, timestampedDir);
        } catch (IOException e) {
            throw new RuntimeException("Operation " + operation + " on " + project.getName() + " did not succeed", e);
        }
    }

    /**
     * Creates the historical description for this action.
     *
     * @param timestamp
     *            when the action did happen.
     * @param timestampedDir
     *            the directory where to save the history.
     * @throws IOException
     *             if writing the history fails.
     */
    private void createHistoryXmlFile(final Calendar timestamp, final File timestampedDir) throws IOException {
        final String user;
        final String userId;
        if (User.current() != null) {
            user = User.current().getFullName();
            userId = User.current().getId();
        } else {
            user = "Anonym";
            userId = "anonymous";
        }

        final XmlFile historyDescription = new XmlFile(new File(timestampedDir, "history.xml"));
        final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, getIdFormatter().format(
                timestamp.getTime()));
        historyDescription.write(myDescr);
    }

    /**
     * Saves a copy of this project's {@code config.xml} into {@code timestampedDir}.
     *
     * @param project
     *            whose {@code config.xml} we want to copy.
     * @param timestampedDir
     *            the directory where to save the copy.
     * @throws FileNotFoundException
     *             if initiating the file holding the copy fails.
     * @throws IOException
     *             if writing the file holding the copy fails.
     */
    private void copyConfigFile(final AbstractProject<?, ?> project, final File timestampedDir)
            throws FileNotFoundException, IOException {
        final FileOutputStream configCopy = new FileOutputStream(new File(timestampedDir, "config.xml"));
        try {
            final FileInputStream configOriginal = new FileInputStream(project.getConfigFile().getFile());
            try {
                Util.copyStream(configOriginal, configCopy);
            } finally {
                configOriginal.close();
            }
        } finally {
            configCopy.close();
        }
    }

    /**
     * Returns a simple formatter used for creating timestamped directories. We create this every time as
     * {@link SimpleDateFormat} is <b>not</b> threadsafe.
     *
     * @return the idFormatter
     */
    SimpleDateFormat getIdFormatter() {
        return new SimpleDateFormat(ID_FORMATTER);
    }

}
