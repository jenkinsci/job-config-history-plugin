/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import org.htmlunit.html.HtmlForm;
import hudson.FilePath;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.security.HudsonPrivateSecurityRealm;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mirko
 */
@WithJenkins
class JobConfigHistoryJobListenerIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {

    private File jobHistoryDir;
    private JenkinsRule.WebClient webClient;

    @Override
    void setUp(JenkinsRule rule) throws Exception {
        super.setUp(rule);
        File rootDir = PluginUtils.getPlugin()
                .getConfiguredHistoryRootDir();
        jobHistoryDir = new File(rootDir,
                JobConfigHistoryConsts.JOBS_HISTORY_DIR);
        rule.jenkins.setSecurityRealm(
                new HudsonPrivateSecurityRealm(true, false, null));
        webClient = rule.createWebClient();
    }

    @Test
    void testCreation() throws IOException {
        final String jobName = "newjob";
        rule.createFreeStyleProject(jobName);
        final List<File> historyFiles = Arrays
                .asList(new File(jobHistoryDir, jobName).listFiles());
        assertFalse(historyFiles.isEmpty(), "Expected " + historyFiles
                + " to have at least one entry");
    }

    @Test
    void testRename()
            throws IOException, InterruptedException {
        final String jobName1 = "newjob";
        final String jobName2 = "renamedjob";
        final FreeStyleProject project = rule.createFreeStyleProject(jobName1);
        // Sleep two seconds to make sure we have at least two history entries.
        Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        project.renameTo(jobName2);
        final File[] historyFiles = new File(jobHistoryDir, jobName1)
                .listFiles();
        assertNull(historyFiles, "Got history files for old job");
        final List<File> historyFilesNew = Arrays
                .asList(new File(jobHistoryDir, jobName2).listFiles());
        assertFalse(historyFilesNew.isEmpty(), "Expected " + historyFilesNew
                + " to have at least two entries");
    }

    @Test
    void testRenameErrors() throws Exception {
        final HtmlForm form = webClient.goTo("configure")
                .getFormByName("config");
        form.getInputByName("historyRootDir")
                .setValue("jobConfigHistory");
        rule.submit(form);
        final FreeStyleProject project1 = rule.createFreeStyleProject("newproject");
        File historyDir = getHistoryDir(project1.getConfigFile());
        // force deletion of existing directory
        (new FilePath(historyDir)).deleteRecursive();
        project1.renameTo("newproject1");
        assertEquals(1,
                getHistoryDir(project1.getConfigFile()).list().length,
                "Verify only 1 history entry after rename.");

        // test rename failure - causes renameTo to fail if we lock the parent
        // NOTE: Windows host seem to ignore the setWritable flag, so the
        // following test will fail on Windows.
        // A somewhat crude test for a windows host.
        if ((new File("c:/")).exists()) {
            System.out.println(
                    "Skipping permission based rename tests - Windows system detected.");
        } else {
            historyDir = getHistoryDir(project1.getConfigFile());
            historyDir.getParentFile().setWritable(false);

            assertThrows(RuntimeException.class,
                    () -> project1.renameTo("newproject2"));

            assertTrue(historyDir.exists(),
                    "Verify history dir not able to be renamed.");
            historyDir.getParentFile().setWritable(true);

            // test delete rename failure
            FreeStyleProject project2 = rule.createFreeStyleProject("newproject_deleteme");
            historyDir = getHistoryDir(project2.getConfigFile());
            historyDir.getParentFile().setWritable(false);

            project2.delete();
            assertTrue(
                    historyDir.exists(),
                    "Verify history dir not able to be renamed on delete.");
            historyDir.getParentFile().setWritable(true);
        }
    }

    @Issue("JENKINS-16499")
    @Test
    void testCopyJob() throws Exception {
        final String text = "This is a description.";
        final FreeStyleProject project1 = rule.createFreeStyleProject();
        project1.setDescription(text);
        final AbstractProject<?, ?> project2 = rule.jenkins
                .copy((AbstractProject<?, ?>) project1, "project2");
        assertEquals(
                text, project2.getDescription(), "Copied project should have same description as original.");
    }

    private File getHistoryDir(XmlFile xmlFile) {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        final File configFile = xmlFile.getFile();
        return ((FileHistoryDao) jch.getHistoryDao()).getHistoryDir(configFile);
    }

}
