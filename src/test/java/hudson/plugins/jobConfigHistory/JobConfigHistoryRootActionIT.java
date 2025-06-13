package hudson.plugins.jobConfigHistory;

import org.htmlunit.TextPage;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.LegacyAuthorizationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class JobConfigHistoryRootActionIT {

    // we need to sleep between saves so we don't overwrite the history
    // directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;
    private JenkinsRule.WebClient webClient;
    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
        webClient = rule.createWebClient();
        Logger.getLogger("org.htmlunit").setLevel(Level.OFF);
        Logger.getLogger("").setLevel(Level.WARNING);
        Logger.getLogger(this.getClass().getPackage().getName())
                .setLevel(Level.INFO);
        webClient.setJavaScriptEnabled(true);
    }

    /**
     * Tests whether info gets displayed correctly for filter parameter
     * none/system/jobs/deleted/created.
     */
    @Test
    void testFilterWithData() throws Exception {
        // create some config history data
        final FreeStyleProject project = rule.createFreeStyleProject("Test1");
        Thread.sleep(SLEEP_TIME);
        project.disable();
        Thread.sleep(SLEEP_TIME);

        rule.jenkins.setSystemMessage("Testmessage");
        Thread.sleep(SLEEP_TIME);

        final FreeStyleProject secondProject = rule.createFreeStyleProject("Test2");
        Thread.sleep(SLEEP_TIME);
        secondProject.delete();

        // check page with system history entries
        checkSystemPage(webClient.goTo(JobConfigHistoryConsts.URLNAME));
        checkSystemPage(webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=system"));

        // check page with job history entries
        final HtmlPage htmlPageJobs = webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=jobs");
        assertNotNull(htmlPageJobs.getAnchorByText("Test1"),
                "Verify history entry for job is listed.");
        final String htmlPageJobsBody = htmlPageJobs.asXml();
        assertTrue(htmlPageJobsBody.contains(DeletedFileFilter.DELETED_MARKER),
                "Verify history entry for deleted job is listed.");
        assertFalse(
                htmlPageJobsBody.contains("config (system)"),
                "Verify that no history entry for system change is listed.");
        assertTrue(htmlPageJobsBody
                .contains("job/Test1/" + JobConfigHistoryConsts.URLNAME), "Check link to job page.");

        // check page with 'created' history entries
        final HtmlPage htmlPageCreated = webClient
                .goTo("jobConfigHistory/?filter=created");
        assertNotNull(htmlPageCreated.getAnchorByText("Test1"),
                "Verify history entry for job is listed.");
        assertFalse(
                htmlPageCreated.asNormalizedText()
                        .contains(DeletedFileFilter.DELETED_MARKER),
                "Verify history entry for deleted job is not listed.");
        assertFalse(
                htmlPageCreated.asNormalizedText().contains("config (system)"),
                "Verify that no history entry for system change is listed.");
        assertTrue(htmlPageJobs.asXml()
                .contains("job/Test1/" + JobConfigHistoryConsts.URLNAME), "Check link to job page exists.");
        assertFalse(htmlPageCreated.asXml().contains("Deleted</td>")
                        || htmlPageCreated.asXml().contains("Changed</td>"),
                "Verify that only 'Created' entries are listed.");

        // check page with 'deleted' history entries
        final HtmlPage htmlPageDeleted = webClient
                .goTo("jobConfigHistory/?filter=deleted");
        final String page = htmlPageDeleted.asXml();
        System.out.println(page);
        assertTrue(page.contains(DeletedFileFilter.DELETED_MARKER),
                "Verify history entry for deleted job is listed.");
        assertFalse(
                page.contains("Test1"),
                "Verify no history entry for existing job is listed.");
        assertFalse(
                page.contains("(system)"),
                "Verify no history entry for system change is listed.");
        assertTrue(page.contains("history?name"),
                "Check link to historypage exists.");
        assertFalse(page.contains("Created</td>") || page.contains("Changed</td>"),
                "Verify that only 'Deleted' entries are listed.");
    }

    /**
     * Checks whether system config history is displayed correctly.
     *
     * @param htmlPage
     */
    private void checkSystemPage(HtmlPage htmlPage) {
        final String page = htmlPage.asXml();
        System.out.println(page);
        assertNotNull(htmlPage.getAnchorByText("config"),
                "Verify history entry for system change is listed.");
        assertFalse(page.contains("Test1"),
                "Verify no job history entry is listed.");
        assertFalse(page.contains(DeletedFileFilter.DELETED_MARKER),
                "Verify history entry for deleted job is listed.");
        assertTrue(page.contains("history?name"),
                "Check link to historypage exists.");
    }

    /**
     * System config history should only be visible with the right permissions.
     */
    @Test
    void testFilterWithoutPermissions() {
        rule.jenkins.setSecurityRealm(
                new HudsonPrivateSecurityRealm(false, false, null));
        rule.jenkins.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        assertDoesNotThrow(() -> {
            final HtmlPage htmlPage = webClient
                    .goTo(JobConfigHistoryConsts.URLNAME);
            assertTrue(htmlPage.asNormalizedText().contains("No permission to view"),
                    "Verify nothing is shown without permission");
        }, "Unable to complete testFilterWithoutPermissions: ");
    }

    /**
     * Tests whether the config history of a single system feature is displayed
     * correctly and showDiffs works.
     */
    @Test
    void testSingleSystemHistoryPage() {
        final String firstMessage = "First Testmessage";
        final String secondMessage = "Second Testmessage";

        // create some config history data
        assertDoesNotThrow(() -> {
            rule.jenkins.setSystemMessage(firstMessage);
            Thread.sleep(SLEEP_TIME);
            rule.jenkins.setSystemMessage(secondMessage);
            Thread.sleep(SLEEP_TIME);
        }, "Unable to prepare Jenkins instance: ");

        assertDoesNotThrow(() -> {
            final HtmlPage htmlPage = webClient.goTo(
                    JobConfigHistoryConsts.URLNAME + "/history?name=config");
            final String page = htmlPage.asXml();
            System.out.println(page);
            assertFalse(page.contains("No configuration history"),
                    "Check whether configuration data is found.");
            assertTrue(
                    page.split("Changed").length > 2,
                    "Verify several entries for config changes exist.");

            final HtmlForm diffFilesForm = htmlPage.getFormByName("diffFiles");
            final HtmlPage diffPage = rule.last(diffFilesForm.getElementsByTagName("button")).click();
            rule.assertStringContains(diffPage.asNormalizedText(), firstMessage);
            rule.assertStringContains(diffPage.asNormalizedText(), secondMessage);
        }, "Unable to complete testHistoryPage: ");
    }

    /**
     * Tests whether config history of single deleted job is displayed
     * correctly.
     */
    @Test
    void testSingleDeletedJobHistoryPage() {
        // create some config history data
        assertDoesNotThrow(() -> {
            final FreeStyleProject project = rule.createFreeStyleProject("Test");
            Thread.sleep(SLEEP_TIME);
            project.delete();
        }, "Unable to prepare Jenkins instance: ");
        assertDoesNotThrow(() -> {
            final HtmlPage htmlPage = webClient
                    .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
            final HtmlAnchor deletedLink = (HtmlAnchor) htmlPage
                    .getElementById("deleted");
            final String historyPage = ((HtmlPage) deletedLink.click()).asXml();
            assertFalse(historyPage.contains("No configuration history"),
                    "Check whether configuration data is found.");
            assertTrue(historyPage.contains("Created"),
                    "Verify entry for creation exists.");
            assertTrue(historyPage.contains("Deleted"),
                    "Verify entry for deletion exists.");
        }, "Unable to complete testHistoryPage: ");
    }

    @Test
    void testGetOldConfigXmlWithWrongParameters() {
        final JobConfigHistoryRootAction rootAction = new JobConfigHistoryRootAction();

        assertThrows(IllegalArgumentException.class,
                () -> rootAction.getOldConfigXml("bla", "bogus"));

        final String timestamp = new SimpleDateFormat(
                JobConfigHistoryConsts.ID_FORMATTER)
                .format(new GregorianCalendar().getTime());
        assertThrows(IllegalArgumentException.class,
                () -> rootAction.getOldConfigXml("bla..blubb", timestamp));
    }

    @Test
    void testDeletedAfterDisabled() throws Exception {
        final String description = "All your base";
        final FreeStyleProject project = rule.createFreeStyleProject("Test");
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage htmlPage = webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final HtmlAnchor rawLink = htmlPage.getAnchorByText("(RAW)");
        final String rawPage = ((TextPage) rawLink.click()).getContent();
        assertTrue(rawPage.contains(description),
                "Verify config file is shown");
    }

    /**
     * Tests if restoring a project that was disabled before deletion works.
     *
     * @throws Exception
     */
    @Test
    void testRestoreAfterDisabled() throws Exception {
        final String description = "bla";
        final String name = "TestProject";
        final FreeStyleProject project = rule.createFreeStyleProject(name);
        project.setDescription(description);
        project.disable();
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);

        final HtmlPage historyPage = webClient
                .goTo("job/" + name + "/" + JobConfigHistoryConsts.URLNAME);
        final String historyAsXml = historyPage.asXml();
        System.out.println(historyAsXml);
        assertTrue(historyAsXml.contains("Deleted"),
                "History page should contain 'Deleted' entry");
        final List<HtmlAnchor> hrefs = historyPage
                .getByXPath("//a[contains(@href, \"configOutput?type=xml\")]");
        assertTrue(hrefs.size() > 2);
    }

    /**
     * Tests whether finding a new name for a restored project works if the old
     * name is already occupied.
     *
     * @throws Exception
     */
    @Test
    void testRestoreWithSameName() throws Exception {
        final String description = "blubb";
        final String name = "TestProject";
        final FreeStyleProject project = rule.createFreeStyleProject(name);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();

        rule.createFreeStyleProject(name);

        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, description);
        WebAssert.assertTextPresent(jobPage, name + "_1");
    }

    /**
     * Tests that project gets restored even without previous configs, because
     * there is one saved at the time of deletion.
     *
     * @throws Exception
     */
    @LocalData
    @Test
    void testRestoreWithoutConfigs() throws Exception {
        final String name = "JobWithNoConfigHistory";
        final FreeStyleProject project = (FreeStyleProject) rule.jenkins
                .getItem(name);
        final String description = project.getDescription();
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);
    }

    /**
     * A project will not be restored if there are no configs present and it has
     * been disabled at the time of deletion.
     *
     * @throws Exception
     */
    @LocalData
    @Test
    void testNoRestoreLinkWhenNoConfigs() throws Exception {
        final String name = "DisabledJobWithNoConfigHistory";
        final FreeStyleProject project = (FreeStyleProject) rule.jenkins
                .getItem(name);
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage htmlPage = webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        WebAssert.assertElementNotPresentByXPath(htmlPage,
                ("//images/symbols[contains(@src, \"restore.svg\")]"));
    }

    private HtmlPage restoreProject() throws Exception {
        final HtmlPage htmlPage = webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final HtmlAnchor restoreLink = (HtmlAnchor) htmlPage
                .getElementById("restore");
        final HtmlPage reallyRestorePage = restoreLink.click();
        final HtmlForm restoreForm = reallyRestorePage.getFormByName("restore");
        return rule.submit(restoreForm, "Submit");
    }

    /**
     * Tests whether the 'Restore project' button on the history page works as
     * well.
     *
     * @throws Exception
     */
    @Test
    void testRestoreFromHistoryPage() throws Exception {
        final String description = "All your base";
        final String name = "TestProject";
        final FreeStyleProject project = rule.createFreeStyleProject(name);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage htmlPage = webClient
                .goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final List<HtmlAnchor> hrefs = htmlPage
                .getByXPath("//a[contains(@href, \"TestProject_deleted_\")]");
        final HtmlPage historyPage = hrefs.get(0).click();
        final HtmlPage reallyRestorePage = rule.submit(
                historyPage.getFormByName("forward"), "Submit");
        final HtmlPage jobPage = rule.submit(
                reallyRestorePage.getFormByName("restore"), "Submit");

        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);
    }
}
