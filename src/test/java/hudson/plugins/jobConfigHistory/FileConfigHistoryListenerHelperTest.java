package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.User;
import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Mirko Friedenhagen
 */
public class FileConfigHistoryListenerHelperTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

    /**
     * Test of values method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testValues() {
        FileConfigHistoryListenerHelper[] result = FileConfigHistoryListenerHelper.values();
        assertEquals(4, result.length);
    }

    /**
     * Test of valueOf method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testValueOf() {
        FileConfigHistoryListenerHelper expResult = FileConfigHistoryListenerHelper.CHANGED;
        FileConfigHistoryListenerHelper result = FileConfigHistoryListenerHelper.valueOf("CHANGED");
        assertEquals(expResult, result);
    }

    /**
     * Test of createNewHistoryEntry method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    @Ignore
    public void testCreateNewHistoryEntry() {
        System.out.println("createNewHistoryEntry");
        XmlFile xmlFile = null;
        FileConfigHistoryListenerHelper sut = null;
        sut.createNewHistoryEntry(xmlFile);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurrentUser method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testGetCurrentUser() {
        FileConfigHistoryListenerHelper sut = FileConfigHistoryListenerHelper.CHANGED;
        User result = sut.getCurrentUser();
        assertNull(result);
    }

    /**
     * Test of getIdFormatter method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testGetIdFormatter() {
        FileConfigHistoryListenerHelper sut = FileConfigHistoryListenerHelper.CHANGED;
        String expResult = "1970-01-01_01-00-00";
        SimpleDateFormat result = sut.getIdFormatter();
        assertEquals(expResult, result.format(new Date(0)));
    }

    /**
     * Test of copyConfigFile method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testCopyConfigFile() throws Exception {
        File currentConfig = new File(FileConfigHistoryListenerHelperTest.class.getResource("file1.txt").getPath());
        File timestampedDir = tempFolder.newFolder();
        FileConfigHistoryListenerHelper sut = FileConfigHistoryListenerHelper.CHANGED;
        sut.copyConfigFile(currentConfig, timestampedDir);
        final File copy = new File(timestampedDir, currentConfig.getName());
        assertTrue(copy.exists());
    }

    /**
     * Test of createHistoryXmlFile method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testCreateHistoryXmlFile() throws Exception {
        Calendar timestamp = new GregorianCalendar();
        File timestampedDir = tempFolder.newFolder();
        FileConfigHistoryListenerHelper sut = FileConfigHistoryListenerHelper.CHANGED;
        sut.createHistoryXmlFile(timestamp, timestampedDir);
        final File historyFile = new File(timestampedDir, JobConfigHistoryConsts.HISTORY_FILE);
        assertTrue(historyFile.exists());
        final String historyContent = Util.loadFile(historyFile, Charset.forName("utf-8"));
        assertThat(historyContent, startsWith("<?xml"));
        assertThat(historyContent, endsWith("HistoryDescr>"));
        assertThat(historyContent, containsString("<user>Anonym"));
    }
}
