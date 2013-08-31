package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryPurgerTest {

    final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

    /**
     * Test of getRecurrencePeriod method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testGetRecurrencePeriod() {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        long expResult = 24*60*60*1000;
        long result = sut.getRecurrencePeriod();
        assertEquals(expResult, result);
    }

    /**
     * Test of doRun method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDoRun() throws Exception {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("1");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(mockedPlugin);
        sut.doRun();
        assertTrue(sut.purgeCalled);
    }

    /**
     * Test of doRun method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDoRunNegative() throws Exception {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("-1");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(mockedPlugin);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    /**
     * Test of doRun method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDoRunNoNumber() throws Exception {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("A");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(mockedPlugin);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    /**
     * Test of doRun method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDoRunEmpty() throws Exception {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(mockedPlugin);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    /**
     * Test of purgeHistoryByAge method, of class JobConfigHistoryPurger.
     */
    @Test
    @Ignore
    public void testPurgeHistoryByAge() {
        System.out.println("purgeHistoryByAge");
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.purgeHistoryByAge();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of purgeSystemOrJobHistory method, of class JobConfigHistoryPurger.
     */
    @Test
    @Ignore
    public void testPurgeSystemOrJobHistory() {
        System.out.println("purgeSystemOrJobHistory");
        File[] itemDirs = null;
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.purgeSystemOrJobHistory(itemDirs);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isTooOld method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testIsNotTooOld() {
        final Date date = new Date();
        assertFalse(testIsOlderThanOneDay(date));
    }

    /**
     * Test of isTooOld method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testIsTooOld() {
        final Date date = new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS));
        assertTrue(testIsOlderThanOneDay(date));
    }

    /**
     * Test of isTooOld method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testIsTooOldInvalidFormat() {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.maxAge = 1;
        assertFalse(sut.isTooOld(new File("invalid format")));
    }

    /**
     * Test of deleteDirectory method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDeleteDirectoryWithWarnings() {
        File dirMock = mock(File.class);
        when(dirMock.listFiles()).thenReturn(new File[]{new File("ABC"), new File("DEF")});
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.deleteDirectory(dirMock);
    }

    /**
     * Test of deleteDirectory method, of class JobConfigHistoryPurger.
     */
    @Test
    public void testDeleteDirectory() throws IOException {
        final File newFile = tempFolder.newFile();
        assertTrue(newFile.exists());
        File dir = tempFolder.getRoot();
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.deleteDirectory(dir);
        assertFalse(newFile.exists());
        assertFalse(dir.exists());
    }

    private boolean testIsOlderThanOneDay(final Date date) {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin);
        sut.maxAge = 1;
        final SimpleDateFormat dateParser = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
        final String format = dateParser.format(date);
        File historyDir = new File(format);
        return sut.isTooOld(historyDir);
    }

    private static class JobConfigHistoryPurgerWithoutPurging extends JobConfigHistoryPurger {

        boolean purgeCalled = false;

        public JobConfigHistoryPurgerWithoutPurging(JobConfigHistory plugin) {
            super(plugin);
        }

        @Override
        void purgeHistoryByAge() {
            purgeCalled = true;
        }
    }

}
