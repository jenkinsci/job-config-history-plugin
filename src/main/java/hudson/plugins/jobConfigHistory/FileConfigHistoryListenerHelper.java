/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener} and
 * {@link JobConfigHistorySaveableListener}.
 * 
 * @author mfriedenhagen
 */
public enum FileConfigHistoryListenerHelper implements ConfigHistoryListenerHelper {

    /**
     * Helper for job creation.
     */
    CREATED(Messages.ConfigHistoryListenerHelper_CREATED()),

    /**
     * Helper for job rename.
     */
    RENAMED(Messages.ConfigHistoryListenerHelper_RENAMED()),

    /**
     * Helper for job change.
     */
    CHANGED(Messages.ConfigHistoryListenerHelper_CHANGED()),

    /**
     * Helper for job deleted.
     */
    DELETED(Messages.ConfigHistoryListenerHelper_DELETED());

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(FileConfigHistoryListenerHelper.class.getName());

    /**
     * Name of the operation.
     */
    private final String operation;

    /**
     * 
     * @param operation
     *            the operation we handle.
     */
    FileConfigHistoryListenerHelper(final String operation) {
        this.operation = operation;
    }

    /**
     * Creates a timestamped directory to save the configuration beneath. Purges old data if configured
     * 
     * @param xmlFile
     *            the current xmlFile configuration file to save
     * @param timestamp
     *            time of operation.
     * @return timestamped directory where to store one history entry.
     */
    @SuppressWarnings("SleepWhileInLoop")
    private File getRootDir(final XmlFile xmlFile, final AtomicReference<Calendar> timestampHolder) {
        final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);
        final File itemHistoryDir = plugin.getHistoryDir(xmlFile);
        // perform check for purge here, when we are actually going to create
        // a new directory, rather than just when we scan it in above method.
        plugin.checkForPurgeByQuantity(itemHistoryDir);
        Calendar timestamp;
        File f;
        while (true) {
            timestamp = new GregorianCalendar();
            f = new File(itemHistoryDir, getIdFormatter().format(timestamp.getTime()));
            if (f.isDirectory()) {
                LOG.log(Level.FINE, "clash on {0}, will wait a moment", f);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException x) {
                    throw new RuntimeException(x);
                }
            } else {
                timestampHolder.set(timestamp);
                break;
            }
        }
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
     * @param xmlFile
     *            configuration file for the item we want to backup
     */
    @Override
    public final void createNewHistoryEntry(final XmlFile xmlFile) {
        try {
            AtomicReference<Calendar> timestamp = new AtomicReference<Calendar>();
            final File timestampedDir = getRootDir(xmlFile, timestamp);
            LOG.log(Level.FINE, "{0} on {1}", new Object[] {this, timestampedDir});
            if (this != DELETED) {
                copyConfigFile(xmlFile.getFile(), timestampedDir);
            }
            assert timestamp.get() != null;
            createHistoryXmlFile(timestamp.get(), timestampedDir);
        } catch (IOException e) {
            // If not able to create the history entry, log, but continue without it.
            // A known issue is where Hudson core fails to move the folders on rename,
            // but continues as if it did.
            // Reference http://issues.hudson-ci.org/browse/HUDSON-8318
            LOG.log(Level.SEVERE, "Unable to create history entry for configuration file: " + xmlFile, e);
        } catch (RuntimeException e) {
            // If not able to create the history entry, log, but continue without it.
            // A known issue is where Hudson core fails to move the folders on rename,
            // but continues as if it did.
            // Reference http://issues.hudson-ci.org/browse/HUDSON-8318
            LOG.log(Level.SEVERE, "Unable to create history entry for configuration file: " + xmlFile, e);
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
        final User currentUser = getCurrentUser();
        final String user;
        final String userId;
        if (currentUser != null) {
            user = currentUser.getFullName();
            userId = currentUser.getId();
        } else {
            user = "Anonym";
            userId = Messages.ConfigHistoryListenerHelper_anonymous();
        }

        final XmlFile historyDescription = new XmlFile(new File(timestampedDir, JobConfigHistoryConsts.HISTORY_FILE));
        final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, getIdFormatter().format(
                timestamp.getTime()));
        historyDescription.write(myDescr);
    }

    /**
     * Returns the user who invoked the action.
     * 
     * @return current user.
     */
    User getCurrentUser() {
        return User.current();
    }

    /**
     * Saves a copy of this project's {@code config.xml} into {@code timestampedDir}.
     * 
     * @param currentConfig
     *            which we want to copy.
     * @param timestampedDir
     *            the directory where to save the copy.
     * @throws FileNotFoundException
     *             if initiating the file holding the copy fails.
     * @throws IOException
     *             if writing the file holding the copy fails.
     */
    private void copyConfigFile(final File currentConfig, final File timestampedDir) throws FileNotFoundException,
            IOException {

        final FileOutputStream configCopy = new FileOutputStream(new File(timestampedDir, currentConfig.getName()));
        try {
            final FileInputStream configOriginal = new FileInputStream(currentConfig);
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
        return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
    }

}
