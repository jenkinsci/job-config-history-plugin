/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
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
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener} and
 * {@link JobConfigHistorySaveableListener}.
 *
 * @author mfriedenhagen
 */
public class FileHistoryDao implements HistoryDao {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(FileHistoryDao.class.getName());

    /** Base location for all files. */
    private final File historyRootDir;

    /** JENKINS_HOME. */
    private final File jenkinsHome;

    /** Currently logged in user. */
    private final User currentUser;

    /**
     */
    FileHistoryDao(final File historyRootDir, File jenkinsHome, User currentUser) {
        this.historyRootDir = historyRootDir;
        this.jenkinsHome = jenkinsHome;
        this.currentUser = currentUser;
    }

    /**
     * Creates a timestamped directory to save the configuration beneath. Purges old data if configured
     *
     * @param xmlFile
     *            the current xmlFile configuration file to save
     * @param timestampHolder
     *            time of operation.
     * @return timestamped directory where to store one history entry.
     */
    File getRootDir(final XmlFile xmlFile, final AtomicReference<Calendar> timestampHolder) {
        final JobConfigHistory plugin = getPlugin();
        final File itemHistoryDir = plugin.getHistoryDir(xmlFile);
        // perform check for purge here, when we are actually going to create
        // a new directory, rather than just when we scan it in above method.
        plugin.checkForPurgeByQuantity(itemHistoryDir);
        return createNewHistoryDir(itemHistoryDir, timestampHolder);
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
    void createHistoryXmlFile(final Calendar timestamp, final File timestampedDir, final String operation) throws IOException {
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
    static void copyConfigFile(final File currentConfig, final File timestampedDir) throws FileNotFoundException,
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
    static SimpleDateFormat getIdFormatter() {
        return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
    }

    /**
     * Creates the new history dir, loops until "enough" time has passed if two events are too near.
     *
     * @param itemHistoryDir the basedir for history items.
     * @param timestampHolder of the event.
     * @return new directory.
     */
    @SuppressWarnings("SleepWhileInLoop")
    static File createNewHistoryDir(final File itemHistoryDir, final AtomicReference<Calendar> timestampHolder) {
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

    JobConfigHistory getPlugin() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class);
    }

    @Override
    public void createNewItem(AbstractItem item) {
        createNewHistoryEntry(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_CREATED());
    }

    @Override
    public void saveItem(AbstractItem item) {
        createNewHistoryEntry(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_CHANGED());
    }

    @Override
    public void saveItem(XmlFile file) {
        createNewHistoryEntry(file, Messages.ConfigHistoryListenerHelper_CHANGED());
    }

    @Override
    public void deleteItem(AbstractItem item) {
        createNewHistoryEntry(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_DELETED());
    }

    @Override
    public void renameItem(AbstractItem item, String newName) {
        createNewHistoryEntry(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_RENAMED());
    }

    @Override
    public SortedMap<String, XmlFile> getRevisions(AbstractItem item) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public XmlFile getOldRevision(AbstractItem item, String identifier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void createNewHistoryEntry(final XmlFile xmlFile, final String operation) {
        try {
            AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
            final File timestampedDir = getRootDir(xmlFile, timestampHolder);
            LOG.log(Level.FINE, "{0} on {1}", new Object[] {this, timestampedDir});
            if (!Messages.ConfigHistoryListenerHelper_DELETED().equals(operation)) {
                copyConfigFile(xmlFile.getFile(), timestampedDir);
            }
            assert timestampHolder.get() != null;
            createHistoryXmlFile(timestampHolder.get(), timestampedDir, operation);
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
}
