/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.util.TextFile;

import java.io.File;
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
public class ConfigHistoryListenerHelper {

    /**
     * Format for timestamped dirs.
     */
    static final String ID_FORMATTER = "yyyy-MM-dd_HH-mm-ss";

    /**
     * Helper for job creation.
     */
    static final ConfigHistoryListenerHelper CREATED = new ConfigHistoryListenerHelper("Created");

    /**
     * Helper for job rename.
     */
    static final ConfigHistoryListenerHelper RENAMED = new ConfigHistoryListenerHelper("Renamed");

    /**
     * Helper for job change.
     */
    static final ConfigHistoryListenerHelper CHANGED = new ConfigHistoryListenerHelper("Changed");

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
            final TextFile myConfig = new TextFile(new File(timestampedDir, "config.xml"));

            final String configContent;
            if (project.getConfigFile().exists()) {
                configContent = project.getConfigFile().asString();
            } else {
                configContent = "";
            }
            myConfig.write(configContent);

            final XmlFile myDescription = new XmlFile(new File(timestampedDir, "history.xml"));

            final String user;
            final String userId;
            if (User.current() != null) {
                user = User.current().getFullName();
                userId = User.current().getId();
            } else {
                user = "Anonym";
                userId = "";
            }

            final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, getIdFormatter().format(
                    timestamp.getTime()));

            myDescription.write(myDescr);
        } catch (IOException e) {
            throw new RuntimeException("Operation " + operation + " on " + project.getName() + " did not succeed", e);
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
