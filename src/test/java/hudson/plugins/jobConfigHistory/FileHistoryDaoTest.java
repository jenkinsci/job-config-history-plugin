package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
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

    private File jenkinsHome;

    private XmlFile test1Config;

    private File historyRoot;

    private FileHistoryDao sutWithoutUser;

    private FileHistoryDao sutWithUser;

    private static final String FULL_NAME = "Full Name";

    private static final String USER_ID = "userId";

    public FileHistoryDaoTest() {


    }

    @Before
    public void setFieldsFromUnpackResource() {
        jenkinsHome = unpackResourceZip.getRoot();
        test1Config = new XmlFile(unpackResourceZip.getResource("jobs/Test1/config.xml"));
        historyRoot = unpackResourceZip.getResource("config-history/jobs/Test1/");
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
        sut.createNewHistoryEntry(xmlFile, "foo");
        final int newLength = getHistoryRootForTest1Length();
        assertEquals(5, newLength);
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
        assertThat(result.getPath(), containsString("jobs" + File.separator + "Test1"));
    }

    /**
     * Test of getPlugin method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testGetPlugin() {
        System.out.println("getPlugin");
        FileHistoryDao sut = null;
        JobConfigHistory expResult = null;
        JobConfigHistory result = sut.getPlugin();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of createNewItem method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testCreateNewItem() {
        System.out.println("createNewItem");
        AbstractItem item = null;
        FileHistoryDao sut = null;
        sut.createNewItem(item);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testSaveItem_AbstractItem() {
        System.out.println("saveItem");
        AbstractItem item = null;
        FileHistoryDao sut = null;
        sut.saveItem(item);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of saveItem method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testSaveItem_XmlFile() {
        System.out.println("saveItem");
        XmlFile file = null;
        FileHistoryDao sut = null;
        sut.saveItem(file);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deleteItem method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testDeleteItem() {
        System.out.println("deleteItem");
        AbstractItem item = null;
        FileHistoryDao sut = null;
        sut.deleteItem(item);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of renameItem method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testRenameItem() {
        System.out.println("renameItem");
        AbstractItem item = null;
        String newName = "";
        FileHistoryDao sut = null;
        sut.renameItem(item, newName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRevisions method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testGetRevisions() {
        System.out.println("getRevisions");
        AbstractItem item = null;
        FileHistoryDao sut = null;
        SortedMap<String, XmlFile> expResult = null;
        SortedMap<String, XmlFile> result = sut.getRevisions(item);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getOldRevision method, of class FileHistoryDao.
     */
    @Test
    @Ignore
    public void testGetOldRevision() {
        System.out.println("getOldRevision");
        AbstractItem item = null;
        String identifier = "";
        FileHistoryDao sut = null;
        XmlFile expResult = null;
        XmlFile result = sut.getOldRevision(item, identifier);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
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
        final int oldLength = getHistoryRootForTest1Length();
        int maxEntries = 0;
        FileHistoryDao.purgeOldEntries(historyRoot, maxEntries);
        final int newLength = getHistoryRootForTest1Length();
        assertEquals(oldLength, newLength);
    }

    /**
     * Test of purgeOldEntries method, of class FileHistoryDao.
     */
    @Test
    public void testPurgeOldEntriesOnlyOneExisting() {
        int maxEntries = 2;
        FileHistoryDao.purgeOldEntries(historyRoot, maxEntries);
        final int newLength = getHistoryRootForTest1Length();
        assertEquals(1, newLength);
    }

    private int getHistoryRootForTest1Length() {
        return historyRoot.list().length;
    }



}
