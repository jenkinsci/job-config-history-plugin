package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class JobConfigHistoryPurgerIT {
    private static final int SLEEP_TIME = 1100;
    private JobConfigHistory jch;
    private JobConfigHistoryPurger purger;
    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
        jch = PluginUtils.getPlugin();
        purger = new JobConfigHistoryPurger();
    }

    /**
     * Tests if the system config entries provided by the test data are deleted
     * correctly except for the newest one.
     */
    @LocalData
    @Test
    void testSystemHistoryPurger() throws Exception {
        final String message = "Some nice message";
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(rule.jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        assertTrue(
                historyEntries > 4,
                "Verify at least 5 original system config history entries, got "
                        + historyEntries);

        rule.jenkins.setSystemMessage(message);
        Thread.sleep(SLEEP_TIME);
        assertEquals(historyEntries + 1, historyDao.getRevisions(configXml).size(), "Verify one additional system history entry.");

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        final SortedMap<String, HistoryDescr> revisions = historyDao
                .getRevisions(configXml);
        assertTrue(
                historyEntries - 5 <= revisions.size(),
                "Verify 5 (old) system history entries less after purging.");
        final ArrayList<String> revisionsKeys = new ArrayList<>(
                revisions.keySet());
        System.out.println(revisionsKeys);

        final XmlFile lastEntry = historyDao.getOldRevision(configXml,
                revisionsKeys.get(revisionsKeys.size() - 1));
        final String lastEntryAsString = lastEntry.asString();
        assertTrue(
                lastEntryAsString.contains(message),
                "Verify remaining entry is the newest one" + lastEntryAsString);
    }

    /**
     * Checks that nothing gets deleted when maxDays is set to 0.
     */
    @LocalData
    @Test
    void testHistoryPurgerWhenMaxDaysSetToZero() {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(rule.jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        assertTrue(
                historyEntries > 4,
                "Verify at least 5 original system config history entries.");

        jch.setMaxDaysToKeepEntries("0");
        purger.run();

        assertEquals(historyEntries, historyDao.getRevisions(configXml).size(), "Verify that original entries are still there.");
    }

    /**
     * Checks that nothing gets deleted when a negative max age is entered.
     */
    @LocalData
    @Test
    void testWithNegativeMaxAge() {
        testWithWrongMaxAge("-1");
    }

    /**
     * Checks that nothings gets deleted when max age is empty.
     */
    @LocalData
    @Test
    void testWithEmptyMaxAge() {
        testWithWrongMaxAge("");
    }

    private void testWithWrongMaxAge(String maxAge) {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(rule.jenkins.root, "config.xml"));
        final int historyEntries = historyDao.getRevisions(configXml).size();
        assertTrue(
                historyEntries > 4,
                "Verify at least 5 original system config history entries.");

        jch.setMaxDaysToKeepEntries(maxAge);
        purger.run();
        assertEquals(historyEntries, historyDao.getRevisions(configXml).size(), "Verify that original entries are still there.");
    }

    /**
     * Tests deletion of JOB config history files.
     */
    @Test
    void testJobHistoryPurger() {
        final String name = "TestJob";
        final File historyDir = new File(
                new File(jch.getConfiguredHistoryRootDir(), "jobs"), name);
        createDirectories(historyDir);
        assertEquals(3,
                historyDir.listFiles().length,
                "Verify 3 original project history entries.");

        jch.setMaxDaysToKeepEntries("3");
        purger.run();
        assertEquals(1, historyDir.listFiles().length, "Verify only 1 history entry left after purging.");

        jch.setMaxDaysToKeepEntries("1");
        purger.run();
        assertEquals(0,
                historyDir.listFiles().length,
                "Verify no history entry left after purging.");
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
    @Test
    void testJobHistoryPurgerWithCreatedEntries() {
        final HistoryDao historyDao = purger.getHistoryDao();
        final XmlFile configXml = new XmlFile(
                new File(rule.jenkins.root, "jobs/Test1/config.xml"));
        assertEquals(5,
                historyDao.getRevisions(configXml).size(),
                "Verify 5 original project history entries.");

        jch.setMaxDaysToKeepEntries("1");
        purger.run();

        final SortedMap<String, HistoryDescr> listFilesAfterPurge = historyDao
                .getRevisions(configXml);
        assertEquals(
                1, listFilesAfterPurge.size(), "Verify 1 project history entries left." + listFilesAfterPurge);
    }
}
