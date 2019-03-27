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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Node;
import hudson.model.User;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;

/**
 *
 * @author Mirko Friedenhagen
 */
public class FileHistoryDaoTest {

	@Rule
	public final UnpackResourceZip unpackResourceZip = UnpackResourceZip
			.create();

	private final User mockedUser = mock(User.class);

	private final AbstractItem mockedItem = mock(AbstractItem.class);

	private File jenkinsHome;

	private XmlFile test1Config;

	private File historyRoot;

	private File test1History;

	private FileHistoryDao sutWithoutUserAndDuplicateHistory;

	private FileHistoryDao sutWithUserAndNoDuplicateHistory;

	private static final String FULL_NAME = "Full Name";

	private static final String USER_ID = "userId";

	private File test1JobDirectory;

	private final Node mockedNode = mock(Node.class);

	public FileHistoryDaoTest() {
	}

	@Before
	public void setFieldsFromUnpackResource() {
		jenkinsHome = unpackResourceZip.getRoot();
		test1Config = new XmlFile(
				unpackResourceZip.getResource("jobs/Test1/config.xml"));
		test1JobDirectory = test1Config.getFile().getParentFile();
		historyRoot = unpackResourceZip.getResource("config-history");
		test1History = new File(historyRoot, "jobs/Test1");
		sutWithoutUserAndDuplicateHistory = new FileHistoryDao(historyRoot,
				jenkinsHome, null, 0, true);
		sutWithUserAndNoDuplicateHistory = new FileHistoryDao(historyRoot,
				jenkinsHome, mockedUser, 0, false);
		when(mockedUser.getFullName()).thenReturn(FULL_NAME);
		when(mockedUser.getId()).thenReturn(USER_ID);
	}

	/**
	 * Test of createNewHistoryEntry method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateNewHistoryEntry() throws IOException {
		sutWithoutUserAndDuplicateHistory.createNewHistoryEntry(test1Config,
				"foo", null, null);
		final int newLength = getHistoryLength();
		assertEquals(7, newLength);
	}

	/**
	 * Test of createNewHistoryEntry method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateNewHistoryEntryRTE() throws IOException {
		final XmlFile xmlFile = test1Config;
		FileHistoryDao sut = new FileHistoryDao(historyRoot, jenkinsHome, null,
				0, false) {
			@Override
			File getRootDir(XmlFile xmlFile,
					AtomicReference<Calendar> timestampHolder) {
				throw new RuntimeException("oops");
			}
		};
		try {
			sut.createNewHistoryEntry(xmlFile, "foo", null, null);
			fail("Should throw RTE");
		} catch (RuntimeException e) {
			final int newLength = getHistoryLength();
			assertEquals(5, newLength);
		}
	}

	/**
	 * Test of getIdFormatter method, of class FileHistoryDao.
	 */
	@Test
	public void testGetIdFormatter() {
		SimpleDateFormat result = FileHistoryDao.getIdFormatter();
		final String formattedDate = result.format(new Date(0));
		// workaround for timezone issues, as cloudbees is in the far east :-)
		// and returns 1969 :-).
		assertThat(formattedDate, startsWith("19"));
		assertThat(formattedDate, endsWith("00-00"));
	}

	/**
	 * Test of copyConfigFile method, of class FileHistoryDao.
	 */
	@Test
	public void testCopyConfigFile() throws Exception {
		File currentConfig = test1Config.getFile();
		File timestampedDir = new File(jenkinsHome, "timestamp");
		timestampedDir.mkdir();
		FileHistoryDao.copyConfigFile(currentConfig, timestampedDir);
		final File copy = new File(timestampedDir, currentConfig.getName());
		assertTrue(copy.exists());
	}

	/**
	 * Test of copyConfigFile method, of class FileHistoryDao.
	 */
	@Test(expected = FileNotFoundException.class)
	public void testCopyConfigFileIOE() throws Exception {
		File currentConfig = test1Config.getFile();
		File timestampedDir = new File(jenkinsHome, "timestamp");
		// do *not* create the directory
		FileHistoryDao.copyConfigFile(currentConfig, timestampedDir);
	}

	/**
	 * Test of createHistoryXmlFile method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateHistoryXmlFile() throws Exception {
		testCreateHistoryXmlFile(sutWithUserAndNoDuplicateHistory, FULL_NAME);
	}

	/**
	 * Test of createHistoryXmlFile method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateHistoryXmlFileAnonym() throws Exception {
		testCreateHistoryXmlFile(sutWithoutUserAndDuplicateHistory, "unknown");
	}

	private void testCreateHistoryXmlFile(FileHistoryDao sut,
			final String fullName) throws IOException {
		Calendar timestamp = new GregorianCalendar();
		File timestampedDir = new File(historyRoot, "timestampedDir");
		sut.createHistoryXmlFile(timestamp, timestampedDir, "foo", null, null);
		final File historyFile = new File(timestampedDir,
				JobConfigHistoryConsts.HISTORY_FILE);
		assertTrue(historyFile.exists());
		final String historyContent = Util.loadFile(historyFile,
				Charset.forName("utf-8"));
		assertThat(historyContent, startsWith("<?xml"));
		assertThat(historyContent, endsWith("HistoryDescr>"));
		assertThat(historyContent, containsString("<user>" + fullName));
		assertThat(historyContent, containsString("foo"));
	}

	/**
	 * Test of createNewHistoryDir method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateNewHistoryDir() throws IOException, InterruptedException {
		final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
		final File first = FileHistoryDao.createNewHistoryDir(historyRoot,
				timestampHolder);
		assertTrue(first.exists());
		assertTrue(first.isDirectory());
		assertTrue(Files.isSymbolicLink(Paths.get(historyRoot.getAbsolutePath() + "/lastHistory")));
		// Check that symblink points to the first history dir created
		File firstSymblink = new File(Paths.get(historyRoot.getAbsolutePath() + "/lastHistory").toString());
		File firstLastRevision = new File(Util.resolveSymlink(firstSymblink));
		assertEquals(first.getAbsolutePath(), firstLastRevision.getAbsolutePath());
		// Should provoke clash
		final File second = FileHistoryDao.createNewHistoryDir(historyRoot,
				timestampHolder);
		assertTrue(second.exists());
		assertTrue(second.isDirectory());
		assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
		assertTrue(Files.isSymbolicLink(Paths.get(historyRoot.getAbsolutePath() + "/lastHistory")));
		// Check that symblink now points to the second history dir created
		File secondSymblink = new File(Paths.get(historyRoot.getAbsolutePath() + "/lastHistory").toString());
		File secondlastRevision = new File(Util.resolveSymlink(secondSymblink));
		assertEquals(second.getAbsolutePath(), secondlastRevision.getAbsolutePath());

	}

	/**
	 * Test of getRootDir method, of class FileHistoryDao.
	 */
	@Test
	public void testGetRootDir() {
		AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
		File result = sutWithoutUserAndDuplicateHistory.getRootDir(test1Config,
				timestampHolder);
		assertTrue(result.exists());
		assertThat(result.getPath(), containsString("config-history"
				+ File.separator + "jobs" + File.separator + "Test1"));
	}

	/**
	 * Test of createNewItem method, of class FileHistoryDao.
	 */
	@Test
	public void testCreateNewItem() throws IOException {
		final File jobDir = new File(jenkinsHome, "jobs/MyTest");
		jobDir.mkdirs();
		IOUtils.copy(test1Config.getFile(),
				new FileOutputStream(new File(jobDir, "config.xml")));
		when(mockedItem.getRootDir()).thenReturn(jobDir);
		sutWithUserAndNoDuplicateHistory.createNewItem(mockedItem);
		assertTrue(new File(jobDir, "config.xml").exists());
		final File historyDir = new File(jenkinsHome,
				"config-history/jobs/MyTest");
		assertTrue(historyDir.exists());

		final File lastHistory = new File(jenkinsHome,
				"config-history/jobs/MyTest/lastHistory");

		Files.isSymbolicLink(Paths.get(lastHistory.getAbsolutePath()));
		assertEquals(2, historyDir.list().length);



	}

	/**
	 * Test of saveItem method, of class FileHistoryDao.
	 */
	@Test
	public void testSaveItem_AbstractItem() throws IOException {
		when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
		sutWithUserAndNoDuplicateHistory.saveItem(mockedItem.getConfigFile());
		assertEquals(5, getHistoryLength());
	}

	/**
	 * Test of saveItem method, of class FileHistoryDao.
	 */
	@Test
	public void testSaveItem_XmlFile() {
		sutWithUserAndNoDuplicateHistory.saveItem(test1Config);
		assertEquals(5, getHistoryLength());
	}

	/**
	 * Test of saveItem method, of class FileHistoryDao.
	 */
	@Test
	public void testSaveItem_XmlFileDuplicate() {
		sutWithoutUserAndDuplicateHistory.saveItem(test1Config);
		assertEquals(7, getHistoryLength());
	}

	private int getHistoryLength() {
		return test1History.list().length;
	}

	/**
	 * Test of deleteItem method, of class FileHistoryDao.
	 */
	@Test
	public void testDeleteItem() {
		when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
		sutWithUserAndNoDuplicateHistory.deleteItem(mockedItem);
	}

	/**
	 * Test of renameItem method, of class FileHistoryDao.
	 */
	@Test
	public void testRenameItem() throws IOException {
		String newName = "NewName";
		File newJobDir = unpackResourceZip.getResource("jobs/" + newName);
		// Rename of Job is already done in Listener.
		FileUtils.touch(new File(newJobDir, "config.xml"));
		when(mockedItem.getRootDir()).thenReturn(newJobDir);
		when(mockedItem.getName()).thenReturn("Test1");
		sutWithUserAndNoDuplicateHistory.renameItem(mockedItem, "Test1",
				"NewName");
		final File newHistoryDir = new File(historyRoot, "jobs/" + newName);
		assertTrue(newHistoryDir.exists());
		assertEquals(7, newHistoryDir.list().length);
	}

	/**
	 * Test of getRevisions method, of class FileHistoryDao.
	 */
	@Test
	public void testGetRevisions_XmlFile() throws IOException {
		SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
				.getRevisions(test1Config);
		testGetRevisions(result);
	}

	private void testGetRevisions(SortedMap<String, HistoryDescr> result) {
		assertEquals(5, result.size());
		assertEquals("2012-11-21_11-29-12", result.firstKey());
		assertEquals("2012-11-21_11-42-05", result.lastKey());
		final HistoryDescr firstValue = result.get(result.firstKey());
		final HistoryDescr lastValue = result.get(result.lastKey());
		assertEquals("Created", firstValue.getOperation());
		assertEquals("anonymous", firstValue.getUserID());
		assertEquals("Changed", lastValue.getOperation());
	}

	/**
	 * Test of getRevisions method, of class FileHistoryDao.
	 */
	@Test
	public void testGetRevisionsForItemWithoutHistory() throws IOException {
		final String jobWithoutHistory = "NewJobWithoutHistory";
		final File configFile = new File(
				unpackResourceZip.getResource("jobs/" + jobWithoutHistory),
				"config.xml");
		FileUtils.touch(configFile);
		assertEquals(0, sutWithUserAndNoDuplicateHistory
				.getRevisions(new XmlFile(configFile)).size());
	}

	/**
	 * Test of getOldRevision method, of class FileHistoryDao.
	 */
	@Test
	public void testGetOldRevision_Item() throws IOException {
		when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
		String identifier = "2012-11-21_11-42-05";
		final XmlFile result = sutWithUserAndNoDuplicateHistory
				.getOldRevision(mockedItem, identifier);
		testGetOldRevision(result);
	}

	/**
	 * Test of getOldRevision method, of class FileHistoryDao.
	 */
	@Test
	public void testGetOldRevision_XmlFile() throws IOException {
		String identifier = "2012-11-21_11-42-05";
		final XmlFile result = sutWithUserAndNoDuplicateHistory
				.getOldRevision(test1Config, identifier);
		testGetOldRevision(result);
	}

	private void testGetOldRevision(final XmlFile result) throws IOException {
		final String xml = result.asString();
		assertThat(xml, startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
		assertThat(xml, endsWith("</project>"));
	}

	/**
	 * Test of getOldRevision method, of class FileHistoryDao.
	 */
	@Test
	public void testHasOldRevision_Item() throws IOException {
		when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
		assertTrue(sutWithUserAndNoDuplicateHistory.hasOldRevision(
				mockedItem.getConfigFile(), "2012-11-21_11-42-05"));
		assertFalse(sutWithUserAndNoDuplicateHistory.hasOldRevision(
				mockedItem.getConfigFile(), "1914-11-21_11-42-05"));
	}

	/**
	 * Test of getOldRevision method, of class FileHistoryDao.
	 */
	@Test
	public void testHasOldRevision_XmlFile() throws IOException {
		assertTrue(sutWithUserAndNoDuplicateHistory.hasOldRevision(test1Config,
				"2012-11-21_11-42-05"));
		assertFalse(sutWithUserAndNoDuplicateHistory.hasOldRevision(test1Config,
				"1914-11-21_11-42-05"));
	}

	/**
	 * Test of getHistoryDir method, of class FileHistoryDao.
	 */
	@Test
	public void testGetHistoryDir() {
		final File configFile = test1Config.getFile();
		File result = sutWithUserAndNoDuplicateHistory
				.getHistoryDir(configFile);
		assertEquals(test1History, result);
	}

	/**
	 * Test of getHistoryDir method, of class FileHistoryDao.
	 */
	@Test
	public void testGetHistoryDirWithFolder() throws IOException {
		String folderName = "FolderName";
		final File destDir = unpackResourceZip.getResource(
				"config-history/jobs/" + folderName + "/jobs/Test1");
		// Create folders
		FileUtils.copyDirectory(
				unpackResourceZip.getResource("config-history/jobs/Test1"),
				destDir);
		final File configFile = unpackResourceZip
				.getResource("jobs/" + folderName + "/jobs/Test1/config.xml");
		File result = sutWithUserAndNoDuplicateHistory
				.getHistoryDir(configFile);
		assertEquals(destDir, result);
	}

	/**
	 * Test of getHistoryDir method, of class FileHistoryDao.
	 */
	@Test
	public void testGetHistoryDirOfSystemXml() throws IOException {
		final XmlFile systemXmlFile = new XmlFile(
				new File(jenkinsHome, "jenkins.xml"));
		final File configFile = systemXmlFile.getFile();
		File result = sutWithUserAndNoDuplicateHistory
				.getHistoryDir(configFile);
		assertThat(result.getPath(), endsWith(File.separatorChar + "jenkins"));
	}

	/**
	 * Test of getJobHistoryRootDir method, of class FileHistoryDao.
	 */
	@Test
	public void testGetJobHistoryRootDir() {
		assertEquals(unpackResourceZip.getResource("config-history/jobs"),
				sutWithUserAndNoDuplicateHistory.getJobHistoryRootDir());
	}

	/**
	 * Test of purgeOldEntries method, of class FileHistoryDao.
	 */
	@Test
	public void testPurgeOldEntriesNoEntriesToDelete() {
		final int maxEntries = 0;
		final int expectedLength = 5;
		testPurgeOldEntries(maxEntries, expectedLength);
	}

	/**
	 * Test of purgeOldEntries method, of class FileHistoryDao.
	 */
	@Test
	public void testPurgeOldEntriesOnlyTwoExistingOneBecauseOfCreatedStatus() {
		final int maxEntries = 2;
		final int expectedLength = 2;
		testPurgeOldEntries(maxEntries, expectedLength);
	}

	private void testPurgeOldEntries(int maxEntries, final int expectedLength) {
		sutWithUserAndNoDuplicateHistory.purgeOldEntries(test1History,
				maxEntries);
		final int newLength = getHistoryLength();
		assertEquals(expectedLength, newLength);
	}

	/**
	 * Test of hasDuplicateHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testHasDuplicateHistoryNoDuplicates() throws IOException {
		XmlFile xmlFile = new XmlFile(
				new File(test1History, "2012-11-21_11-41-14/history.xml"));
		final File configFile = new File(test1JobDirectory, "config.xml");
		IOUtils.write(xmlFile.asString(), new FileOutputStream(configFile));
		boolean result = sutWithUserAndNoDuplicateHistory
				.hasDuplicateHistory(new XmlFile(configFile));
		assertEquals(false, result);
	}

	/**
	 * Test of hasDuplicateHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testHasDuplicateHistoryDuplicates() throws IOException {
		final File configFile = new File(test1JobDirectory, "config.xml");
		boolean result = sutWithUserAndNoDuplicateHistory
				.hasDuplicateHistory(new XmlFile(configFile));
		assertEquals(true, result);
	}

	/**
	 * Test of hasDuplicateHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testHasDuplicateHistoryNoHistory() throws IOException {
		final File configFile = new File(jenkinsHome, "jobs/Test2/config.xml");
		boolean result = sutWithUserAndNoDuplicateHistory
				.hasDuplicateHistory(new XmlFile(configFile));
		assertEquals(false, result);
	}

	/**
	 * Test of checkDuplicate method, of class FileHistoryDao.
	 */
	@Test
	public void testCheckDuplicate() throws IOException {
		XmlFile xmlFile = new XmlFile(
				new File(test1JobDirectory, "config.xml"));
		boolean result = sutWithoutUserAndDuplicateHistory
				.checkDuplicate(xmlFile);
		assertEquals(true, result);
	}

	/**
	 * Test of checkDuplicate method, of class FileHistoryDao.
	 */
	@Test
	public void testCheckDuplicateSkip() {
		XmlFile xmlFile = new XmlFile(
				new File(test1JobDirectory, "config.xml"));
		boolean result = sutWithUserAndNoDuplicateHistory
				.checkDuplicate(xmlFile);
		assertEquals(false, result);
	}

	/**
	 * Test of checkDuplicate method, of class FileHistoryDao.
	 */
	@Test
	public void testCheckDuplicateHasDuplicate() {
		XmlFile xmlFile = new XmlFile(
				new File(jenkinsHome, "jobs/Test2/config.xml"));
		boolean result = sutWithUserAndNoDuplicateHistory
				.checkDuplicate(xmlFile);
		assertEquals(true, result);
	}

	/**
	 * Test of getSystemConfigs method, of class FileHistoryDao.
	 */
	@Test
	public void testGetSystemConfigs() {
		final File[] systemConfigs = sutWithoutUserAndDuplicateHistory
				.getSystemConfigs();
		assertEquals(1, systemConfigs.length);
		assertEquals("config", systemConfigs[0].getName());
	}

	/**
	 * Test of getSystemConfigs method, of class FileHistoryDao.
	 */
	@Test
	public void testGetSystemConfigsWithoutData() {
		new File(jenkinsHome, "config-history")
				.renameTo(new File(jenkinsHome, "invalid"));
		final File[] systemConfigs = sutWithoutUserAndDuplicateHistory
				.getSystemConfigs();
		assertEquals(0, systemConfigs.length);
	}

	/**
	 * Test of getDeletedJobs method, of class FileHistoryDao.
	 */
	@Test
	public void testGetDeletedJobs() {
		final File[] deletedJobs = sutWithoutUserAndDuplicateHistory
				.getDeletedJobs("");
		assertEquals(1, deletedJobs.length);
		final String name = deletedJobs[0].getName();
		assertThat(name, containsString("Foo"));
	}

	/**
	 * Test of getDeletedJobs method, of class FileHistoryDao.
	 */
	@Test
	public void testGetJobs() {
		final File[] jobs = sutWithoutUserAndDuplicateHistory.getJobs("");
		assertEquals(1, jobs.length);
		final String name = jobs[0].getName();
		assertThat(name, containsString("Test1"));
	}

	/**
	 * Test of getJobHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testGetJobHistory() {
		final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
				.getJobHistory("Test1");
		assertEquals(5, result.size());
		assertEquals("2012-11-21_11-29-12", result.firstKey());
	}

	/**
	 * Test of getJobHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testGetJobHistoryNonExistent() {
		final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
				.getJobHistory("JobDoesNotExist");
		assertTrue(result.isEmpty());
	}

	/**
	 * Test of getSystemHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testGetSystemHistory() {
		final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
				.getSystemHistory("config");
		assertEquals(5, result.size());
		assertEquals("2013-01-18_17-34-22", result.firstKey());
	}

	/**
	 * Test of getSystemHistory method, of class FileHistoryDao.
	 */
	@Test
	public void testGetSystemHistoryNonExistent() {
		final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
				.getSystemHistory("config-does-not-exist");
		assertTrue(result.isEmpty());
	}

	/**
	 * Test of copyHistoryAndDelete method, of class FileHistoryDao.
	 */
	@Test
	public void testMoveHistory() {
		sutWithUserAndNoDuplicateHistory
				.copyHistoryAndDelete("Foo_deleted_20130830_223932_071", "Foo");
		assertEquals(3,
				sutWithUserAndNoDuplicateHistory.getJobHistory("Foo").size());
	}

	/**
	 * Test of copyHistoryAndDelete method, of class FileHistoryDao.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMoveHistoryIOException() {
		sutWithUserAndNoDuplicateHistory.copyHistoryAndDelete("Test1", "Test1");
	}

	@Test
	public void testCreateNewNode() throws Exception {
		when(mockedNode.getNodeName()).thenReturn("slave1");
		sutWithUserAndNoDuplicateHistory.createNewNode(mockedNode);
		File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
		File revisions = new File(file, "slave1");
		File[] revisionsList = revisions.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !name.matches("lastHistory");
			}
		});
		assertEquals("Slave1 should have only one save history.", 1,
				revisionsList.length);
		File config = new File(revisionsList[0], "config.xml");
		assertTrue("File config.xml should be saved.", config.exists());
		File history = new File(revisionsList[0],
				JobConfigHistoryConsts.HISTORY_FILE);
		assertTrue("File history.xml should be saved.", history.exists());
	}

	/**
	 * Runs a test, which renames or deletes a node. This method decouples the
	 * shared logic from {@link #testDeleteNode()} and
	 * {@link #testRenameNode()}.
	 */
	private void printTestData(Callable<Void> func) throws Exception {
		when(mockedNode.getNodeName()).thenReturn("slave1");
		File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
		File revisions = new File(file, "slave1");
		File revision = new File(revisions, "2014-01-20_10-12-34");
		revision.mkdirs();
		File config = new File(revision, "config.xml");
		File history = new File(revision, JobConfigHistoryConsts.HISTORY_FILE);

		HistoryDescr descr = new HistoryDescr("User", "user", "created",
				"2014-01-20_10-12-34", null, null);
		OutputStream configsFile = new PrintStream(config);
		OutputStream historyFile = new FileOutputStream(history);
		try {
			Jenkins.XSTREAM2.toXMLUTF8(mockedNode, configsFile);
			Jenkins.XSTREAM2.toXMLUTF8(descr, historyFile);
		} finally {
			configsFile.close();
			historyFile.close();
		}

		// Call method an check that all files have been deleted
		func.call();
		assertFalse("File config.xml should be deleted.", config.exists());
		assertFalse("File history.xml should be deleted.", history.exists());
		assertFalse("Previous revision directory should be deleted",
				revision.exists());
	}

	@Test
	public void testDeleteNode() throws Exception {
		printTestData(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				sutWithUserAndNoDuplicateHistory.deleteNode(mockedNode);
				return null;
			}
		});
	}

	@Test
	public void testRenameNode() throws Exception {
		printTestData(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				sutWithUserAndNoDuplicateHistory.renameNode(mockedNode,
						mockedNode.getNodeName(),
						mockedNode.getNodeName() + "_renamed");
				return null;
			}
		});
	}

	@Test
	public void testGetRevisions() throws Exception {
		when(mockedNode.getNodeName()).thenReturn("slave1");
		createNodeRevision("2014-01-18_10-12-34", mockedNode);
		createNodeRevision("2014-01-19_10-12-34", mockedNode);
		createNodeRevision("2014-01-20_10-12-34", mockedNode);
		createNodeRevision("2014-01-20_10-21-34", mockedNode);
		SortedMap<String, HistoryDescr> revisions = sutWithUserAndNoDuplicateHistory
				.getRevisions(mockedNode);
		assertNotNull("Revisiosn 2014-01-18_10-12-34 should be returned.",
				revisions.get("2014-01-18_10-12-34"));
		assertNotNull("Revisiosn 2014-01-19_10-12-34 should be returned.",
				revisions.get("2014-01-19_10-12-34"));
		assertNotNull("Revisiosn 2014-01-20_10-12-34 should be returned.",
				revisions.get("2014-01-20_10-12-34"));
		assertNotNull("Revisiosn 2014-01-20_10-21-34 should be returned.",
				revisions.get("2014-01-20_10-21-34"));
	}

	private File createNodeRevision(String timestamp, Node mockedNode)
			throws Exception {
		File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
		File revisions = new File(file, "slave1");
		File revision = new File(revisions, timestamp);
		revision.mkdirs();
		Jenkins.XSTREAM2.toXMLUTF8(mockedNode,
				new PrintStream(new File(revision, "config.xml")));
		HistoryDescr descr = new HistoryDescr("User", "user", "created",
				timestamp, null, null);
		Jenkins.XSTREAM2.toXMLUTF8(descr, new FileOutputStream(
				new File(revision, JobConfigHistoryConsts.HISTORY_FILE)));
		return revision;
	}

	@Test
	public void testSaveNode() {
		when(mockedNode.getNodeName()).thenReturn("slave1");
		File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
		File revisions = new File(file, "slave1");
		sutWithUserAndNoDuplicateHistory.saveNode(mockedNode);
		assertEquals("New revision should be saved.", 2,
				revisions.list().length);
	}

	@Test
	public void testGetOldRevision_Node() throws Exception {
		when(mockedNode.getNodeName()).thenReturn("slave1");
		when(mockedNode.getNumExecutors()).thenReturn(1);
		File revision1 = createNodeRevision("2014-01-18_10-12-34", mockedNode);
		when(mockedNode.getNumExecutors()).thenReturn(2);
		File revision2 = createNodeRevision("2014-01-19_10-12-34", mockedNode);
		when(mockedNode.getNumExecutors()).thenReturn(3);
		File revision3 = createNodeRevision("2014-01-20_10-12-34", mockedNode);
		XmlFile file1 = sutWithUserAndNoDuplicateHistory
				.getOldRevision(mockedNode, "2014-01-18_10-12-34");
		XmlFile file2 = sutWithUserAndNoDuplicateHistory
				.getOldRevision(mockedNode, "2014-01-19_10-12-34");
		XmlFile file3 = sutWithUserAndNoDuplicateHistory
				.getOldRevision(mockedNode, "2014-01-20_10-12-34");
		assertEquals(
				"Should return config.xml file of revision 2014-01-18_10-12-34",
				new File(revision1, "config.xml").getAbsolutePath(),
				file1.getFile().getAbsolutePath());
		assertEquals(
				"Should return config.xml file of revision 2014-01-19_10-12-34",
				new File(revision2, "config.xml").getAbsolutePath(),
				file2.getFile().getAbsolutePath());
		assertEquals(
				"Should return config.xml file of revision 2014-01-20_10-12-34",
				new File(revision3, "config.xml").getAbsolutePath(),
				file3.getFile().getAbsolutePath());
	}

}
