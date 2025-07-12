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

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.User;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for FileHistoryDao.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class FileHistoryDaoTest {

    private static final String FULL_NAME = "Full Name";
    private static final String USER_ID = "userId";

    private final User mockedUser = mock(User.class);
    private final AbstractItem mockedItem = mock(AbstractItem.class);

    private JenkinsRule jenkinsRule;
    private UnpackResourceZip unpackResourceZip;
    private File jenkinsHome;
    private XmlFile test1Config;
    private File historyRoot;
    private File test1History;
    private FileHistoryDao sutWithoutUserAndDuplicateHistory;
    private FileHistoryDao sutWithUserAndNoDuplicateHistory;
    private File test1JobDirectory;


    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkinsRule = rule;
        unpackResourceZip = UnpackResourceZip.create();
        jenkinsHome = unpackResourceZip.getRoot();
        test1Config = new XmlFile(
                unpackResourceZip.getResource("jobs/Test1/config.xml"));
        test1JobDirectory = test1Config.getFile().getParentFile();
        historyRoot = unpackResourceZip.getResource("config-history");
        test1History = new File(historyRoot, "jobs/Test1");
        sutWithoutUserAndDuplicateHistory = new FileHistoryDao(historyRoot,
                jenkinsHome, null, 0, true);
        when(mockedUser.getFullName()).thenReturn(FULL_NAME);
        when(mockedUser.getId()).thenReturn(USER_ID);
        sutWithUserAndNoDuplicateHistory = new FileHistoryDao(historyRoot,
                jenkinsHome, new MimickedUser(mockedUser), 0, false);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (unpackResourceZip != null) {
            unpackResourceZip.cleanUp();
        }
    }

    /**
     * Test of createNewHistoryEntry method, of class FileHistoryDao.
     */
    @Test
    void testCreateNewHistoryEntry() {
        sutWithoutUserAndDuplicateHistory.createNewHistoryEntry(test1Config,
                "foo", null, null, null);
        final int newLength = getHistoryLength();
        assertEquals(6, newLength);
    }

    /**
     * Test of createNewHistoryEntry method, of class FileHistoryDao.
     */
    @Test
    void testCreateNewHistoryEntryRTE() {
        final XmlFile xmlFile = test1Config;
        FileHistoryDao sut = new FileHistoryDao(historyRoot, jenkinsHome, null,
                0, false) {
            @Override
            File getRootDir(XmlFile xmlFile,
                            AtomicReference<Calendar> timestampHolder) {
                throw new RuntimeException("oops");
            }
        };
        assertThrows(RuntimeException.class,
                () -> sut.createNewHistoryEntry(xmlFile, "foo", null, null, null));

        final int newLength = getHistoryLength();
        assertEquals(5, newLength);
    }

    /**
     * Test of getIdFormatter method, of class FileHistoryDao.
     */
    @Test
    void testGetIdFormatter() {
        SimpleDateFormat result = FileHistoryDao.getIdFormatter();
        // setting a default timezone to avoid timezone issues in different environments
        result.setTimeZone(TimeZone.getTimeZone("UTC"));

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
    void testCopyConfigFile() throws Exception {
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
    @Test
    void testCopyConfigFileIOE() {
        File currentConfig = test1Config.getFile();
        File timestampedDir = new File(jenkinsHome, "timestamp");
        assertThrows(FileNotFoundException.class, () ->
            // do *not* create the directory
            FileHistoryDao.copyConfigFile(currentConfig, timestampedDir));
    }

    /**
     * Test of createHistoryXmlFile method, of class FileHistoryDao.
     */
    @Test
    void testCreateHistoryXmlFile() throws Exception {
        testCreateHistoryXmlFile(sutWithUserAndNoDuplicateHistory, FULL_NAME);
    }

    /**
     * Test of createHistoryXmlFile method, of class FileHistoryDao.
     */
    @Test
    void testCreateHistoryXmlFileAnonym() throws Exception {
        testCreateHistoryXmlFile(sutWithoutUserAndDuplicateHistory, "unknown");
    }

    private void testCreateHistoryXmlFile(FileHistoryDao sut,
                                          final String fullName) throws IOException {
        Calendar timestamp = new GregorianCalendar();
        File timestampedDir = new File(historyRoot, "timestampedDir");
        sut.createHistoryXmlFile(timestamp, timestampedDir, "foo", null, null, null);
        final File historyFile = new File(timestampedDir,
                JobConfigHistoryConsts.HISTORY_FILE);
        assertTrue(historyFile.exists());
        final String historyContent = Util.loadFile(historyFile,
                StandardCharsets.UTF_8);
        assertThat(historyContent, startsWith("<?xml"));
        assertThat(historyContent, endsWith("HistoryDescr>"));
        assertThat(historyContent, containsString("<user>" + fullName));
        assertThat(historyContent, containsString("foo"));
    }

    /**
     * Test of createNewHistoryDir method, of class FileHistoryDao.
     */
    @Test
    void testCreateNewHistoryDir() throws IOException {
        final AtomicReference<Calendar> timestampHolder = new AtomicReference<>();
        final File first = FileHistoryDao.createNewHistoryDir(historyRoot,
                timestampHolder);
        assertTrue(first.exists());
        assertTrue(first.isDirectory());
        // Should provoke clash
        final File second = FileHistoryDao.createNewHistoryDir(historyRoot,
                timestampHolder);
        assertTrue(second.exists());
        assertTrue(second.isDirectory());
        assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
    }

    /**
     * Test of getRootDir method, of class FileHistoryDao.
     */
    @Test
    void testGetRootDir() throws IOException {
        AtomicReference<Calendar> timestampHolder = new AtomicReference<>();
        File result = sutWithoutUserAndDuplicateHistory.getRootDir(test1Config,
                timestampHolder);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        assertTrue(result.exists());
        assertThat(result.getPath(), containsString("config-history"
                + File.separator + "jobs" + File.separator + "Test1"));
    }

    /**
     * Test of createNewItem method, of class FileHistoryDao.
     */
    @Test
    void testCreateNewItem() throws IOException {
        final File jobDir = new File(jenkinsHome, "jobs/MyTest");
        jobDir.mkdirs();
        FileUtils.copyFile(test1Config.getFile(), new File(jobDir, "config.xml"));
        when(mockedItem.getRootDir()).thenReturn(jobDir);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        sutWithUserAndNoDuplicateHistory.createNewItem(mockedItem);
        assertTrue(new File(jobDir, "config.xml").exists());
        final File historyDir = new File(jenkinsHome,
                "config-history/jobs/MyTest");
        assertTrue(historyDir.exists());
        assertEquals(1, historyDir.list().length);
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    void testSaveItem_AbstractItem() {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        sutWithUserAndNoDuplicateHistory.saveItem(mockedItem.getConfigFile());
        assertEquals(5, getHistoryLength());
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    void testSaveItem_XmlFile() {
        sutWithUserAndNoDuplicateHistory.saveItem(test1Config);
        assertEquals(5, getHistoryLength());
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    void testSaveItem_XmlFileDuplicate() {
        sutWithoutUserAndDuplicateHistory.saveItem(test1Config);
        assertEquals(6, getHistoryLength());
    }

    private int getHistoryLength() {
        return test1History.list().length;
    }

    /**
     * Test of deleteItem method, of class FileHistoryDao.
     */
    @Test
    void testDeleteItem() {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        sutWithUserAndNoDuplicateHistory.deleteItem(mockedItem);
    }

    /**
     * Test of renameItem method, of class FileHistoryDao.
     */
    @Test
    void testRenameItem() throws IOException {
        String newName = "NewName";
        File newJobDir = unpackResourceZip.getResource("jobs/" + newName);
        // Rename of Job is already done in Listener.
        FileUtils.touch(new File(newJobDir, "config.xml"));
        when(mockedItem.getRootDir()).thenReturn(newJobDir);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        when(mockedItem.getName()).thenReturn("Test1");
        sutWithUserAndNoDuplicateHistory.renameItem(mockedItem, "Test1",
                "NewName");
        final File newHistoryDir = new File(historyRoot, "jobs/" + newName);
        assertTrue(newHistoryDir.exists());
        assertEquals(6, newHistoryDir.list().length);
    }

    /**
     * Test of getRevisions method, of class FileHistoryDao.
     */
    @Test
    void testGetRevisions_XmlFile() {
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
    void testGetRevisionsForItemWithoutHistory() throws IOException {
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
    void testGetOldRevision_Item() throws IOException {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        String identifier = "2012-11-21_11-42-05";
        final XmlFile result = sutWithUserAndNoDuplicateHistory
                .getOldRevision(mockedItem, identifier);
        testGetOldRevision(result);
    }

    @Test
    void testGetRevisionAmount() throws IOException {
        assertEquals(5, sutWithUserAndNoDuplicateHistory.getRevisionAmount(test1Config));

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        assertEquals(2, getJenkinsRuleSut().getRevisionAmount(freeStyleProject.getConfigFile()));
    }

    @Test
    void testGetJobRevisionAmount() throws IOException, InterruptedException {
        assertEquals(6, sutWithUserAndNoDuplicateHistory.getJobRevisionAmount());


        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        assertEquals(2, getJenkinsRuleSut().getJobRevisionAmount());
        jenkinsRule.createFreeStyleProject();
        jenkinsRule.createFreeStyleProject();
        assertEquals(6, getJenkinsRuleSut().getJobRevisionAmount());
        freeStyleProject.renameTo("a9832jpokk");
        assertEquals(7, getJenkinsRuleSut().getJobRevisionAmount());
        freeStyleProject.delete();
        //2 Jobs created (=2*2 revisions) + 1 deleted (=1 revision)
        assertEquals(5, getJenkinsRuleSut().getJobRevisionAmount());
    }

    @Test
    void testGetJobRevisionAmount_Single() throws IOException, InterruptedException {
        assertEquals(5, sutWithUserAndNoDuplicateHistory.getJobRevisionAmount(test1JobDirectory.getName()));

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        assertEquals(2, getJenkinsRuleSut().getJobRevisionAmount(freeStyleProject.getFullName()));
        freeStyleProject.renameTo("asdoknlwqkn");
        assertEquals(3, getJenkinsRuleSut().getJobRevisionAmount(freeStyleProject.getFullName()));
        freeStyleProject.delete();
        assertEquals(0, getJenkinsRuleSut().getJobRevisionAmount(freeStyleProject.getFullName()));
    }

    @Test
    void testGetSystemConfigsMap() {
        Iterator<String> it =
                Arrays.asList(new String[]{"config", "jenkins.model.JenkinsLocationConfiguration", "jenkins.telemetry.Correlator"}).iterator();
        getJenkinsRuleSut().getSystemConfigsMap().forEach((k, v) -> {
            assertEquals(it.next(), k);
            assertInstanceOf(LazyHistoryDescr.class, v);
        });
        getJenkinsRuleSut().getSystemConfigsMap().forEach((k, v) -> System.out.println(new Pair<>(k, v)));
    }

    /**
     * Test of getOldRevision method, of class FileHistoryDao.
     */
    @Test
    void testGetOldRevision_XmlFile() throws IOException {
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
    void testHasOldRevision_Item() {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        when(mockedItem.getConfigFile()).thenCallRealMethod();
        assertTrue(sutWithUserAndNoDuplicateHistory.hasOldRevision(
                mockedItem.getConfigFile(), "2012-11-21_11-42-05"));
        assertFalse(sutWithUserAndNoDuplicateHistory.hasOldRevision(
                mockedItem.getConfigFile(), "1914-11-21_11-42-05"));
    }

    /**
     * Test of getOldRevision method, of class FileHistoryDao.
     */
    @Test
    void testHasOldRevision_XmlFile() {
        assertTrue(sutWithUserAndNoDuplicateHistory.hasOldRevision(test1Config,
                "2012-11-21_11-42-05"));
        assertFalse(sutWithUserAndNoDuplicateHistory.hasOldRevision(test1Config,
                "1914-11-21_11-42-05"));
    }

    /**
     * Test of getHistoryDir method, of class FileHistoryDao.
     */
    @Test
    void testGetHistoryDir() {
        final File configFile = test1Config.getFile();
        File result = sutWithUserAndNoDuplicateHistory
                .getHistoryDir(configFile);
        assertEquals(test1History, result);
    }

    /**
     * Test of getHistoryDir method, of class FileHistoryDao.
     */
    @Test
    void testGetHistoryDirWithFolder() throws IOException {
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
    void testGetHistoryDirOfSystemXml() {
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
    void testGetJobHistoryRootDir() {
        assertEquals(unpackResourceZip.getResource("config-history/jobs"),
                sutWithUserAndNoDuplicateHistory.getJobHistoryRootDir());
    }

    /**
     * Test of purgeOldEntries method, of class FileHistoryDao.
     */
    @Test
    void testPurgeOldEntriesNoEntriesToDelete() {
        final int maxEntries = 0;
        final int expectedLength = 5;
        testPurgeOldEntries(maxEntries, expectedLength);
    }

    /**
     * Test of purgeOldEntries method, of class FileHistoryDao.
     */
    @Test
    void testPurgeOldEntriesOnlyTwoExistingOneBecauseOfCreatedStatus() {
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
    void testHasDuplicateHistoryNoDuplicates() throws IOException {
        XmlFile xmlFile = new XmlFile(
                new File(test1History, "2012-11-21_11-41-14/history.xml"));
        final File configFile = new File(test1JobDirectory, "config.xml");
        IOUtils.write(xmlFile.asString(), new FileOutputStream(configFile), StandardCharsets.UTF_8);
        boolean result = sutWithUserAndNoDuplicateHistory
                .hasDuplicateHistory(new XmlFile(configFile));
        assertFalse(result);
    }

    /**
     * Test of hasDuplicateHistory method, of class FileHistoryDao.
     */
    @Test
    void testHasDuplicateHistoryDuplicates() {
        final File configFile = new File(test1JobDirectory, "config.xml");
        boolean result = sutWithUserAndNoDuplicateHistory
                .hasDuplicateHistory(new XmlFile(configFile));
        assertTrue(result);
    }

    /**
     * Test of hasDuplicateHistory method, of class FileHistoryDao.
     */
    @Test
    void testHasDuplicateHistoryNoHistory() {
        final File configFile = new File(jenkinsHome, "jobs/Test2/config.xml");
        boolean result = sutWithUserAndNoDuplicateHistory
                .hasDuplicateHistory(new XmlFile(configFile));
        assertFalse(result);
    }

    /**
     * Test of checkDuplicate method, of class FileHistoryDao.
     */
    @Test
    void testCheckDuplicate() {
        XmlFile xmlFile = new XmlFile(
                new File(test1JobDirectory, "config.xml"));
        boolean result = sutWithoutUserAndDuplicateHistory
                .checkDuplicate(xmlFile);
        assertTrue(result);
    }

    /**
     * Test of checkDuplicate method, of class FileHistoryDao.
     */
    @Test
    void testCheckDuplicateSkip() {
        XmlFile xmlFile = new XmlFile(
                new File(test1JobDirectory, "config.xml"));
        boolean result = sutWithUserAndNoDuplicateHistory
                .checkDuplicate(xmlFile);
        assertFalse(result);
    }

    /**
     * Test of checkDuplicate method, of class FileHistoryDao.
     */
    @Test
    void testCheckDuplicateHasDuplicate() {
        XmlFile xmlFile = new XmlFile(
                new File(jenkinsHome, "jobs/Test2/config.xml"));
        boolean result = sutWithUserAndNoDuplicateHistory
                .checkDuplicate(xmlFile);
        assertTrue(result);
    }

    /**
     * Test of getSystemConfigs method, of class FileHistoryDao.
     */
    @Test
    void testGetSystemConfigs() {
        final File[] systemConfigs = sutWithoutUserAndDuplicateHistory
                .getSystemConfigs();
        assertEquals(1, systemConfigs.length);
        assertEquals("config", systemConfigs[0].getName());
    }

    /**
     * Test of getSystemConfigs method, of class FileHistoryDao.
     */
    @Test
    void testGetSystemConfigsWithoutData() {
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
    void testGetDeletedJobs() {
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
    void testGetJobs() {
        final File[] jobs = sutWithoutUserAndDuplicateHistory.getJobs("");
        assertEquals(1, jobs.length);
        final String name = jobs[0].getName();
        assertThat(name, containsString("Test1"));
    }

    /**
     * Test of getJobHistory method, of class FileHistoryDao.
     */
    @Test
    void testGetJobHistory() {
        final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
                .getJobHistory("Test1");
        assertEquals(5, result.size());
        assertEquals("2012-11-21_11-29-12", result.firstKey());
    }

    /**
     * Test of getJobHistory method, of class FileHistoryDao.
     */
    @Test
    void testGetJobHistoryNonExistent() {
        final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
                .getJobHistory("JobDoesNotExist");
        assertTrue(result.isEmpty());
    }

    /**
     * Test of getSystemHistory method, of class FileHistoryDao.
     */
    @Test
    void testGetSystemHistory() {
        final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
                .getSystemHistory("config");
        assertEquals(5, result.size());
        assertEquals("2013-01-18_17-34-22", result.firstKey());
    }

    /**
     * Test of getSystemHistory method, of class FileHistoryDao.
     */
    @Test
    void testGetSystemHistoryNonExistent() {
        final SortedMap<String, HistoryDescr> result = sutWithUserAndNoDuplicateHistory
                .getSystemHistory("config-does-not-exist");
        assertTrue(result.isEmpty());
    }

    /**
     * Test of copyHistoryAndDelete method, of class FileHistoryDao.
     */
    @Test
    void testMoveHistory() {
        sutWithUserAndNoDuplicateHistory
                .copyHistoryAndDelete("Foo_deleted_20130830_223932_071", "Foo");
        assertEquals(3,
                sutWithUserAndNoDuplicateHistory.getJobHistory("Foo").size());
    }

    /**
     * Test of copyHistoryAndDelete method, of class FileHistoryDao.
     */
    @Test
    void testMoveHistoryIOException() {
        assertThrows(IllegalArgumentException.class, () ->
            sutWithUserAndNoDuplicateHistory.copyHistoryAndDelete("Test1", "Test1"));
    }

    @Test
    void testCreateNewNode() throws Exception {
        Node agent = jenkinsRule.createOnlineSlave();
        sutWithUserAndNoDuplicateHistory.createNewNode(agent);
        File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
        File revisions = new File(file, agent.getNodeName());
        assertEquals(1,
                revisions.list().length,
                "agent should have only one save history.");
        File config = new File(revisions.listFiles()[0], "config.xml");
        assertTrue(config.exists(), "File config.xml should be saved.");
        File history = new File(revisions.listFiles()[0],
                JobConfigHistoryConsts.HISTORY_FILE);
        assertTrue(history.exists(), "File history.xml should be saved.");
    }

    /**
     * Runs a test, which renames or deletes a node. This method decouples the
     * shared logic from {@link #testDeleteNode()} and
     * {@link #testRenameNode()}.
     */
    private void printTestData(Node agent, Callable<Void> func) throws Exception {
        File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
        File revisions = new File(file, agent.getNodeName());
        File revision = new File(revisions, "2014-01-20_10-12-34");
        revision.mkdirs();
        File config = new File(revision, "config.xml");
        File history = new File(revision, JobConfigHistoryConsts.HISTORY_FILE);

        HistoryDescr descr = new HistoryDescr("User", "user", "created",
                "2014-01-20_10-12-34", null, null);
        try (OutputStream configsFile = new PrintStream(config); OutputStream historyFile = new FileOutputStream(history)) {
            Jenkins.XSTREAM2.toXMLUTF8(agent, configsFile);
            Jenkins.XSTREAM2.toXMLUTF8(descr, historyFile);
        }

        // Call method an check that all files have been deleted
        func.call();
        assertFalse(config.exists(), "File config.xml should be deleted.");
        assertFalse(history.exists(), "File history.xml should be deleted.");
        assertFalse(revision.exists(),
                "Previous revision directory should be deleted");
    }

    @Test
    void testDeleteNode() throws Exception {
        Node agent = jenkinsRule.createOnlineSlave();
        printTestData(agent, () -> {
            sutWithUserAndNoDuplicateHistory.deleteNode(agent);
            return null;
        });
    }

    @Test
    void testRenameNode() throws Exception {
        Node agent = jenkinsRule.createOnlineSlave();
        printTestData(agent, () -> {
            sutWithUserAndNoDuplicateHistory.renameNode(agent,
                    agent.getNodeName(),
                    agent.getNodeName() + "_renamed");
            return null;
        });
    }

    @Test
    void testGetRevisions() throws Exception {
        Node agent = jenkinsRule.createOnlineSlave();
        createNodeRevisionManually("2014-01-18_10-12-34", getRandomConfigXml(), getHistoryXmlFromTimestamp("2014-01-18_10-12-34"), agent);
        createNodeRevisionManually("2014-01-19_10-12-34", getRandomConfigXml(), getHistoryXmlFromTimestamp("2014-01-19_10-12-34"), agent);
        createNodeRevisionManually("2014-01-20_10-12-34", getRandomConfigXml(), getHistoryXmlFromTimestamp("2014-01-20_10-12-34"), agent);
        createNodeRevisionManually("2014-01-20_10-21-34", getRandomConfigXml(), getHistoryXmlFromTimestamp("2014-01-20_10-21-34"), agent);

        SortedMap<String, HistoryDescr> revisions = sutWithUserAndNoDuplicateHistory
                .getRevisions(agent);
        assertNotNull(revisions.get("2014-01-18_10-12-34"),
                "Revision 2014-01-18_10-12-34 should be returned.");
        assertNotNull(revisions.get("2014-01-19_10-12-34"),
                "Revision 2014-01-19_10-12-34 should be returned.");
        assertNotNull(revisions.get("2014-01-20_10-12-34"),
                "Revision 2014-01-20_10-12-34 should be returned.");
        assertNotNull(revisions.get("2014-01-20_10-21-34"),
                "Revision 2014-01-20_10-21-34 should be returned.");
    }

    private File createNodeRevisionManually(String timestamp, String configXmlContent, String historyXmlContent, Node agent) throws IOException {
        File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
        File revisions = new File(file, agent.getNodeName());
        File revision = new File(revisions, timestamp);
        revision.mkdirs();
        FileUtils.writeStringToFile(
                new File(revision, "config.xml"),
                configXmlContent,
                StandardCharsets.UTF_8
        );
        FileUtils.writeStringToFile(
                new File(revision, "history.xml"),
                historyXmlContent,
                StandardCharsets.UTF_8
        );
        return revision;
    }

    private String getRandomConfigXml() {
        return "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
                "<randomField>" + Math.random() + "</randomField>";
    }

    private String getHistoryXmlFromTimestamp(String timestamp) {
        return
                "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
                        "<hudson.plugins.jobConfigHistory.HistoryDescr plugin=\"jobConfigHistory@2.20-SNAPSHOT\">" + System.lineSeparator() +
                        "  <user>unknown</user>" + System.lineSeparator() +
                        "  <userId>unknown</userId>" + System.lineSeparator() +
                        "  <operation>Deleted</operation>" + System.lineSeparator() +

                        "  <timestamp>" + timestamp + "</timestamp>" + System.lineSeparator() +

                        "  <currentName></currentName>" + System.lineSeparator() +
                        "  <oldName></oldName>" + System.lineSeparator() +
                        "</hudson.plugins.jobConfigHistory.HistoryDescr>";
    }

    private File createNodeRevision(String timestamp, Node agent)
            throws Exception {
        File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
        File revisions = new File(file, agent.getNodeName());
        File revision = new File(revisions, timestamp);
        revision.mkdirs();
        Jenkins.XSTREAM2.toXMLUTF8(agent,
                new PrintStream(new File(revision, "config.xml")));
        HistoryDescr descr = new HistoryDescr("User", "user", "created",
                timestamp, null, null);
        Jenkins.XSTREAM2.toXMLUTF8(descr, new FileOutputStream(
                new File(revision, JobConfigHistoryConsts.HISTORY_FILE)));
        return revision;
    }

    @Test
    void testSaveNode() throws Exception {
        Node agent = jenkinsRule.createOnlineSlave();
        File file = sutWithUserAndNoDuplicateHistory.getNodeHistoryRootDir();
        File revisions = new File(file, agent.getNodeName());
        sutWithUserAndNoDuplicateHistory.saveNode(agent);
        assertEquals(1,
                revisions.list().length,
                "New revision should be saved.");
    }

    @Test
    void testGetOldRevision_Node() throws Exception {
        Slave agent = jenkinsRule.createOnlineSlave();
        agent.setNumExecutors(1);
        File revision1 = createNodeRevision("2014-01-18_10-12-34", agent);
        agent.setNumExecutors(2);
        File revision2 = createNodeRevision("2014-01-19_10-12-34", agent);
        agent.setNumExecutors(3);
        File revision3 = createNodeRevision("2014-01-20_10-12-34", agent);
        XmlFile file1 = sutWithUserAndNoDuplicateHistory
                .getOldRevision(agent, "2014-01-18_10-12-34");
        XmlFile file2 = sutWithUserAndNoDuplicateHistory
                .getOldRevision(agent, "2014-01-19_10-12-34");
        XmlFile file3 = sutWithUserAndNoDuplicateHistory
                .getOldRevision(agent, "2014-01-20_10-12-34");
        assertEquals(
                new File(revision1, "config.xml").getAbsolutePath(),
                file1.getFile().getAbsolutePath(),
                "Should return config.xml file of revision 2014-01-18_10-12-34");
        assertEquals(
                new File(revision2, "config.xml").getAbsolutePath(),
                file2.getFile().getAbsolutePath(),
                "Should return config.xml file of revision 2014-01-19_10-12-34");
        assertEquals(
                new File(revision3, "config.xml").getAbsolutePath(),
                file3.getFile().getAbsolutePath(),
                "Should return config.xml file of revision 2014-01-20_10-12-34");
    }

    private FileHistoryDao getJenkinsRuleSut() {
        return (FileHistoryDao) PluginUtils.getHistoryDao();
    }

}
