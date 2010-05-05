/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.security.HudsonPrivateSecurityRealm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlForm;

/**
 * @author mirko
 *
 */
public class JobConfigHistoryJobListenerTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private File jobsDir;
    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jobsDir = new File(hudson.root, "jobs");
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(true));
        webClient = new WebClient();
    }

    public void testCreation() throws IOException, SAXException {
        createFreeStyleProject("newjob");
        final List<File> historyFiles = Arrays.asList(new File(jobsDir, "newjob/config-history").listFiles());
        assertTrue("Expected " + historyFiles.toString() + " to have at least one entry", historyFiles.size()>=1);
    }

    public void testRename() throws IOException, SAXException, InterruptedException {
        final FreeStyleProject project = createFreeStyleProject("newjob");
        // Sleep two seconds to make sure we have at least two history entries.
        Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        project.renameTo("renamedjob");
        final File[] historyFiles = new File(jobsDir, "newjob/config-history").listFiles();
        assertNull("Got history files for old job", historyFiles);
        final List<File> historyFilesNew = Arrays.asList(new File(jobsDir, "renamedjob/config-history").listFiles());
        assertTrue("Expected " + historyFilesNew.toString() + " to have at least two entries", historyFilesNew.size()>=1);
    }

    public void testNonAbstractProjects() {
        final JobConfigHistoryJobListener listener = new JobConfigHistoryJobListener();
        listener.onCreated(null);
        listener.onRenamed(null, "oldName", "newName");
        listener.onDeleted(null);
    }

    public void testRenameErrors() {
        JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        try {
            HtmlForm form = webClient.goTo("configure").getFormByName("config");
            form.getInputByName("historyRootDir").setValueAttribute("jobConfigHistory");
            form.getInputByName("saveSystemConfiguration").setChecked(true);
            submit(form);
        } catch (Exception e) {
            fail("unable to configure historyRootDir" + e);
        }
        try {
            FreeStyleProject project = createFreeStyleProject("newproject");
            File historyDir = jch.getHistoryDir(project.getConfigFile());
            // force deletion of existing directory
            (new FilePath(historyDir)).deleteRecursive();
            project.renameTo("newproject1");
            assertEquals("Verify only 1 history entry after rename.", 1, jch.getHistoryDir(project.getConfigFile()).list().length);

            // test rename failure - causes renameTo to fail if we lock the parent
            // NOTE: Windows host seem to ignore the setWritable flag, so the following test will fail on Windows.
            // A somewhat crude test for a windows host.
            if ((new File("c:/")).exists()) {
                System.out.println("Skipping permission based rename tests - Windows system detected.");
            } else {
                historyDir = jch.getHistoryDir(project.getConfigFile());
                historyDir.getParentFile().setWritable(false);

                // catch the RuntimeException thrown by ConfigHistoryListenerHelper
                try {
                    project.renameTo("newproject2");
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                assertTrue("Verify history dir not able to be renamed.", historyDir.exists());
                historyDir.getParentFile().setWritable(true);

                // test delete rename failure
                project = createFreeStyleProject("newproject_deleteme");
                historyDir = jch.getHistoryDir(project.getConfigFile());
                historyDir.getParentFile().setWritable(false);

                // again catch RuntimeException from ConfigHistoryListenerHelper
                try {
                    project.delete();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                assertTrue("Verify history dir not able to be renamed on delete.", historyDir.exists());
                historyDir.getParentFile().setWritable(true);
            }

        } catch (Exception e) {
            fail("Cannot complete rename errors test: " + e);
        }
    }
}
