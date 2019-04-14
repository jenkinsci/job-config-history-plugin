/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
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

import static java.util.logging.Level.FINEST;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileFilter;

import java.text.SimpleDateFormat;

import java.util.List;
import java.util.LinkedList;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.XmlFile;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.User;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener}
 * and {@link JobConfigHistorySaveableListener}.
 *
 * @author Mirko Friedenhagen
 */
@Extension
public class FileHistoryDao extends JobConfigHistoryStrategy
	implements
	Purgeable {

	/**
	 * Our logger.
	 */
	private static final Logger LOG = Logger
		.getLogger(FileHistoryDao.class.getName());

	/**
	 * milliseconds between attempts to save a new entry.
	 */
	private static final int CLASH_SLEEP_TIME = 500;

	/**
	 * Base location for all files.
	 */
	private final File historyRootDir;

	/**
	 * JENKINS_HOME.
	 */
	private final File jenkinsHome;

	/**
	 * Currently logged in user.
	 */
	private final User currentUser;

	/**
	 * Maximum numbers which should exist.
	 */
	private final int maxHistoryEntries;

	/**
	 * Should we save duplicate entries?
	 */
	private final boolean saveDuplicates;

	public FileHistoryDao() {
		this(null, null, null, 0, false);
	}

	/**
	 * @param historyRootDir    where to store history
	 * @param jenkinsHome       JENKKINS_HOME
	 * @param currentUser       of operation
	 * @param maxHistoryEntries max number of history entries
	 * @param saveDuplicates    should we save duplicate entries?
	 */
	public FileHistoryDao(final File historyRootDir, final File jenkinsHome,
						  final User currentUser, final int maxHistoryEntries,
						  final boolean saveDuplicates) {
		this.historyRootDir = historyRootDir;
		this.jenkinsHome = jenkinsHome;
		this.currentUser = currentUser;
		this.maxHistoryEntries = maxHistoryEntries;
		this.saveDuplicates = saveDuplicates;
	}

	/**
	 * Creates a timestamped directory to save the configuration beneath. Purges
	 * old data if configured
	 *
	 * @param xmlFile         the current xmlFile configuration file to save
	 * @param timestampHolder time of operation.
	 * @return timestamped directory where to store one history entry.
	 */
	File getRootDir(final XmlFile xmlFile,
					final AtomicReference<Calendar> timestampHolder) {
		final File configFile = xmlFile.getFile();
		final File itemHistoryDir = getHistoryDir(configFile);
		// perform check for purge here, when we are actually going to create
		// a new directory, rather than just when we scan it in above method.
		purgeOldEntries(itemHistoryDir, maxHistoryEntries);
		return createNewHistoryDir(itemHistoryDir, timestampHolder);
	}

	/**
	 * Creates the historical description for this action.
	 *
	 * @param timestamp      when the action did happen.
	 * @param timestampedDir the directory where to save the history.
	 * @param operation      description of operation.
	 * @throws IOException if writing the history fails.
	 */
	void createHistoryXmlFile(final Calendar timestamp,
							  final File timestampedDir, final String operation,
							  final String newName, String oldName) throws IOException {
		oldName = ((oldName == null) ? "" : oldName);

		// Mimicking User.getUnknown() that can not be instantiated here as a lot of tests are run without Jenkins
		final String user = currentUser != null ? currentUser.getFullName() : "unknown";
		final String userId = currentUser != null ? currentUser.getId() : "unknown";

		final XmlFile historyDescription = getHistoryXmlFile(timestampedDir);
		final HistoryDescr myDescr = new HistoryDescr(user, userId, operation,
			getIdFormatter().format(timestamp.getTime()),
			(newName == null) ? "" : newName, (newName == null)
			? ""
			: ((newName.equals(oldName)) ? "" : oldName));
		historyDescription.write(myDescr);
	}

	/**
	 * Returns the history.xml file in the directory.
	 *
	 * @param directory to search.
	 * @return history.xml
	 */
	private XmlFile getHistoryXmlFile(final File directory) {
		return new XmlFile(
			new File(directory, JobConfigHistoryConsts.HISTORY_FILE));
	}

	/**
	 * Saves a copy of this project's {@literal config.xml} into
	 * {@literal timestampedDir}.
	 *
	 * @param currentConfig  which we want to copy.
	 * @param timestampedDir the directory where to save the copy.
	 * @throws FileNotFoundException if initiating the file holding the copy fails.
	 * @throws IOException           if writing the file holding the copy fails.
	 */
	static void copyConfigFile(final File currentConfig,
							   final File timestampedDir)
		throws FileNotFoundException, IOException {
		final BufferedOutputStream configCopy = new BufferedOutputStream(
			new FileOutputStream(
				new File(timestampedDir, currentConfig.getName())));
		try {
			final FileInputStream configOriginal = new FileInputStream(
				currentConfig);
			try {
				IOUtils.copy(configOriginal, configCopy);
			} finally {
				configOriginal.close();
			}
		} finally {
			configCopy.close();
		}
	}

	/**
	 * Returns a simple formatter used for creating timestamped directories. We
	 * create this every time as {@link SimpleDateFormat} is <b>not</b>
	 * threadsafe.
	 *
	 * @return the idFormatter
	 */
	static SimpleDateFormat getIdFormatter() {
		return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
	}

	/**
	 * Creates the new history dir, loops until "enough" time has passed if two
	 * events are too near.
	 *
	 * @param itemHistoryDir  the basedir for history items.
	 * @param timestampHolder of the event.
	 * @return new directory.
	 */
	@SuppressWarnings("SleepWhileInLoop")
	static File createNewHistoryDir(final File itemHistoryDir,
									final AtomicReference<Calendar> timestampHolder) {
		Calendar timestamp;
		File f;
		while (true) {
			timestamp = new GregorianCalendar();
			f = new File(itemHistoryDir,
				getIdFormatter().format(timestamp.getTime()));
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
	public void createNewItem(final Item item) {
		final AbstractItem aItem = (AbstractItem) item;
		createNewHistoryEntryAndCopyConfig(aItem.getConfigFile(),
			Messages.ConfigHistoryListenerHelper_CREATED(), null, null);
	}

	/**
	 * Creates a new history entry and copies the old config.xml to a
	 * timestamped dir.
	 *
	 * @param configFile to copy.
	 * @param operation  operation
	 */
	private void createNewHistoryEntryAndCopyConfig(final XmlFile configFile,
													final String operation, final String newName,
													final String oldName) {
		final File timestampedDir = createNewHistoryEntry(configFile, operation,
			newName, oldName);
		try {
			copyConfigFile(configFile.getFile(), timestampedDir);
		} catch (IOException ex) {
			throw new RuntimeException("Unable to copy " + configFile, ex);
		}
	}

	@Override
	public void saveItem(final XmlFile file) {
		if (checkDuplicate(file)) {
			createNewHistoryEntryAndCopyConfig(file,
				Messages.ConfigHistoryListenerHelper_CHANGED(), null, null);
		}
	}

	@Override
	public void deleteItem(final Item item) {
		final AbstractItem aItem = (AbstractItem) item;
		createNewHistoryEntry(aItem.getConfigFile(),
			Messages.ConfigHistoryListenerHelper_DELETED(), null, null);
		final File configFile = aItem.getConfigFile().getFile();
		final File currentHistoryDir = getHistoryDir(configFile);
		final SimpleDateFormat buildDateFormat = new SimpleDateFormat(
			"yyyyMMdd_HHmmss_SSS");
		final String timestamp = buildDateFormat.format(new Date());
		final String deletedHistoryName = item.getName()
			+ DeletedFileFilter.DELETED_MARKER + timestamp;
		final File deletedHistoryDir = new File(
			currentHistoryDir.getParentFile(), deletedHistoryName);
		if (!currentHistoryDir.renameTo(deletedHistoryDir)) {
			LOG.log(Level.WARNING,
				"unable to rename deleted history dir to: {0}",
				deletedHistoryDir);
		}
	}

	private File getHistoryDir(Item item) {
		return new File(getHistoryDir(item.getRootDir()), item.getName());
	}

	@Override
	public void changeItemLocation(Item item, String oldFullName, String newFullName) {
		final String onLocationChangedDescription = "old full name: " + oldFullName
			+ ", new full name: " + newFullName;
		if (historyRootDir != null) {
			final String jobsStr;
			final File newHistoryDir = getHistoryDir(item);
			if (SystemUtils.IS_OS_UNIX) {
				jobsStr = "/jobs/";
			} else {
				//windows
				jobsStr = "\\jobs\\";
			}
			final File oldHistoryDir = new File(newHistoryDir.getAbsolutePath()
				.replaceFirst(
					newFullName.replaceAll("/", jobsStr),
					oldFullName.replaceAll("/", jobsStr)
				)
			);

			if (oldHistoryDir.exists()) {
				final FilePath newHistoryFilePath = new FilePath(newHistoryDir);
				final FilePath oldHistoryFilePath = new FilePath(oldHistoryDir);
				try {
					oldHistoryFilePath.copyRecursiveTo(newHistoryFilePath);
					oldHistoryFilePath.deleteRecursive();
					LOG.log(FINEST,
						"completed move of old history files on location change {0}{1}",
						onLocationChangedDescription);
				} catch (IOException e) {
					final String ioExceptionStr = "unable to move old history on location change."
						+ onLocationChangedDescription;
					LOG.log(Level.SEVERE, ioExceptionStr, e);
				} catch (InterruptedException e) {
					final String irExceptionStr = "interrupted while moving old history on location change."
						+ onLocationChangedDescription;
					LOG.log(Level.WARNING, irExceptionStr, e);
				}
			}
		}
	}

	@Override
	public void renameItem(final Item item, final String oldName,
						   final String newName) {
		final AbstractItem aItem = (AbstractItem) item;
		final String onRenameDesc = " old name: " + oldName + ", new name: "
			+ newName;
		if (historyRootDir != null) {
			final File configFile = aItem.getConfigFile().getFile();
			final File currentHistoryDir = getHistoryDir(configFile);
			final File historyParentDir = currentHistoryDir.getParentFile();
			final File oldHistoryDir = new File(historyParentDir, oldName);
			if (oldHistoryDir.exists()) {
				final FilePath fp = new FilePath(oldHistoryDir);
				// catch all exceptions so Jenkins can continue with other
				// rename
				// tasks.
				try {
					fp.copyRecursiveTo(new FilePath(currentHistoryDir));
					fp.deleteRecursive();
					LOG.log(FINEST,
						"completed move of old history files on rename.{0}",
						onRenameDesc);
				} catch (IOException e) {
					final String ioExceptionStr = "unable to move old history on rename."
						+ onRenameDesc;
					LOG.log(Level.SEVERE, ioExceptionStr, e);
				} catch (InterruptedException e) {
					final String irExceptionStr = "interrupted while moving old history on rename."
						+ onRenameDesc;
					LOG.log(Level.WARNING, irExceptionStr, e);
				}
			}

		}
		createNewHistoryEntryAndCopyConfig(aItem.getConfigFile(),
			Messages.ConfigHistoryListenerHelper_RENAMED(), newName,
			oldName);
	}

	@Override
	public SortedMap<String, HistoryDescr> getRevisions(final XmlFile xmlFile) {
		return getRevisions(xmlFile.getFile());
	}

	private SortedMap<String, HistoryDescr> getRevisions(
		final File configFile) {
		final File historiesDir = getHistoryDir(configFile);
		return getRevisions(historiesDir, configFile);
	}

	/**
	 * Returns a sorted map of all revisions for this configFile.
	 *
	 * @param historiesDir to search.
	 * @param configFile   for exception
	 * @return sorted map
	 */
	private SortedMap<String, HistoryDescr> getRevisions(
		final File historiesDir, final File configFile) {
		final File[] historyDirsOfItem = historiesDir
			.listFiles(HistoryFileFilter.INSTANCE);
		final TreeMap<String, HistoryDescr> map = new TreeMap<String, HistoryDescr>();
		if (historyDirsOfItem == null) {
			return map;
		} else {
			for (File historyDir : historyDirsOfItem) {
				final XmlFile historyXml = getHistoryXmlFile(historyDir);
				final LazyHistoryDescr historyDescription = new LazyHistoryDescr(
					historyXml);
				map.put(historyDir.getName(), historyDescription);
			}
			return map;
		}
	}

	@Override
	public XmlFile getOldRevision(final AbstractItem item,
								  final String identifier) {
		final File configFile = item.getConfigFile().getFile();
		final File historyDir = new File(getHistoryDir(configFile), identifier);
		if (PluginUtils.isMavenPluginAvailable()
			&& item instanceof MavenModule) {
			final String path = historyDir
				+ ((MavenModule) item).getParent().getFullName()
				.replace("/", "/jobs/")
				+ "/modules/"
				+ ((MavenModule) item).getModuleName().toFileSystemName()
				+ "/" + identifier;
			return new XmlFile(getConfigFile(new File(path)));
		} else {
			return new XmlFile(getConfigFile(historyDir));
		}
	}

	@Override
	public XmlFile getOldRevision(final XmlFile xmlFile,
								  final String identifier) {
		final File configFile = xmlFile.getFile();
		return getOldRevision(configFile, identifier);
	}

	private XmlFile getOldRevision(final File configFile,
								   final String identifier) {
		final File historyDir = new File(getHistoryDir(configFile), identifier);
		return new XmlFile(getConfigFile(historyDir));
	}

	@Override
	public XmlFile getOldRevision(final String configFileName,
								  final String identifier) {
		final File historyDir = new File(
			new File(historyRootDir, configFileName), identifier);
		final File configFile = getConfigFile(historyDir);
		if (configFile == null) {
			throw new IllegalArgumentException("Could not find " + historyDir);
		}
		return new XmlFile(configFile);
	}

	@Override
	public boolean hasOldRevision(final XmlFile xmlFile,
								  final String identifier) {
		final File configFile = xmlFile.getFile();
		final XmlFile oldRevision = getOldRevision(configFile, identifier);
		return oldRevision.getFile() != null && oldRevision.getFile().exists();
	}

	/**
	 * Creates a new history entry.
	 *
	 * @param xmlFile   to save.
	 * @param operation description
	 * @return timestampedDir
	 */
	File createNewHistoryEntry(final XmlFile xmlFile, final String operation,
							   final String newName, final String oldName) {
		try {
			final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
			final File timestampedDir = getRootDir(xmlFile, timestampHolder);
			LOG.log(Level.FINE, "{0} on {1}",
				new Object[] {this, timestampedDir});
			createHistoryXmlFile(timestampHolder.get(), timestampedDir,
				operation, newName, oldName);
			assert timestampHolder.get() != null;
			return timestampedDir;
		} catch (IOException e) {
			// If not able to create the history entry, log, but continue
			// without it.
			// A known issue is where Jenkins core fails to move the folders on
			// rename,
			// but continues as if it did.
			// Reference https://issues.jenkins-ci.org/browse/JENKINS-8318
			throw new RuntimeException(
				"Unable to create history entry for configuration file: "
					+ xmlFile.getFile().getAbsolutePath(),
				e);
		}
	}

	/**
	 * Returns the configuration history directory for the given configuration
	 * file.
	 *
	 * @param configFile The configuration file whose content we are saving.
	 * @return The base directory where to store the history, or null if the
	 * file is not a valid Jenkins configuration file.
	 */
	public File getHistoryDir(final File configFile) {
		final String configRootDir = configFile.getParent();
		final String jenkinsRootDir = jenkinsHome.getPath();
		if (!configRootDir.startsWith(jenkinsRootDir)) {
			throw new IllegalArgumentException(
				"Trying to get history dir for object outside of Jenkins: "
					+ configFile);
		}
		// if the file is stored directly under JENKINS_ROOT, it's a system
		// config
		// so create a distinct directory
		String underRootDir = null;
		if (configRootDir.equals(jenkinsRootDir)) {
			final String fileName = configFile.getName();
			underRootDir = fileName.substring(0, fileName.lastIndexOf('.'));
		}
		final File historyDir;
		if (underRootDir == null) {
			final String remainingPath = configRootDir
				.substring(jenkinsRootDir.length()
					+ JobConfigHistoryConsts.JOBS_HISTORY_DIR.length()
					+ 1);
			historyDir = new File(getJobHistoryRootDir(), remainingPath);
		} else {
			historyDir = new File(historyRootDir, underRootDir);
		}
		return historyDir;
	}

	/**
	 * Returns the File object representing the job history directory, which is
	 * for reasons of backwards compatibility either a sibling or child of the
	 * configured history root dir.
	 *
	 * @return The job history File object.
	 */
	File getJobHistoryRootDir() {
		// ROOT/config-history/jobs
		return new File(historyRootDir,
			"/" + JobConfigHistoryConsts.JOBS_HISTORY_DIR);
	}

	@Override
	public void purgeOldEntries(final File itemHistoryRoot,
								final int maxEntries) {
		if (maxEntries > 0) {
			LOG.log(Level.FINE,
				"checking for history files to purge ({0} max allowed)",
				maxEntries);
			final int entriesToLeave = maxEntries - 1;
			final File[] historyDirs = itemHistoryRoot
				.listFiles(HistoryFileFilter.INSTANCE);
			if (historyDirs != null && historyDirs.length >= entriesToLeave) {
				Arrays.sort(historyDirs, Collections.reverseOrder());
				for (int i = entriesToLeave; i < historyDirs.length; i++) {
					if (isCreatedEntry(historyDirs[i])) {
						continue;
					}
					LOG.log(Level.FINE,
						"purging old directory from history logs: {0}",
						historyDirs[i]);
					deleteDirectory(historyDirs[i]);
				}
			}
		}
	}

	@Override
	public boolean isCreatedEntry(final File historyDir) {
		final XmlFile historyXml = getHistoryXmlFile(historyDir);
		try {
			final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
			LOG.log(Level.FINEST, "historyDir: {0}", historyDir);
			LOG.log(Level.FINEST, "histDescr.getOperation(): {0}",
				histDescr.getOperation());
			if ("Created".equals(histDescr.getOperation())) {
				return true;
			}
		} catch (IOException ex) {
			LOG.log(Level.FINEST, "Unable to retrieve history file for {0}",
				historyDir);
		}
		return false;
	}

	/**
	 * Deletes a history directory (e.g. Test/2013-18-01_19-53-40), first
	 * deleting the files it contains.
	 *
	 * @param dir The directory which should be deleted.
	 */
	private void deleteDirectory(final File dir) {
		try {
			for (File file : dir.listFiles()) {
				if (!file.delete()) {
					LOG.log(Level.WARNING, "problem deleting history file: {0}",
						file);
				}
			}
			if (!dir.delete()) {
				LOG.log(Level.WARNING,
					"problem deleting history directory: {0}", dir);
			}
		} catch (NullPointerException e) {
			LOG.log(Level.WARNING, "Directory already deleted or null. ", e);
		}
	}

	/**
	 * Returns the configuration data file stored in the specified history
	 * directory. It looks for a file with an 'xml' extension that is not named
	 * {@link JobConfigHistoryConsts#HISTORY_FILE}.
	 * <p>
	 * Relies on the assumption that random '.xml' files will not appear in the
	 * history directories.
	 * <p>
	 * Checks that we are in an actual 'history directory' to prevent use for
	 * getting random xml files.
	 *
	 * @param historyDir The history directory to look under.
	 * @return The configuration file or null if no file is found.
	 */
	public static File getConfigFile(final File historyDir) {
		File configFile = null;
		if (HistoryFileFilter.accepts(historyDir)) {
			// get the *.xml file that is not the
			// JobConfigHistoryConsts.HISTORY_FILE
			// assumes random .xml files won't appear in the history directory
			try {
				final File[] listing = historyDir.listFiles();
				for (final File file : listing) {
					if (!file.getName()
						.equals(JobConfigHistoryConsts.HISTORY_FILE)
						&& file.getName().matches(".*\\.xml$")) {
						configFile = file;
					}
				}
			} catch (NullPointerException e) {
				LOG.log(Level.WARNING, "History dir is null. ", e);
			}
		}
		return configFile;
	}

	/**
	 * Determines if the {@link XmlFile} contains a duplicate of the last saved
	 * information, if there is previous history.
	 *
	 * @param xmlFile The {@link XmlFile} configuration file under consideration.
	 * @return true if previous history is accessible, and the file duplicates
	 * the previously saved information.
	 */
	boolean hasDuplicateHistory(final XmlFile xmlFile) {
		boolean isDuplicated = false;
		final ArrayList<String> timeStamps = new ArrayList<String>(
			getRevisions(xmlFile).keySet());
		if (!timeStamps.isEmpty()) {
			Collections.sort(timeStamps, Collections.reverseOrder());
			final XmlFile lastRevision = getOldRevision(xmlFile,
				timeStamps.get(0));
			try {
				if (xmlFile.asString().equals(lastRevision.asString())) {
					isDuplicated = true;
				}
			} catch (IOException e) {
				LOG.log(Level.WARNING,
					"unable to check for duplicate previous history file: {0}\n{1}",
					new Object[] {lastRevision, e});
			}
		}
		return isDuplicated;
	}

	/**
	 * Checks whether the configuration file should not be saved because it's a
	 * duplicate.
	 *
	 * @param xmlFile The config file
	 * @return True if it should be saved
	 */
	boolean checkDuplicate(final XmlFile xmlFile) {
		if (!saveDuplicates && hasDuplicateHistory(xmlFile)) {
			LOG.log(Level.FINE, "found duplicate history, skipping save of {0}",
				xmlFile);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public File[] getDeletedJobs() {
		return returnEmptyFileArrayForNull(getJobFilesIncludingThoseInFolders(DeletedFileFilter.INSTANCE));
	}

	@Override
	public File[] getDeletedJobs(final String folderName) {
		return returnEmptyFileArrayForNull(
			getJobDirectoryIncludingFolder(folderName)
				.listFiles(DeletedFileFilter.INSTANCE));
	}

	@Override
	public File[] getJobs() {
		return returnEmptyFileArrayForNull(getJobFilesIncludingThoseInFolders(NonDeletedFileFilter.INSTANCE));
	}

	@Override
	public File[] getJobs(final String folderName) {
		return returnEmptyFileArrayForNull(
			getJobDirectoryIncludingFolder(folderName)
				.listFiles(NonDeletedFileFilter.INSTANCE));
	}

	/**
	 * Returns the history directory for a job in a folder.
	 *
	 * @param folderName name of the folder.
	 * @return history directory for a job in a folder.
	 */
	private File getJobDirectoryIncludingFolder(final String folderName) {
		final String realFolderName = folderName.isEmpty()
			? folderName
			: folderName + "/jobs";
		return new File(getJobHistoryRootDir(), realFolderName);
	}

	private File[] getJobFilesIncludingThoseInFolders(final FileFilter fileFilter) {
		final List<File> folderFiles = getJobFilesIncludingThoseInFolders();

		return folderFiles.stream().filter(fileFilter::accept).toArray(File[]::new);
	}

	/**
	 * @return all jobs
	 */
	private List<File> getJobFilesIncludingThoseInFolders() {
		return getJobFilesIncludingThoseInFolders(getJobHistoryRootDir());
	}

	private boolean isFolder(File file) {
		//a file is a jenkins-folder if its contained in a "jobs" directory and has one itself.
		boolean hasJobsSubdirectory = false;
		File[] files = file.listFiles();
		if (files == null) {
			return false;
		}
		for (File child : files) {
			if (child.getName().equals("jobs")) {
				hasJobsSubdirectory = true;
				break;
			}
		}
		return file.getParentFile().getName().equals("jobs") && file.isDirectory() && hasJobsSubdirectory;
	}

	private boolean isJobFile(File file) {
		return file.getParentFile().getName().equals("jobs");
	}

	private File getSubDirectory(File file, String subdirectoryName) throws FileNotFoundException {
		FileNotFoundException up = new FileNotFoundException("File " + new File(file, subdirectoryName).toString() + " not found.");

		File[] files = file.listFiles();
		if (files == null) {
			throw up;
		}
		for (File child : files) {
			if (child.getName().equals(subdirectoryName)) {
				return child;
			}
		}

		throw up;
	}

	private List<File> getJobFilesIncludingThoseInFolders(File fromFile) {
		List<File> folderNames = new LinkedList<File>();

		File[] currentChildren = fromFile.listFiles();
		if (currentChildren == null) {
			return folderNames;
		}
		for (File child : currentChildren) {
			if (isFolder(child)) {
				//get everything from the jobs subdirectory (which it has)
				try {
					folderNames.addAll(getJobFilesIncludingThoseInFolders(getSubDirectory(child, "jobs")));
				} catch (FileNotFoundException e) {
					//do nothing, this cannot happen. If it happens, it should probably be logged.
					LOG.log(Level.SEVERE, "File not found although it should have been found: " + new File(child, "jobs"));
				}
			} else if (isJobFile(child)) {
				//stop recursion, for the job found can't be a folder.
				folderNames.add(child);
			}
		}
		return folderNames;
	}

	@Override
	public File[] getSystemConfigs() {
		return returnEmptyFileArrayForNull(
			historyRootDir.listFiles(NonJobsDirectoryFileFilter.INSTANCE));
	}

	/**
	 * Returns an empty array when array is null.
	 *
	 * @param array file array.
	 * @return an empty array when array is null.
	 */
	private File[] returnEmptyFileArrayForNull(final File[] array) {
		if (array != null) {
			return array;
		} else {
			return new File[0];
		}
	}

	@Override
	public SortedMap<String, HistoryDescr> getJobHistory(final String jobName) {
		return getRevisions(new File(getJobHistoryRootDir(), jobName),
			new File(jobName));
	}

	@Override
	public SortedMap<String, HistoryDescr> getSystemHistory(final String name) {
		return getRevisions(new File(historyRootDir, name), new File(name));
	}

	@Deprecated
	public void copyHistoryAndDelete(final String oldName,
									 final String newName) {
		final File oldFile = new File(getJobHistoryRootDir(), oldName);
		final File newFile = new File(getJobHistoryRootDir(), newName);
		try {
			FileUtils.copyDirectory(oldFile, newFile);
			FileUtils.deleteDirectory(oldFile);
		} catch (IOException ex) {
			throw new IllegalArgumentException(
				"Unable to move from " + oldFile + " to " + newFile, ex);
		}
	}

	@Override
	public void createNewNode(final Node node) {
		final String content = Jenkins.XSTREAM2.toXML(node);
		createNewHistoryEntryAndSaveConfig(node, content,
			Messages.ConfigHistoryListenerHelper_CREATED(), null, null);
	}

	/**
	 * Creates a new history entry and saves the slave configuration.
	 *
	 * @param node      node.
	 * @param content   content.
	 * @param operation operation.
	 */
	private void createNewHistoryEntryAndSaveConfig(final Node node,
													final String content, final String operation, final String newName,
													final String oldName) {
		final File timestampedDir = createNewHistoryEntry(node, operation,
			newName, oldName);
		final File nodeConfigHistoryFile = new File(timestampedDir,
			"config.xml");
		PrintStream stream = null;
		try {
			stream = new PrintStream(nodeConfigHistoryFile, "UTF-8");
			stream.print(content);
		} catch (IOException ex) {
			throw new RuntimeException(
				"Unable to write " + nodeConfigHistoryFile, ex);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

	}

	@Override
	public void deleteNode(final Node node) {
		createNewHistoryEntry(node,
			Messages.ConfigHistoryListenerHelper_DELETED(), null, null);
		// final File configFile = aItem.getConfigFile().getFile();
		final File currentHistoryDir = getHistoryDirForNode(node);
		final SimpleDateFormat buildDateFormat = new SimpleDateFormat(
			"yyyyMMdd_HHmmss_SSS");
		final String timestamp = buildDateFormat.format(new Date());
		final String deletedHistoryName = node.getNodeName()
			+ DeletedFileFilter.DELETED_MARKER + timestamp;
		final File deletedHistoryDir = new File(
			currentHistoryDir.getParentFile(), deletedHistoryName);
		if (!currentHistoryDir.renameTo(deletedHistoryDir)) {
			LOG.log(Level.WARNING,
				"unable to rename deleted history dir to: {0}",
				deletedHistoryDir);
		}
	}

	@Override
	public void renameNode(final Node node, final String oldName,
						   final String newName) {
		final String onRenameDesc = " old name: " + oldName + ", new name: "
			+ newName;
		if (historyRootDir != null) {
			// final File configFile = aItem.getConfigFile().getSlaveFile();
			final File currentHistoryDir = getHistoryDirForNode(node);
			final File historyParentDir = currentHistoryDir.getParentFile();
			final File oldHistoryDir = new File(historyParentDir, oldName);
			if (oldHistoryDir.exists()) {
				final FilePath fp = new FilePath(oldHistoryDir);
				// catch all exceptions so Jenkins can continue with other
				// rename
				// tasks.
				try {
					fp.copyRecursiveTo(new FilePath(currentHistoryDir));
					fp.deleteRecursive();
					LOG.log(Level.FINEST,
						"completed move of old history files on rename.{0}",
						onRenameDesc);
				} catch (IOException e) {
					final String ioExceptionStr = "unable to move old history on rename."
						+ onRenameDesc;
					LOG.log(Level.SEVERE, ioExceptionStr, e);
				} catch (InterruptedException e) {
					final String irExceptionStr = "interrupted while moving old history on rename."
						+ onRenameDesc;
					LOG.log(Level.WARNING, irExceptionStr, e);
				}
			}

		}
		final String content = Jenkins.XSTREAM2.toXML(node);
		createNewHistoryEntryAndSaveConfig(node, content,
			Messages.ConfigHistoryListenerHelper_RENAMED(), newName,
			oldName);
	}

	@Override
	public SortedMap<String, HistoryDescr> getRevisions(final Node node) {
		final File historiesDir = getHistoryDirForNode(node);
		final File[] historyDirsOfItem = historiesDir
			.listFiles(HistoryFileFilter.INSTANCE);
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
					throw new RuntimeException("Unable to read history for "
						+ node.getDisplayName(), ex);
				}
				map.put(historyDir.getName(), historyDescription);
			}
			return map;
		}
	}

	private File getRootDir(final Node node,
							final AtomicReference<Calendar> timestampHolder) {
		final File itemHistoryDir = getHistoryDirForNode(node);
		// perform check for purge here, when we are actually going to create
		// a new directory, rather than just when we scan it in above method.
		purgeOldEntries(itemHistoryDir, maxHistoryEntries);
		return createNewHistoryDir(itemHistoryDir, timestampHolder);
	}

	private File createNewHistoryEntry(final Node node, final String operation,
									   final String newName, final String oldName) {
		try {
			final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
			final File timestampedDir = getRootDir(node, timestampHolder);
			LOG.log(Level.FINE, "{0} on {1}",
				new Object[] {this, timestampedDir});
			createHistoryXmlFile(timestampHolder.get(), timestampedDir,
				operation, newName, oldName);
			assert timestampHolder.get() != null;
			return timestampedDir;
		} catch (IOException e) {
			// If not able to create the history entry, log, but continue
			// without it.
			// A known issue is where Jenkins core fails to move the folders on
			// rename,
			// but continues as if it did.
			// Reference https://issues.jenkins-ci.org/browse/JENKINS-8318
			throw new RuntimeException(
				"Unable to create history entry for configuration file of node "
					+ node.getDisplayName(),
				e);
		}
	}

	/**
	 * Returns the configuration history directory for the given configuration
	 * file.
	 *
	 * @param node node
	 * @return The base directory where to store the history, or null if the
	 * file is not a valid Jenkins configuration file.
	 */
	private File getHistoryDirForNode(final Node node) {
		final String name = node.getNodeName();
		final File configHistoryDir = getNodeHistoryRootDir();
		return new File(configHistoryDir, name);
	}

	File getNodeHistoryRootDir() {
		return new File(historyRootDir,
			"/" + JobConfigHistoryConsts.NODES_HISTORY_DIR);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDuplicateHistory(final Node node) {
		final String content = Jenkins.XSTREAM2.toXML(node);
		boolean isDuplicated = false;
		final ArrayList<String> timeStamps = new ArrayList<String>(
			getRevisions(node).keySet());
		if (!timeStamps.isEmpty()) {
			Collections.sort(timeStamps, Collections.reverseOrder());
			final XmlFile lastRevision = getOldRevision(node,
				timeStamps.get(0));
			try {
				if (content.equals(lastRevision.asString())) {
					isDuplicated = true;
				}
			} catch (IOException e) {
				LOG.log(Level.WARNING,
					"unable to check for duplicate previous history file: {0}\n{1}",
					new Object[] {lastRevision, e});
			}
		}
		return isDuplicated;
	}

	/**
	 * Check if it is a duplicate.
	 *
	 * @param node node
	 * @return true if it is a duplicate
	 */
	private boolean checkDuplicate(final Node node) {
		if (!saveDuplicates && hasDuplicateHistory(node)) {
			LOG.log(Level.FINE, "found duplicate history, skipping save of {0}",
				node.getDisplayName());
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void saveNode(final Node node) {
		final String content = Jenkins.XSTREAM2.toXML(node);
		if (checkDuplicate(node)) {
			createNewHistoryEntryAndSaveConfig(node, content,
				Messages.ConfigHistoryListenerHelper_CHANGED(), null, null);
		}
	}

	@Override
	public XmlFile getOldRevision(final Node node, final String identifier) {
		final File historyDir = new File(getHistoryDirForNode(node),
			identifier);
		return new XmlFile(getConfigFile(historyDir));
	}

	@Override
	public boolean hasOldRevision(final Node node, final String identifier) {
		final XmlFile oldRevision = getOldRevision(node, identifier);
		return oldRevision.getFile() != null && oldRevision.getFile().exists();
	}
}
