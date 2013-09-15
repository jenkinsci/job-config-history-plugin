package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jvnet.hudson.test.recipes.LocalData;

import hudson.XmlFile;
import java.util.Arrays;
import java.util.List;

public class JobConfigHistoryPurgerIT extends AbstractHudsonTestCaseDeletingInstanceDir {
    private static final int SLEEP_TIME = 1100;
    private JobConfigHistory jch;
    private JobConfigHistoryPurger purger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jch = hudson.getPlugin(JobConfigHistory.class);
        purger = new JobConfigHistoryPurger();
    }

    /**
     * Tests if the system config entries provided by the test data are deleted correctly
     * except for the newest one.
     * @throws Exception
     */
    @LocalData
    public void testSystemHistoryPurger() throws Exception {
        final String message = "Some nice message";
        final File hudsonConfigDir = new File(jch.getConfiguredHistoryRootDir() + "/config");
        assertEquals("Verify 5 original system config history entries.", 5, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

        jch.setSaveSystemConfiguration(true);
        hudson.setSystemMessage(message);
        Thread.sleep(SLEEP_TIME);
        assertEquals("Verify 5+1 project history entries.", 6, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        assertEquals("Verify only 1 (new) job history entry is left after purging.", 1, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);
        final XmlFile lastEntry = new XmlFile(new File (hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE)[0], "config.xml"));
        assertTrue("Verify remaining entry is the newest one", lastEntry.asString().contains(message));
    }

    /**
     * Checks that nothing gets deleted when maxDays is set to 0.
     * @throws Exception
     */
    @LocalData
    public void testHistoryPurgerWhenMaxDaysSetToZero() throws Exception {
        final File hudsonConfigDir = new File(jch.getConfiguredHistoryRootDir() + "/config");
        assertEquals("Verify 5 original system config history entries.", 5, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

        jch.setMaxDaysToKeepEntries("0");
        purger.run();

        assertEquals("Verify that 5 original entries are still there.", 5, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);
    }

    /**
     * Checks that nothing gets deleted when a negative max age is entered.
     * @throws Exception
     */
    @LocalData
    public void testWithNegativeMaxAge() throws Exception {
        testWithWrongMaxAge("-1");
    }

    /**
     * Checks that nothings gets deleted when max age is empty.
     * @throws Exception
     */
    @LocalData
    public void testWithEmptyMaxAge() throws Exception {
        testWithWrongMaxAge("");
    }

    private void testWithWrongMaxAge(String maxAge) throws Exception {
        final File hudsonConfigDir = new File(jch.getConfiguredHistoryRootDir() + "/config");
        assertEquals("Verify 5 original system config history entries.", 5, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

        jch.setMaxDaysToKeepEntries(maxAge);
        purger.run();
        assertEquals("Verify that 5 original entries are still there.", 5, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);
    }

    /**
     * Tests deletion of JOB config history files.
     * @throws Exception
     */
    public void testJobHistoryPurger() throws Exception {
        final String name = "TestJob";
        final File historyDir = new File(jch.getJobHistoryRootDir(), name);
        createDirectories(historyDir);
        assertEquals("Verify 3 original project history entries.", 3,  historyDir.listFiles().length);

        jch.setMaxDaysToKeepEntries("3");
        purger.run();
        assertEquals("Verify only 1 history entry left after purging.", 1,  historyDir.listFiles().length);

        jch.setMaxDaysToKeepEntries("1");
        purger.run();
        assertEquals("Verify no history entry left after purging.", 0, historyDir.listFiles().length);
    }

    private void createDirectories(File historyDir) {
        final int[] daysAgo = {2, 3, 4};
        for (int offset : daysAgo) {
            final File f = new File(historyDir, createTimestamp(offset) + "/history.xml");
            // mkdirs sometimes fails although the directory exists afterwards,
            // so check for existence as well and just be happy if it does.
            if (!(f.mkdirs() || f.exists())) {
                throw new RuntimeException("Could not create directory " + f);
            }
        }
    }

    private String createTimestamp(int daysOffset) {
        final Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DAY_OF_YEAR,-daysOffset);
        final SimpleDateFormat formatter = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
        return formatter.format(calendar.getTime());
    }

    /**
     * Checks that job history entries with the operation "Created" are not deleted
     * even if they are too old.
     * @throws Exception
     */
    @LocalData
    public void testJobHistoryPurgerWithCreatedEntries() throws Exception {
        final String name = "Test1";
        final File historyDir = new File(jch.getJobHistoryRootDir(), name);
        assertEquals("Verify 5 original project history entries.", 5, historyDir.listFiles().length);

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        final List<File> listFilesAfterPurge = Arrays.asList(historyDir.listFiles());
        assertEquals("Verify 1 project history entries left." + listFilesAfterPurge, 1, listFilesAfterPurge.size());
    }
}