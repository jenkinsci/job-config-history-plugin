package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import org.junit.Assert;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedMap;

public class JobConfigHistoryPurgerIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {
    private static final int SLEEP_TIME = 1100;
    private JobConfigHistory jch;
    private JobConfigHistoryPurger purger;

    @Override
    public void before() throws Throwable {
        super.before();
        jch = PluginUtils.getPlugin();
        purger = new JobConfigHistoryPurger();
    }

    /**
     * Tests if the system config entries provided by the test data are deleted
     * correctly except for the newest one.
     */
    @LocalData
    public void testSystemHistoryPurger() throws Exception {
        final String message = "Some nice message";
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        Assert.assertTrue(
                "Verify at least 5 original system config history entries, got "
                        + historyEntries,
                historyEntries > 4);

        jenkins.setSystemMessage(message);
        Thread.sleep(SLEEP_TIME);
        Assert.assertEquals("Verify one additional system history entry.",
                historyEntries + 1, historyDao.getRevisions(configXml).size());

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        final SortedMap<String, HistoryDescr> revisions = historyDao
                .getRevisions(configXml);
        Assert.assertTrue(
                "Verify 5 (old) system history entries less after purging.",
                historyEntries - 5 <= revisions.size());
        final ArrayList<String> revisionsKeys = new ArrayList<>(
                revisions.keySet());
        System.out.println(revisionsKeys);

        final XmlFile lastEntry = historyDao.getOldRevision(configXml,
                revisionsKeys.get(revisionsKeys.size() - 1));
        final String lastEntryAsString = lastEntry.asString();
        Assert.assertTrue(
                "Verify remaining entry is the newest one" + lastEntryAsString,
                lastEntryAsString.contains(message));
    }

    /**
     * Checks that nothing gets deleted when maxDays is set to 0.
     */
    @LocalData
    public void testHistoryPurgerWhenMaxDaysSetToZero() {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        Assert.assertTrue(
                "Verify at least 5 original system config history entries.",
                historyEntries > 4);

        jch.setMaxDaysToKeepEntries("0");
        purger.run();

        Assert.assertEquals("Verify that original entries are still there.",
                historyEntries, historyDao.getRevisions(configXml).size());
    }

    /**
     * Checks that nothing gets deleted when a negative max age is entered.
     */
    @LocalData
    public void testWithNegativeMaxAge() throws Exception {
        testWithWrongMaxAge("-1");
    }

    /**
     * Checks that nothings gets deleted when max age is empty.
     */
    @LocalData
    public void testWithEmptyMaxAge() throws Exception {
        testWithWrongMaxAge("");
    }

    private void testWithWrongMaxAge(String maxAge) {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        Assert.assertTrue(
                "Verify at least 5 original system config history entries.",
                historyEntries > 4);

        jch.setMaxDaysToKeepEntries(maxAge);
        purger.run();
        Assert.assertEquals("Verify that original entries are still there.",
                historyEntries, historyDao.getRevisions(configXml).size());
    }

    /**
     * Tests deletion of JOB config history files.
     */
    public void testJobHistoryPurger() {
        final String name = "TestJob";
        final File historyDir = new File(
                new File(jch.getConfiguredHistoryRootDir(), "jobs"), name);
        createDirectories(historyDir);
        Assert.assertEquals("Verify 3 original project history entries.", 3,
                historyDir.listFiles().length);

        jch.setMaxDaysToKeepEntries("3");
        purger.run();
        Assert.assertEquals("Verify only 1 history entry left after purging.",
                1, historyDir.listFiles().length);

        jch.setMaxDaysToKeepEntries("1");
        purger.run();
        Assert.assertEquals("Verify no history entry left after purging.", 0,
                historyDir.listFiles().length);
    }

    private void createDirectories(File historyDir) {
        final int[] daysAgo = {2, 3, 4};
        for (int offset : daysAgo) {
            final File f = new File(historyDir,
                    createTimestamp(offset) + "/history.xml");
            // mkdirs sometimes fails although the directory exists afterwards,
            // so check for existence as well and just be happy if it does.
            if (!(f.mkdirs() || f.exists())) {
                throw new RuntimeException("Could not create directory " + f);
            }
        }
    }

    private String createTimestamp(int daysOffset) {
        final Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, -daysOffset);
        final SimpleDateFormat formatter = new SimpleDateFormat(
                JobConfigHistoryConsts.ID_FORMATTER);
        return formatter.format(calendar.getTime());
    }

    /**
     * Checks that job history entries with the operation "Created" are not
     * deleted even if they are too old.
     */
    @LocalData
    public void testJobHistoryPurgerWithCreatedEntries() {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(jenkins.root, "jobs/Test1/config.xml"));
        Assert.assertEquals("Verify 5 original project history entries.", 5,
                historyDao.getRevisions(configXml).size());

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        final SortedMap<String, HistoryDescr> listFilesAfterPurge = historyDao
                .getRevisions(configXml);
        Assert.assertEquals(
                "Verify 1 project history entries left." + listFilesAfterPurge,
                1, listFilesAfterPurge.size());
    }
}
