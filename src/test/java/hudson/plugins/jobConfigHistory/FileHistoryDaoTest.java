package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.User;
import hudson.util.IOUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class FileHistoryDaoTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.INSTANCE;

    private final User mockedUser = mock(User.class);

    private final AbstractItem mockedItem = mock(AbstractItem.class);

    private File jenkinsHome;

    private XmlFile test1Config;

    private File historyRoot;

    private File test1History;

    private FileHistoryDao sutWithoutUser;

    private FileHistoryDao sutWithUser;

    private static final String FULL_NAME = "Full Name";

    private static final String USER_ID = "userId";

    private File test1JobDirectory;

    public FileHistoryDaoTest() {
    }

    @Before
    public void setFieldsFromUnpackResource() {
        jenkinsHome = unpackResourceZip.getRoot();
        test1Config = new XmlFile(unpackResourceZip.getResource("jobs/Test1/config.xml"));
        test1JobDirectory = test1Config.getFile().getParentFile();
        historyRoot = unpackResourceZip.getResource("config-history");
        test1History = new File(historyRoot, "jobs/Test1");
        sutWithoutUser = new FileHistoryDao(historyRoot, jenkinsHome, null, 0);
        sutWithUser = new FileHistoryDao(historyRoot, jenkinsHome, mockedUser, 0);
        when(mockedUser.getFullName()).thenReturn(FULL_NAME);
        when(mockedUser.getId()).thenReturn(USER_ID);
    }


    /**
     * Test of createNewHistoryEntry method, of class FileHistoryDao.
     */
    @Test
    public void testCreateNewHistoryEntry() throws IOException {
        sutWithoutUser.createNewHistoryEntry(test1Config, "foo");
        final int newLength = getHistoryRootForTest1Length();
        assertEquals(6, newLength);
    }


    /**
     * Test of createNewHistoryEntry method, of class FileHistoryDao.
     */
    @Test
    public void testCreateNewHistoryEntryRTE() throws IOException {
        final XmlFile xmlFile = test1Config;
        FileHistoryDao sut = new FileHistoryDao(historyRoot, jenkinsHome, null, 0) {
            @Override
            File getRootDir(XmlFile xmlFile, AtomicReference<Calendar> timestampHolder) {
                throw new RuntimeException("oops");
            }
        };
        try {
            sut.createNewHistoryEntry(xmlFile, "foo");
            fail("Should throw RTE");
        } catch (RuntimeException e) {
            final int newLength = getHistoryRootForTest1Length();
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
        // workaround for timezone issues, as cloudbees is in the far east :-) and returns 1969 :-).
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
        testCreateHistoryXmlFile(sutWithUser, FULL_NAME);
    }

    /**
     * Test of createHistoryXmlFile method, of class FileHistoryDao.
     */
    @Test
    public void testCreateHistoryXmlFileAnonym() throws Exception {
        testCreateHistoryXmlFile(sutWithoutUser, "Anonym");
    }

    private void testCreateHistoryXmlFile(FileHistoryDao sut, final String fullName) throws IOException {
        Calendar timestamp = new GregorianCalendar();
        File timestampedDir = new File(historyRoot, "timestampedDir");
        sut.createHistoryXmlFile(timestamp, timestampedDir, "foo");
        final File historyFile = new File(timestampedDir, JobConfigHistoryConsts.HISTORY_FILE);
        assertTrue(historyFile.exists());
        final String historyContent = Util.loadFile(historyFile, Charset.forName("utf-8"));
        assertThat(historyContent, startsWith("<?xml"));
        assertThat(historyContent, endsWith("HistoryDescr>"));
        assertThat(historyContent, containsString("<user>"+fullName));
        assertThat(historyContent, containsString("foo"));
    }

    /**
     * Test of createNewHistoryDir method, of class FileHistoryDao.
     */
    @Test
    public void testCreateNewHistoryDir() throws IOException {
        final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
        final File first = FileHistoryDao.createNewHistoryDir(historyRoot, timestampHolder);
        assertTrue(first.exists());
        assertTrue(first.isDirectory());
        // Should provoke clash
        final File second = FileHistoryDao.createNewHistoryDir(historyRoot, timestampHolder);
        assertTrue(second.exists());
        assertTrue(second.isDirectory());
        assertNotEquals(first.getAbsolutePath(), second.getAbsolutePath());
    }

    /**
     * Test of getRootDir method, of class FileHistoryDao.
     */
    @Test
    public void testGetRootDir() {
        AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
        File result = sutWithoutUser.getRootDir(test1Config, timestampHolder);
        assertTrue(result.exists());
        assertThat(result.getPath(), containsString("config-history"  + File.separator + "jobs" + File.separator + "Test1"));
    }

    /**
     * Test of createNewItem method, of class FileHistoryDao.
     */
    @Test
    public void testCreateNewItem() throws IOException {
        final File jobDir = new File(jenkinsHome, "jobs/MyTest");
        jobDir.mkdirs();
        IOUtils.copy(test1Config.getFile(), new FileOutputStream(new File(jobDir, "config.xml")));
        when(mockedItem.getRootDir()).thenReturn(jobDir);
        sutWithUser.createNewItem(mockedItem);
        assertTrue(new File(jobDir, "config.xml").exists());
        final File historyDir = new File(jenkinsHome, "config-history/jobs/MyTest");
        assertTrue(historyDir.exists());
        assertEquals(1, historyDir.list().length);
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    public void testSaveItem_AbstractItem() throws IOException {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        sutWithUser.saveItem(mockedItem);
        assertEquals(6, getHistoryLength());
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    public void testSaveItem_XmlFile() {
        sutWithUser.saveItem(test1Config);
        assertEquals(6, getHistoryLength());
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
        sutWithUser.deleteItem(mockedItem);
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
        sutWithUser.renameItem(mockedItem, "Test1", "NewName");
        final File newHistoryDir = new File(historyRoot, "jobs/" + newName);
        assertTrue(newHistoryDir.exists());
        assertEquals(6, newHistoryDir.list().length);
    }

    /**
     * Test of getRevisions method, of class FileHistoryDao.
     */
    @Test
    public void testGetRevisions() throws IOException {
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        SortedMap<String, HistoryDescr> result = sutWithUser.getRevisions(mockedItem);
        assertEquals(5, result.size());
        assertEquals("2012-11-21_11-29-12", result.firstKey());
        assertEquals("2012-11-21_11-42-05", result.lastKey());
        final HistoryDescr firstValue = result.get(result.firstKey());
        assertEquals("Changed", firstValue.getOperation());
        assertEquals("anonymous", firstValue.getUserID());
    }

    /**
     * Test of getOldRevision method, of class FileHistoryDao.
     */
    @Test
    public void testGetOldRevision() throws IOException {
        System.out.println("getOldRevision");
        when(mockedItem.getRootDir()).thenReturn(test1JobDirectory);
        String identifier = "2012-11-21_11-42-05";
        final XmlFile result = sutWithUser.getOldRevision(mockedItem, identifier);
        final String xml = result.asString();
        assertThat(xml, startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
        assertThat(xml, endsWith("</project>"));
    }

    /**
     * Test of getHistoryDir method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testGetHistoryDir() {
        System.out.println("getHistoryDir");
        XmlFile xmlFile = null;
        FileHistoryDao sut = null;
        File expResult = null;
        File result = sut.getHistoryDir(xmlFile);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getJobHistoryRootDir method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testGetJobHistoryRootDir() {
        System.out.println("getJobHistoryRootDir");
        FileHistoryDao sut = null;
        File expResult = null;
        File result = sut.getJobHistoryRootDir();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
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
    public void testPurgeOldEntriesOnlyOneExisting() {
        final int maxEntries = 2;
        final int expectedLength = 1;
        testPurgeOldEntries(maxEntries, expectedLength);
    }

    private void testPurgeOldEntries(int maxEntries, final int expectedLength) {
        FileHistoryDao.purgeOldEntries(test1History, maxEntries);
        final int newLength = getHistoryRootForTest1Length();
        assertEquals(expectedLength, newLength);
    }

    private int getHistoryRootForTest1Length() {
        return getHistoryLength();
    }



}
