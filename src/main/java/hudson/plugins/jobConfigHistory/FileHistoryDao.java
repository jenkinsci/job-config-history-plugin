/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.FilePath;
import hudson.Util;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.User;
import java.io.BufferedOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import static java.util.logging.Level.FINEST;
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

    /** Should we save duplicate entries? */
    private final boolean saveDuplicates;

    /**
     * @param historyRootDir where to store history
     * @param jenkinsHome JENKKINS_HOME
     * @param currentUser of operation
     * @param maxHistoryEntries max number of history entries
     * @param saveDuplicates should we save duplicate entries?
     */
    FileHistoryDao(final File historyRootDir, File jenkinsHome, User currentUser, int maxHistoryEntries, boolean saveDuplicates) {
        this.historyRootDir = historyRootDir;
        this.jenkinsHome = jenkinsHome;
        this.currentUser = currentUser;
        this.maxHistoryEntries = maxHistoryEntries;
        this.saveDuplicates = saveDuplicates;
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

        final XmlFile historyDescription = getHistoryXmlFile(timestampedDir);
        final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, getIdFormatter().format(
                timestamp.getTime()));
        historyDescription.write(myDescr);
    }

    /**
     * Returns the history.xml file in the directory.
     *
     * @param directory to search.
     *
     * @return history.xml
     */
    private XmlFile getHistoryXmlFile(final File directory) {
        return new XmlFile(new File(directory, JobConfigHistoryConsts.HISTORY_FILE));
    }

    /**
     * Saves a copy of this project's {@literal config.xml} into {@literal timestampedDir}.
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
        final BufferedOutputStream configCopy = new BufferedOutputStream(
                new FileOutputStream(new File(timestampedDir, currentConfig.getName())));
        try {
            final FileInputStream configOriginal = new FileInputStream(currentConfig);
            try {
                // in is buffered by copyStream.
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
        createNewHistoryEntryAndCopyConfig(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_CREATED());
    }

    /**
     * Creates a new history entry and copies the old config.xml to a timestamped dir.
     *
     * @param configFile to copy.
     * @param operation operation
     */
    void createNewHistoryEntryAndCopyConfig(final XmlFile configFile, final String operation) {
        final File timestampedDir = createNewHistoryEntry(configFile, operation);
        try {
            copyConfigFile(configFile.getFile(), timestampedDir);
        } catch (IOException ex) {
            throw new RuntimeException("Uanble to copy " + configFile, ex);
        }
    }

    @Override
    public void saveItem(AbstractItem item) {
        saveItem(item.getConfigFile());
    }

    @Override
    public void saveItem(XmlFile file) {
        if (checkDuplicate(file)) {
            createNewHistoryEntryAndCopyConfig(file, Messages.ConfigHistoryListenerHelper_CHANGED());
        }
    }

    @Override
    public void deleteItem(AbstractItem item) {
        createNewHistoryEntry(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_DELETED());
        final File currentHistoryDir = getHistoryDir(item.getConfigFile());
        final SimpleDateFormat buildDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        final String timestamp = buildDateFormat.format(new Date());
        final String deletedHistoryName = item.getName() + JobConfigHistoryConsts.DELETED_MARKER + timestamp;
        final File deletedHistoryDir = new File(currentHistoryDir.getParentFile(), deletedHistoryName);
        if (!currentHistoryDir.renameTo(deletedHistoryDir)) {
            LOG.warning("unable to rename deleted history dir to: " + deletedHistoryDir);
        }
    }

    @Override
    public void renameItem(AbstractItem item, String oldName, String newName) {
        final String onRenameDesc = " old name: " + oldName + ", new name: " + newName;
        if (historyRootDir != null) {
            final File currentHistoryDir = getHistoryDir(item.getConfigFile());
            final File historyParentDir = currentHistoryDir.getParentFile();
            final File oldHistoryDir = new File(historyParentDir, oldName);
            if (oldHistoryDir.exists()) {
                final FilePath fp = new FilePath(oldHistoryDir);
                // catch all exceptions so Hudson can continue with other rename tasks.
                try {
                    fp.copyRecursiveTo(new FilePath(currentHistoryDir));
                    fp.deleteRecursive();
                    LOG.log(FINEST, "completed move of old history files on rename.{0}", onRenameDesc);
                } catch (IOException e) {
                    final String ioExceptionStr = "unable to move old history on rename." + onRenameDesc;
                    LOG.log(Level.SEVERE, ioExceptionStr, e);
                } catch (InterruptedException e) {
                    final String irExceptionStr = "interrupted while moving old history on rename." + onRenameDesc;
                    LOG.log(Level.WARNING, irExceptionStr, e);
                }
            }

        }
        createNewHistoryEntryAndCopyConfig(item.getConfigFile(), Messages.ConfigHistoryListenerHelper_RENAMED());
    }

    @Override
    public SortedMap<String, HistoryDescr> getRevisions(AbstractItem item) {
        return getRevisions(item.getConfigFile());
    }

    @Override
    public SortedMap<String, HistoryDescr> getRevisions(XmlFile xmlFile) {
        final File historiesDir = getHistoryDir(xmlFile);
        final File[] historyDirsOfItem = historiesDir.listFiles(HistoryFileFilter.INSTANCE);
        final TreeMap<String, HistoryDescr> map = new TreeMap<String, HistoryDescr>();
        if (historyDirsOfItem == null) {
            return map;
        } else {
            for (File historyDir : historyDirsOfItem) {
                final XmlFile historyXml = getHistoryXmlFile(historyDir);
                final HistoryDescr historyDescription;
                try {
                    historyDescription = (HistoryDescr) historyXml.read();
                } catch (IOException ex) {
                    throw new RuntimeException("Unable to read history for " + xmlFile, ex);
                }
                map.put(historyDir.getName(), historyDescription);
            }
            return map;
        }
    }

    @Override
    public XmlFile getOldRevision(AbstractItem item, String identifier) {
        final File historyDir = new File(getHistoryDir(item.getConfigFile()), identifier);
        if (item instanceof MavenModule) {
            final String path = historyDir + ((MavenModule) item).getParent().getFullName().replace("/", "/jobs/") + "/modules/"
                    + ((MavenModule) item).getModuleName().toFileSystemName() + "/" + identifier;
            return new XmlFile(getConfigFile(new File(path)));
        } else {
            return new XmlFile(getConfigFile(historyDir));
        }
    }

    @Override
    public XmlFile getOldRevision(XmlFile xmlFile, String identifier) {
        final File historyDir = new File(getHistoryDir(xmlFile), identifier);
        return new XmlFile(getConfigFile(historyDir));
    }

    @Override
    public boolean hasOldRevision(AbstractItem item, String identifier) {
        return hasOldRevision(item.getConfigFile(), identifier);
    }

    @Override
    public boolean hasOldRevision(XmlFile xmlFile, String identifier) {
        final XmlFile oldRevision = getOldRevision(xmlFile, identifier);
        return oldRevision.getFile() != null && oldRevision.getFile().exists();
    }

    /**
     * Creates a new history entry.
     *
     * @param xmlFile to save.
     * @param operation description
     *
     * @return timestampedDir
     */
    File createNewHistoryEntry(final XmlFile xmlFile, final String operation) {
        try {
            final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
            final File timestampedDir = getRootDir(xmlFile, timestampHolder);
            LOG.log(Level.FINE, "{0} on {1}", new Object[] {this, timestampedDir});
            createHistoryXmlFile(timestampHolder.get(), timestampedDir, operation);
            assert timestampHolder.get() != null;
            return timestampedDir;
        } catch (IOException e) {
            // If not able to create the history entry, log, but continue without it.
            // A known issue is where Hudson core fails to move the folders on rename,
            // but continues as if it did.
            // Reference https://issues.jenkins-ci.org/browse/JENKINS-8318
            throw new RuntimeException(
                    "Unable to create history entry for configuration file: " + xmlFile.getFile().getAbsolutePath(), e);
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
            throw new IllegalArgumentException("Trying to get history dir for object outside of HUDSON: " + xmlFile);
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

    @Override
    public void purgeOldEntries(final File itemHistoryRoot, final int maxEntries) {
        if (maxEntries > 0) {
            LOG.log(Level.FINE, "checking for history files to purge ({0} max allowed)", maxEntries);
            final int entriesToLeave = maxEntries - 1;
            final File[] historyDirs = itemHistoryRoot.listFiles(HistoryFileFilter.INSTANCE);
            if (historyDirs != null && historyDirs.length >= entriesToLeave) {
                Arrays.sort(historyDirs, Collections.reverseOrder());
                for (int i = entriesToLeave; i < historyDirs.length; i++) {
                    if (isCreatedEntry(historyDirs[i])) {
                        continue;
                    }
                    LOG.log(Level.FINE, "purging old directory from history logs: {0}", historyDirs[i]);
                    deleteDirectory(historyDirs[i]);
                }
            }
        }
    }

    @Override
    public boolean isCreatedEntry(File historyDir) {
        final XmlFile historyXml = getHistoryXmlFile(historyDir);
        try {
            final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
            LOG.log(Level.FINEST, "historyDir: {0}", historyDir);
            LOG.log(Level.FINEST, "histDescr.getOperation(): {0}", histDescr.getOperation());
            if ("Created".equals(histDescr.getOperation())) {
                return true;
            }
        } catch (IOException ex) {
            LOG.log(Level.FINEST, "Unable to retrieve history file for {0}", historyDir);
        }
        return false;
    }

    /**
     * Deletes a history directory (e.g. Test/2013-18-01_19-53-40),
     * first deleting the files it contains.
     * @param dir The directory which should be deleted.
     */
    private void deleteDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (!file.delete()) {
                LOG.log(Level.WARNING, "problem deleting history file: {0}", file);
            }
        }
        if (!dir.delete()) {
            LOG.log(Level.WARNING, "problem deleting history directory: {0}", dir);
        }
    }


    /**
     * Returns the configuration data file stored in the specified history directory.
     * It looks for a file with an 'xml' extension that is not named
     * {@link JobConfigHistoryConsts#HISTORY_FILE}.
     * <p>
     * Relies on the assumption that random '.xml' files
     * will not appear in the history directories.
     * <p>
     * Checks that we are in an actual 'history directory' to prevent use for
     * getting random xml files.
     * @param historyDir
     *            The history directory to look under.
     * @return The configuration file or null if no file is found.
     */
    static File getConfigFile(final File historyDir) {
        File configFile = null;
        if (HistoryFileFilter.accepts(historyDir)) {
            // get the *.xml file that is not the JobConfigHistoryConsts.HISTORY_FILE
            // assumes random .xml files won't appear in the history directory
            final File[] listing = historyDir.listFiles();
            for (final File file : listing) {
                if (!file.getName().equals(JobConfigHistoryConsts.HISTORY_FILE) && file.getName().matches(".*\\.xml$")) {
                    configFile = file;
                    break;
                }
            }
        }
        return configFile;
    }

    /**
     * Determines if the {@link XmlFile} contains a duplicate of
     * the last saved information, if there is previous history.
     *
     * @param xmlFile
     *           The {@link XmlFile} configuration file under consideration.
     * @return true if previous history is accessible, and the file duplicates the previously saved information.
     */
    boolean hasDuplicateHistory(XmlFile xmlFile) {
        boolean isDuplicated = false;
        final ArrayList<String> timeStamps = new ArrayList<String>(getRevisions(xmlFile).keySet());
        if (!timeStamps.isEmpty()) {
            Collections.sort(timeStamps, Collections.reverseOrder());
            final XmlFile lastRevision = getOldRevision(xmlFile, timeStamps.get(0));
            try {
                if (xmlFile.asString().equals(lastRevision.asString())) {
                    isDuplicated = true;
                }
            } catch (IOException e) {
                LOG.warning("unable to check for duplicate previous history file: " + lastRevision + "\n" + e);
            }
        }
        return isDuplicated;
    }

    /**
     * Checks whether the configuration file should not be saved because it's a duplicate.
     * @param xmlFile The config file
     * @return True if it should be saved
     */
    boolean checkDuplicate(final XmlFile xmlFile) {
        if (!saveDuplicates && hasDuplicateHistory(xmlFile)) {
            LOG.log(Level.FINE, "found duplicate history, skipping save of {0}", xmlFile);
            return false;
        } else {
            return true;
        }
    }

}
