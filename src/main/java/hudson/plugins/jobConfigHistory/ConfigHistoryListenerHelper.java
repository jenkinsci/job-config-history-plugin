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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener} and
 * {@link JobConfigHistorySaveableListener}.
 *
 * @author mirko *
 */
public class ConfigHistoryListenerHelper {

    static final ConfigHistoryListenerHelper CREATED = new ConfigHistoryListenerHelper("Created");

    static final ConfigHistoryListenerHelper RENAMED = new ConfigHistoryListenerHelper("Renamed");

    static final ConfigHistoryListenerHelper DELETED = new ConfigHistoryListenerHelper("Deleted");

    static final ConfigHistoryListenerHelper CHANGED = new ConfigHistoryListenerHelper("Changed");

    final SimpleDateFormat idFormatter = new SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss");

    final String operation;

    /**
     *
     */
    ConfigHistoryListenerHelper(final String operation) {
        this.operation = operation;
    }

    private File getConfigsDir(Item item) {
        return new File(item.getRootDir(), "config-history");
    }

    private File getRootDir(Item item, Calendar timestamp) {
        final File f = new File(getConfigsDir(item), idFormatter.format(timestamp
                .getTime()));
        if (!f.mkdirs()) {
            throw new RuntimeException("Could not create rootDir " + f);
        }
        return f;
    }

    /**
     * Creates a new backup of the job configuration
     * @param item
     * @return
     */
    public final String createNewHistoryEntry(Item item) {
        final Calendar timestamp = new GregorianCalendar();
        final File myDir = getRootDir(item, timestamp);
        final TextFile myConfig = new TextFile(new File(myDir, "config.xml"));

        final String configContent;
        try {
            if (((AbstractProject<?, ?>) item).getConfigFile().exists()) {
                configContent = ((AbstractProject<?, ?>) item).getConfigFile()
                        .asString();
            } else {
                configContent = "";
            }
            myConfig.write(configContent);

            final XmlFile myDescription = new XmlFile(new File(myDir, "history.xml"));

            final String user;
            final String userId;
            if (User.current() != null) {
                user = User.current().getFullName();
                userId = User.current().getId();
            } else {
                user = "Anonym";
                userId = "";
            }

            final HistoryDescr myDescr = new HistoryDescr(user, userId, operation,
                    idFormatter.format(timestamp.getTime()));

            myDescription.write(myDescr);

        } catch (Exception e) {
            Logger.getLogger("Config History Exception: " + e.getMessage());
        }

        return myDir.getAbsolutePath();
    }

}
