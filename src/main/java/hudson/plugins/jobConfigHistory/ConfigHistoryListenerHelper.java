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

    private final SimpleDateFormat idFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

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
        final File f = new File(getConfigsDir(item), idFormatter.format(timestamp.getTime()));
        if (f.mkdirs() || f.exists()) {
            // mkdirs sometimes fails although the directory exists afterwards,
            // so check for existence as well and just be happy if it does.
        } else {
            throw new RuntimeException("Could not create rootDir " + f);
        }
        return f;
    }

    /**
     * Creates a new backup of the job configuration.
     *
     * @param project
     *            for which we want to save the configuration.
     * @throws IOException
     *             when reading the old configuration or storing the new configuration does not succeed.
     */
    public final void createNewHistoryEntry(final AbstractProject<?, ?> project) throws IOException {
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

        final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, idFormatter.format(timestamp.getTime()));

        myDescription.write(myDescr);

    }

}
