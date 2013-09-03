/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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

    /** milliseconds between attempts to save a new entry. */
    private static final int CLASH_SLEEP_TIME = 500;

    /** Base location for all files. */
    private final File historyRootDir;

    /** JENKINS_HOME. */
    private final File jenkinsHome;

    /** Currently logged in user. */
    private final User currentUser;

    /** Maximum numbers which should exist. */
    private final int maxHistoryEntries;

    /**
     * @param historyRootDir where to store history
     * @param jenkinsHome JENKKINS_HOME
     * @param currentUser of operation
     * @param maxHistoryEntries max number of history entries
     */
    FileHistoryDao(final File historyRootDir, File jenkinsHome, User currentUser, int maxHistoryEntries) {
        this.historyRootDir = historyRootDir;
        this.jenkinsHome = jenkinsHome;
        this.currentUser = currentUser;
        this.maxHistoryEntries = maxHistoryEntries;
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
        final File itemHistoryDir = getHistoryDir(xmlFile);
        // perform check for purge here, when we are actually going to create
        // a new directory, rather than just when we scan it in above method.
        purgeOldEntries(itemHistoryDir, maxHistoryEntries);
        return createNewHistoryDir(itemHistoryDir, timestampHolder);
    }

    /**
     * Creates the historical description for this action.
     *
     * @param timestamp
     *            when the action did happen.
     * @param timestampedDir
     *            the directory where to save the history.
     * @param operation
     *            description of operation.
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
                    Thread.sleep(CLASH_SLEEP_TIME);
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XmlFile getOldRevision(AbstractItem item, String identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Creates a new history entry.
     *
     * @param xmlFile to save.
     * @param operation description
     */
    void createNewHistoryEntry(final XmlFile xmlFile, final String operation) {
        try {
            final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
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
            LOG.log(Level.SEVERE, "Unable to create history entry for configuration file: " + xmlFile.getFile().getAbsolutePath(), e);
        } catch (RuntimeException e) {
            // If not able to create the history entry, log, but continue without it.
            // A known issue is where Hudson core fails to move the folders on rename,
            // but continues as if it did.
            // Reference http://issues.hudson-ci.org/browse/HUDSON-8318
            LOG.log(Level.SEVERE, "Unable to create history entry for configuration file: " + xmlFile.getFile().getAbsolutePath(), e);
        }
    }

    /**
     * Returns the configuration history directory for the given configuration file.
     *
     * @param xmlFile
     *            The configuration file whose content we are saving.
     * @return The base directory where to store the history,
     *         or null if the file is not a valid Hudson configuration file.
     */
    File getHistoryDir(final XmlFile xmlFile) {
        final String configRootDir = xmlFile.getFile().getParent();
        final String hudsonRootDir = jenkinsHome.getPath();
        if (!configRootDir.startsWith(hudsonRootDir)) {
            LOG.warning("Trying to get history dir for object outside of HUDSON: " + xmlFile);
            return null;
        }
        //if the file is stored directly under HUDSON_ROOT, it's a system config
        //so create a distinct directory
        String underRootDir = null;
        if (configRootDir.equals(hudsonRootDir)) {
            final String xmlFileName = xmlFile.getFile().getName();
            underRootDir = xmlFileName.substring(0, xmlFileName.lastIndexOf('.'));
        }
        final File historyDir;
        if (underRootDir == null) {
            final String remainingPath = configRootDir.substring(
                    hudsonRootDir.length() + JobConfigHistoryConsts.JOBS_HISTORY_DIR.length() + 1);
            historyDir = new File(getJobHistoryRootDir(), remainingPath);
        } else {
            historyDir = new File(historyRootDir, underRootDir);
        }
        return historyDir;
    }

    /**
     * Returns the File object representing the job history directory,
     * which is for reasons of backwards compatibility either a sibling or child
     * of the configured history root dir.
     *
     * @return The job history File object.
     */
    File getJobHistoryRootDir() {
        //ROOT/config-history/jobs
        return new File(historyRootDir, "/" + JobConfigHistoryConsts.JOBS_HISTORY_DIR);
    }

    /**
     * Purges old entries for the given history root to maxEntries.
     *
     * @param itemHistoryRoot directory to inspect.
     * @param maxEntries maximum number of entries.
     */
    static void purgeOldEntries(final File itemHistoryRoot, final int maxEntries) {
        if (maxEntries > 0) {
            LOG.fine("checking for history files to purge (" + maxEntries + " max allowed)");
            final int entriesToLeave = maxEntries - 1;
            final File[] historyDirs = itemHistoryRoot.listFiles(HistoryFileFilter.INSTANCE);
            if (historyDirs != null && historyDirs.length >= entriesToLeave) {
                Arrays.sort(historyDirs, Collections.reverseOrder());
                for (int i = entriesToLeave; i < historyDirs.length; i++) {
                    LOG.fine("purging old directory from history logs: " + historyDirs[i]);
                    for (File file : historyDirs[i].listFiles()) {
                        if (!file.delete()) {
                            LOG.warning("problem deleting history file: " + file);
                        }
                    }
                    if (!historyDirs[i].delete()) {
                        LOG.warning("problem deleting history directory: " + historyDirs[i]);
                    }
                }
            }
        }
    }


}
