/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.security.HudsonPrivateSecurityRealm;
import org.junit.Assert;
import org.jvnet.hudson.test.Issue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author mirko
 */
public class JobConfigHistoryJobListenerIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {

    private File jobHistoryDir;
    private WebClient webClient;

    @Override
    public void before() throws Throwable {
        super.before();
        File rootDir = jenkins.getPlugin(JobConfigHistory.class)
                .getConfiguredHistoryRootDir();
        jobHistoryDir = new File(rootDir,
                JobConfigHistoryConsts.JOBS_HISTORY_DIR);
        jenkins.setSecurityRealm(
                new HudsonPrivateSecurityRealm(true, false, null));
        webClient = new WebClient();
    }

    public void testCreation() throws IOException {
        final String jobName = "newjob";
        createFreeStyleProject(jobName);
        final List<File> historyFiles = Arrays
                .asList(new File(jobHistoryDir, jobName).listFiles());
        Assert.assertTrue(
                "Expected " + historyFiles
                        + " to have at least one entry",
                historyFiles.size() >= 1);
    }

    public void testRename()
            throws IOException, InterruptedException {
        final String jobName1 = "newjob";
        final String jobName2 = "renamedjob";
        final FreeStyleProject project = createFreeStyleProject(jobName1);
        // Sleep two seconds to make sure we have at least two history entries.
        Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        project.renameTo(jobName2);
        final File[] historyFiles = new File(jobHistoryDir, jobName1)
                .listFiles();
        Assert.assertNull("Got history files for old job", historyFiles);
        final List<File> historyFilesNew = Arrays
                .asList(new File(jobHistoryDir, jobName2).listFiles());
        Assert.assertTrue(
                "Expected " + historyFilesNew
                        + " to have at least two entries",
                historyFilesNew.size() >= 1);
    }

    // TODO: ???
    public void testNonAbstractProjects() {
        final JobConfigHistoryJobListener listener = new JobConfigHistoryJobListener();
        listener.onCreated(null);
        listener.onRenamed(null, "oldName", "newName");
        listener.onDeleted(null);
    }

    public void testRenameErrors() throws Exception {
        final HtmlForm form = webClient.goTo("configure")
                .getFormByName("config");
        form.getInputByName("historyRootDir")
                .setValueAttribute("jobConfigHistory");
        submit(form);
        FreeStyleProject project = createFreeStyleProject("newproject");
        File historyDir = getHistoryDir(project.getConfigFile());
        // force deletion of existing directory
        (new FilePath(historyDir)).deleteRecursive();
        project.renameTo("newproject1");
        Assert.assertEquals("Verify only 1 history entry after rename.", 1,
                getHistoryDir(project.getConfigFile()).list().length);

        // test rename failure - causes renameTo to fail if we lock the parent
        // NOTE: Windows host seem to ignore the setWritable flag, so the
        // following test will fail on Windows.
        // A somewhat crude test for a windows host.
        if ((new File("c:/")).exists()) {
            System.out.println(
                    "Skipping permission based rename tests - Windows system detected.");
        } else {
            historyDir = getHistoryDir(project.getConfigFile());
            historyDir.getParentFile().setWritable(false);
            try {
                project.renameTo("newproject2");
                Assert.fail("Expected RTE on rename");
            } catch (RuntimeException e) {
                Assert.assertTrue("Verify history dir not able to be renamed.",
                        historyDir.exists());
                historyDir.getParentFile().setWritable(true);

                // test delete rename failure
                project = createFreeStyleProject("newproject_deleteme");
                historyDir = getHistoryDir(project.getConfigFile());
                historyDir.getParentFile().setWritable(false);

                project.delete();
                Assert.assertTrue(
                        "Verify history dir not able to be renamed on delete.",
                        historyDir.exists());
                historyDir.getParentFile().setWritable(true);
            }
        }
    }

    @Issue("JENKINS-16499")
    public void testCopyJob() throws Exception {
        final String text = "This is a description.";
        final FreeStyleProject project1 = createFreeStyleProject();
        project1.setDescription(text);
        final AbstractProject<?, ?> project2 = jenkins
                .copy((AbstractProject<?, ?>) project1, "project2");
        Assert.assertEquals(
                "Copied project should have same description as original.",
                project2.getDescription(), text);
    }

    private File getHistoryDir(XmlFile xmlFile) {
        final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
        final File configFile = xmlFile.getFile();
        return ((FileHistoryDao) jch.getHistoryDao()).getHistoryDir(configFile);
    }

}
