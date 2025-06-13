/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import org.htmlunit.ElementNotFoundException;
import org.htmlunit.TextPage;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.security.AccessControlled;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.Permission;
import hudson.tasks.LogRotator;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration Tests for JobConfigHistoryBaseAction.
 *
 * @author mfriedenhagen
 */
@WithJenkins
class JobConfigHistoryBaseActionIT {

    // we need to sleep between saves so we don't overwrite the history
    // directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;
    private final File file1 = new File("old/config.xml");
    private final File file2 = new File("new/config.xml");
    private JenkinsRule rule;
    private JenkinsRule.WebClient webClient;
    private String oldLineSeparator;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
        webClient = rule.createWebClient();
        oldLineSeparator = System.lineSeparator();
        System.setProperty("line.separator", "\n");
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setProperty("line.separator", oldLineSeparator);
    }

    /**
     * Test method for
     * {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffAsString(File, File, String[], String[])}.
     */
    @Test
    void testGetDiffFileStringStringSameLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals(
                "--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,2 @@\n 123\n-346\n+3467\n",
                makeResultPlatformIndependent(action.getDiffAsString(file1,
                        file2, s1.split("\n"), s2.split("\n"))));
    }

    /**
     * Test method for
     * {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffAsString(File, File, String[], String[])}.
     */
    @Test
    void testGetDiffFileStringStringEmpty() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        assertEquals("\n", makeResultPlatformIndependent(action
                .getDiffAsString(file1, file2, new String[0], new String[0])));
    }

    private JobConfigHistoryBaseAction createJobConfigHistoryBaseAction() {
        return new JobConfigHistoryBaseAction() {

            @Override
            protected AccessControlled getAccessControlledObject() {
                return Jenkins.get();
            }

            @Override
            protected void checkConfigurePermission() {
                getAccessControlledObject()
                        .checkPermission(Permission.CONFIGURE);
            }

            @Override
            public boolean hasAdminPermission() {
                return getAccessControlledObject().hasPermission(Jenkins.ADMINISTER);
            }

            @Override
            public boolean hasDeleteEntryPermission() {
                return getAccessControlledObject().hasPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
            }

            @Override
            protected void checkDeleteEntryPermission() {
                getAccessControlledObject().checkPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
            }

            @Override
            protected boolean hasConfigurePermission() {
                return getAccessControlledObject()
                        .hasPermission(Permission.CONFIGURE);
            }

            @Override
            public int getRevisionAmount() {
                return -1;
            }

            public String getIconFileName() {
                return null;
            }

            public List<SideBySideView.Line> getLines(boolean useRegex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    /**
     * Test method for
     * {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffAsString(File, File, String[], String[])}.
     */
    @Test
    void testGetDiffFileStringStringDifferentLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        assertEquals("\n",
                makeResultPlatformIndependent(action.getDiffAsString(file1,
                        file2, "123\n346".split("\n"),
                        "123\n346\n".split("\n"))));
        assertEquals(
                "--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,3 @@\n 123\n 346\n+123\n",
                makeResultPlatformIndependent(action.getDiffAsString(file1,
                        file2, "123\n346".split("\n"),
                        "123\n346\n123".split("\n"))));
    }

    private String makeResultPlatformIndependent(final String result) {
        return result.replace("\\", "/");
    }

    @Test
    void testGetConfigXmlIllegalArgumentExceptionNonExistingJobName()
            throws IOException, SAXException {
        TextPage page = (TextPage) webClient.goTo(
                JobConfigHistoryConsts.URLNAME
                        + "/configOutput?type=raw&name=bogus&timestamp=2013-01-11_17-26-27",
                "text/plain");
        assertTrue(page.getContent().trim().isEmpty(),
                "Page should be empty.");
    }

    @Test
    void testGetConfigXmlIllegalArgumentExceptionInvalidTimestamp() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();

        assertThrows(IllegalArgumentException.class,
                () -> action.checkTimestamp("bla"));
    }

    @Issue("JENKINS-5534")
    @Test
    void testSecuredAccessToJobConfigHistoryPage()
            throws IOException, SAXException {
        // without security the jobConfigHistory-badge should show.
        final HtmlPage withoutSecurity = webClient.goTo("");
        assertThat(withoutSecurity.asXml(), Matchers
                .containsString("/jenkins/jobConfigHistory"));
        withoutSecurity.getAnchorByHref("/jenkins/" + JobConfigHistoryConsts.URLNAME);
        // with security enabled the jobConfigHistory-badge should not show
        // anymore.
        rule.jenkins.setSecurityRealm(
                new HudsonPrivateSecurityRealm(false, false, null));
        rule.jenkins.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        final HtmlPage withSecurityEnabled = webClient.goTo("");
        assertThat(withSecurityEnabled.asXml(), not(Matchers
                .containsString("/jenkins/jobConfigHistory")));

        assertThrows(ElementNotFoundException.class,
                () -> withSecurityEnabled
                    .getAnchorByHref("/jenkins/" + JobConfigHistoryConsts.URLNAME));
    }

    @Issue("JENKINS-17124")
    @Test
    void testClearDuplicateLines() throws Exception {
        final String jobName = "Test";

        final FreeStyleProject project = rule.createFreeStyleProject(jobName);
        project.setBuildDiscarder(new LogRotator(42, 42, -1, -1));
        project.save();
        Thread.sleep(SLEEP_TIME);
        LogRotator rotator = (LogRotator) project.getBuildDiscarder();
        assertEquals(42, rotator.getDaysToKeep());

        project.setBuildDiscarder(new LogRotator(47, 47, -1, -1));
        project.save();
        Thread.sleep(SLEEP_TIME);
        rotator = (LogRotator) project.getBuildDiscarder();
        assertEquals(47, rotator.getDaysToKeep());

        final HtmlPage historyPage = webClient
                .goTo("job/" + jobName + "/" + JobConfigHistoryConsts.URLNAME);
        final HtmlForm diffFilesForm = historyPage.getFormByName("diffFiles");
        final HtmlPage diffPage = rule.last(diffFilesForm.getElementsByTagName("button")).click();
        rule.assertStringContains(diffPage.asNormalizedText(), "<daysToKeep>42</daysToKeep>");
        rule.assertStringContains(diffPage.asNormalizedText(), "<numToKeep>42</numToKeep>");
        rule.assertStringContains(diffPage.asNormalizedText(), "<daysToKeep>47</daysToKeep>");
        rule.assertStringContains(diffPage.asNormalizedText(), "<numToKeep>47</numToKeep>");
    }
}
