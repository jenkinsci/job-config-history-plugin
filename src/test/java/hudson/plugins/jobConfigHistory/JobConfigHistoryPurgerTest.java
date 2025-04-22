package hudson.plugins.jobConfigHistory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistoryPurger.
 *
 * @author Mirko Friedenhagen
 */
class JobConfigHistoryPurgerTest {

    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Purgeable mockedDao = mock(Purgeable.class);
    private final OverviewHistoryDao mockedOverviewDao = mock(OverviewHistoryDao.class);

    @TempDir
    private File tempFolder;

    @Test
    void testGetRecurrencePeriod() {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        long expResult = 24 * 60 * 60 * 1000;
        long result = sut.getRecurrencePeriod();
        assertEquals(expResult, result);
    }

    @Test
    void testDoRun() {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("1");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
                mockedPlugin, mockedDao, mockedOverviewDao);
        sut.doRun();
        assertTrue(sut.purgeCalled);
    }

    @Test
    void testDoRunNegative() {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("-1");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
                mockedPlugin, mockedDao, mockedOverviewDao);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    @Test
    void testDoRunNoNumber() {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("A");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
                mockedPlugin, mockedDao, mockedOverviewDao);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    @Test
    void testDoRunEmpty() {
        when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("");
        JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
                mockedPlugin, mockedDao, mockedOverviewDao);
        sut.doRun();
        assertFalse(sut.purgeCalled);
    }

    @Test
    void testPurgeSystemOrJobHistory() throws IOException {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        final File oldItemDir = newFolder(tempFolder, getFormattedDate(twoDaysAgo()));
        new File(oldItemDir, JobConfigHistoryConsts.HISTORY_FILE)
                .createNewFile();
        final File newItemDir = newFolder(tempFolder, getFormattedDate(now()));
        new File(newItemDir, JobConfigHistoryConsts.HISTORY_FILE)
                .createNewFile();
        File[] itemDirs = {tempFolder};
        sut.purgeSystemOrJobHistory(itemDirs);
        assertFalse(oldItemDir.exists());
        assertTrue(newItemDir.exists());
    }

    @Test
    void testPurgeSystemOrJobHistoryNoItems() {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        File[] itemDirs = {};
        sut.purgeSystemOrJobHistory(itemDirs);
        sut.purgeSystemOrJobHistory(null);
    }

    @Test
    void testPurgeSystemOrJobHistoryItemIsAFile() throws IOException {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        final File newFile = File.createTempFile(getFormattedDate(now()), null, tempFolder);
        File[] itemDirs = {newFile};
        sut.purgeSystemOrJobHistory(itemDirs);
        assertTrue(newFile.exists());
    }

    @Test
    void testPurgeSystemOrJobHistoryItemHasNoHistory()
            throws IOException {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        final File newFolder = newFolder(tempFolder, getFormattedDate(twoDaysAgo()));
        File[] itemDirs = {newFolder};
        sut.purgeSystemOrJobHistory(itemDirs);
        assertTrue(newFolder.exists());
    }

    @Test
    void testIsNotTooOld() {
        assertFalse(testIsOlderThanOneDay(now()));
    }

    @Test
    void testIsTooOld() {
        assertTrue(testIsOlderThanOneDay(twoDaysAgo()));
    }

    @Test
    void testIsTooOldInvalidFormat() {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        assertFalse(sut.isTooOld(new File("invalid format")));
    }

    @Test
    void testDeleteDirectoryWithWarnings() {
        File dirMock = mock(File.class);
        when(dirMock.listFiles())
                .thenReturn(new File[]{new File("ABC"), new File("DEF")});
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.deleteDirectory(dirMock);
    }

    @Test
    void testDeleteDirectory() throws IOException {
        final File newFile = File.createTempFile("junit", null, tempFolder);
        assertTrue(newFile.exists());
        File dir = tempFolder;
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.deleteDirectory(dir);
        assertFalse(newFile.exists());
        assertFalse(dir.exists());
    }

    private boolean testIsOlderThanOneDay(final Date date) {
        JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
                mockedDao, mockedOverviewDao);
        sut.setMaxAge(1);
        File historyDir = new File(getFormattedDate(date));
        return sut.isTooOld(historyDir);
    }

    private Date twoDaysAgo() {
        return new Date(System.currentTimeMillis()
                - TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS));
    }

    private String getFormattedDate(Date date) {
        final SimpleDateFormat dateParser = new SimpleDateFormat(
                JobConfigHistoryConsts.ID_FORMATTER);
        return dateParser.format(date);
    }

    private Date now() {
        return new Date();
    }

    private static class JobConfigHistoryPurgerWithoutPurging
            extends
            JobConfigHistoryPurger {

        boolean purgeCalled = false;

        public JobConfigHistoryPurgerWithoutPurging(JobConfigHistory plugin,
                                                    Purgeable purgeable, OverviewHistoryDao overviewHistoryDao) {
            super(plugin, purgeable, overviewHistoryDao);
        }

        @Override
        void purgeHistoryByAge() {
            purgeCalled = true;
        }

    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }

}
