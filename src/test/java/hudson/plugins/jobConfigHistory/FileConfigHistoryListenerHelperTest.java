package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Mirko Friedenhagen
 */
public class FileConfigHistoryListenerHelperTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

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
        FileConfigHistoryListenerHelper sut = new FileConfigHistoryListenerHelper("foo") {
            @Override
            User getCurrentUser() {
                return null;
            }
        };
        User result = sut.getCurrentUser();
        assertNull(result);
    }

    /**
     * Test of getIdFormatter method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testGetIdFormatter() {
        SimpleDateFormat result = FileConfigHistoryListenerHelper.getIdFormatter();
        final String formattedDate = result.format(new Date(0));
        // workaround for timezone issues, as cloudbees is in the far east :-) and returns 1969 :-).
        assertThat(formattedDate, startsWith("19"));
        assertThat(formattedDate, endsWith("00-00"));
    }

    /**
     * Test of copyConfigFile method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testCopyConfigFile() throws Exception {
        File currentConfig = new File(FileConfigHistoryListenerHelperTest.class.getResource("file1.txt").getPath());
        File timestampedDir = tempFolder.newFolder();
        FileConfigHistoryListenerHelper.copyConfigFile(currentConfig, timestampedDir);
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
        FileConfigHistoryListenerHelper sut = new FileConfigHistoryListenerHelper("foo") {
            @Override
            User getCurrentUser() {
                return null;
            }
        };
        sut.createHistoryXmlFile(timestamp, timestampedDir);
        final File historyFile = new File(timestampedDir, JobConfigHistoryConsts.HISTORY_FILE);
        assertTrue(historyFile.exists());
        final String historyContent = Util.loadFile(historyFile, Charset.forName("utf-8"));
        assertThat(historyContent, startsWith("<?xml"));
        assertThat(historyContent, endsWith("HistoryDescr>"));
        assertThat(historyContent, containsString("<user>Anonym"));
        assertThat(historyContent, containsString("foo"));
    }

    /**
     * Test of createNewHistoryDir method, of class FileConfigHistoryListenerHelper.
     */
    @Test
    public void testCreateNewHistoryDir() throws IOException {
        final File itemHistoryDir = tempFolder.newFolder();
        final AtomicReference<Calendar> timestampHolder = new AtomicReference<Calendar>();
        final File result = FileConfigHistoryListenerHelper.createNewHistoryDir(itemHistoryDir, timestampHolder);
        assertTrue(result.exists());
        assertTrue(result.isDirectory());
        // Should provoke clash
        final File result2 = FileConfigHistoryListenerHelper.createNewHistoryDir(itemHistoryDir, timestampHolder);
        assertTrue(result2.exists());
        assertTrue(result2.isDirectory());
        assertNotEquals(result, result2);

    }
}
