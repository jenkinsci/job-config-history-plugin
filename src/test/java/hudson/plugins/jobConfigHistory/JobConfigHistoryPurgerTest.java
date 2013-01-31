package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.jvnet.hudson.test.HudsonTestCase.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;

public class JobConfigHistoryPurgerTest extends AbstractHudsonTestCaseDeletingInstanceDir {
    
    private static final int SLEEP_TIME = 1100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @LocalData
    public void testJobHistoryPurger() throws Exception {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        
        final FreeStyleProject project = (FreeStyleProject) hudson.getItem("Test1");
        final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);
        assertEquals("Verify 5 original project history entries.", 5, projectAction.getJobConfigs().size());
        
        project.setDescription("bla");
        Thread.sleep(SLEEP_TIME);
        assertEquals("Verify 5+1 project history entries.", 6, projectAction.getJobConfigs().size());
        
        jch.setMaxDaysToKeepEntries("2");
        JobConfigHistoryPurger purger = new JobConfigHistoryPurger();
        purger.run();
        
        assertEquals("Verify only 1 (new) job history entry is left after purging.", 1, projectAction.getJobConfigs().size());
    }
    
    @LocalData
    public void testSystemHistoryPurger() throws Exception {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        final File hudsonConfigDir = new File(jch.getConfiguredHistoryRootDir() + "/config");
        assertEquals("Verify 5 original system config history entries.", 5, hudsonConfigDir.listFiles(JobConfigHistory.HISTORY_FILTER).length);

        jch.setSaveSystemConfiguration(true);
        hudson.setSystemMessage("bla");
        Thread.sleep(SLEEP_TIME);
        assertEquals("Verify 5+1 project history entries.", 6, hudsonConfigDir.listFiles(JobConfigHistory.HISTORY_FILTER).length);
        
        jch.setMaxDaysToKeepEntries("2");
        JobConfigHistoryPurger purger = new JobConfigHistoryPurger();
        purger.run();
        
        assertEquals("Verify only 1 (new) job history entry is left after purging.", 1, hudsonConfigDir.listFiles(JobConfigHistory.HISTORY_FILTER).length);
    }
    
    public void testHistoryPurger() throws Exception {
        //Grenzfälle: >, = , <
        final String name = "TestJob";
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        final File historyDir = new File(jch.getJobHistoryRootDir(), name);
        
        createDirectories(historyDir);
        assertEquals("Verify 3 original project history entries.", 3,  historyDir.listFiles().length);
        jch.setMaxDaysToKeepEntries("3");
        JobConfigHistoryPurger purger = new JobConfigHistoryPurger();
        purger.run();
        assertEquals("Verify only 2 history entries are left after purging.", 2,  historyDir.listFiles().length);
        
    }
    
    public void testHistoryPurger1() throws Exception {
        final String name = "TestJob";
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        final File historyDir = new File(jch.getJobHistoryRootDir(), name);
        
        final File f = new File(historyDir, createTimestamp(4) + "/history.xml");
        if (!(f.mkdirs() || f.exists())) {
            throw new RuntimeException("Could not create directory " + f);
        }
        assertEquals("Verify 1 original project history entry.", 1,  historyDir.listFiles().length);
        jch.setMaxDaysToKeepEntries("3");
        JobConfigHistoryPurger purger = new JobConfigHistoryPurger();
        purger.run();
        assertEquals("Verify no history entry left after purging.", 0,  historyDir.listFiles().length);
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
}