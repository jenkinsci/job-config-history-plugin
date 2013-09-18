package hudson.plugins.jobConfigHistory;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.FreeStyleProject;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigHistoryRootActionIT extends
        AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;
    // we need to sleep between saves so we don't overwrite the history
    // directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    /**
     * Tests whether info gets displayed correctly for filter parameter
     * none/system/jobs/deleted/created.
     */
    public void testFilterWithData() throws Exception {

        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        jch.setSaveSystemConfiguration(true);

        // create some config history data
        final FreeStyleProject project = createFreeStyleProject("Test1");
        Thread.sleep(SLEEP_TIME);
        project.disable();
        Thread.sleep(SLEEP_TIME);

        hudson.setSystemMessage("Testmessage");
        Thread.sleep(SLEEP_TIME);

        final FreeStyleProject secondProject = createFreeStyleProject("Test2");
        Thread.sleep(SLEEP_TIME);
        secondProject.delete();

        // check page with system history entries
        checkSystemPage(webClient.goTo(JobConfigHistoryConsts.URLNAME));
        checkSystemPage(webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=system"));

        // check page with job history entries
        final HtmlPage htmlPageJobs = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=jobs");
        assertTrue("Verify history entry for job is listed.", htmlPageJobs.getAnchorByText("Test1") != null);
        assertTrue("Verify history entry for deleted job is listed.", htmlPageJobs.asText().contains(JobConfigHistoryConsts.DELETED_MARKER));
        assertFalse("Verify that no history entry for system change is listed.", htmlPageJobs.asText().contains("config (system)"));
        assertTrue("Check link to job page.", htmlPageJobs.asXml().contains("job/Test1/" + JobConfigHistoryConsts.URLNAME));

        // check page with 'created' history entries
        final HtmlPage htmlPageCreated = webClient.goTo("jobConfigHistory/?filter=created");
        assertTrue("Verify history entry for job is listed.", htmlPageCreated.getAnchorByText("Test1") != null);
        assertFalse("Verify history entry for deleted job is not listed.",
                htmlPageCreated.asText().contains(JobConfigHistoryConsts.DELETED_MARKER));
        assertFalse("Verify that no history entry for system change is listed.",
                htmlPageCreated.asText().contains("config (system)"));
        assertTrue("Check link to job page exists.", htmlPageJobs.asXml().contains("job/Test1/" + JobConfigHistoryConsts.URLNAME));
        assertFalse("Verify that only \'Created\' entries are listed.",
                htmlPageCreated.asXml().contains("Deleted</td>") || htmlPageCreated.asXml().contains("Changed</td>"));

        // check page with 'deleted' history entries
        final HtmlPage htmlPageDeleted = webClient.goTo("jobConfigHistory/?filter=deleted");
        final String page = htmlPageDeleted.asXml();
        System.out.println(page);
        assertTrue("Verify history entry for deleted job is listed.", page.contains(JobConfigHistoryConsts.DELETED_MARKER));
        assertFalse("Verify no history entry for existing job is listed.", page.contains("Test1"));
        assertFalse("Verify no history entry for system change is listed.", page.contains("(system)"));
        assertTrue("Check link to historypage exists.", page.contains("history?name"));
        assertFalse("Verify that only \'Deleted\' entries are listed.",
                page.contains("Created</td>") || page.contains("Changed</td>"));
    }

    /**
     * Checks whether system config history is displayed correctly.
     * 
     * @param htmlPage
     */
    private void checkSystemPage(HtmlPage htmlPage) {
        final String page = htmlPage.asXml();
        assertTrue("Verify history entry for system change is listed.", htmlPage.getAnchorByText("config") != null);
        assertFalse("Verify no job history entry is listed.", page.contains("Test1"));
        assertFalse("Verify history entry for deleted job is listed.", page.contains(JobConfigHistoryConsts.DELETED_MARKER));
        assertTrue("Check link to historypage exists.", page.contains("history?name"));
    }

    /**
     * If there is no config history available, it should say so.
     */
    public void testFilterWithoutData() {
        try {
            final HtmlPage htmlPage = webClient.goTo("jobConfigHistory");
            assertTrue(htmlPage.asText().contains("No configuration history"));
        } catch (Exception ex) {
            fail("Unable to complete testFilterWithoutData: " + ex);
        }
    }

    /**
     * System config history should only be visible with the right permissions.
     */
    public void testFilterWithoutPermissions() {
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(false, false,
                null));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        try {
            final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME);
            assertTrue("Verify nothing is shown without permission", 
                    htmlPage.asText().contains("No permission to view"));
        } catch (Exception ex) {
            fail("Unable to complete testFilterWithoutPermissions: " + ex);
        }
    }

    /**
     * Tests whether the config history of a single system feature is displayed
     * correctly and showDiffs works.
     */
    public void testSingleSystemHistoryPage() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        jch.setSaveSystemConfiguration(true);
        final String firstMessage = "First Testmessage";
        final String secondMessage = "Second Testmessage";

        // create some config history data
        try {
            hudson.setSystemMessage(firstMessage);
            Thread.sleep(SLEEP_TIME);
            hudson.setSystemMessage(secondMessage);
            Thread.sleep(SLEEP_TIME);
        } catch (Exception ex) {
            fail("Unable to prepare Hudson instance: " + ex);
        }

        try {
            final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/history?name=config");
            final String page = htmlPage.asXml();
            assertFalse("Check whether configuration data is found.", page.contains("No configuration history"));
            assertTrue("Verify several entries for config changes exist.",
                    page.split("Changed").length > 2);

            final HtmlForm diffFilesForm = htmlPage.getFormByName("diffFiles");
            final HtmlPage diffPage = (HtmlPage) diffFilesForm
                    .submit((HtmlButton) last(diffFilesForm
                            .getHtmlElementsByTagName("button")));
            assertStringContains(diffPage.asText(), firstMessage);
            assertStringContains(diffPage.asText(), secondMessage);
        } catch (Exception ex) {
            fail("Unable to complete testHistoryPage: " + ex);
        }
    }

    /**
     * Tests whether config history of single deleted job is displayed
     * correctly.
     */
    public void testSingleDeletedJobHistoryPage() {
        // create some config history data
        try {
            final FreeStyleProject project = createFreeStyleProject("Test");
            Thread.sleep(SLEEP_TIME);
            project.delete();
        } catch (Exception ex) {
            fail("Unable to prepare Hudson instance: " + ex);
        }
        try {
            final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
            final HtmlAnchor deletedLink = (HtmlAnchor) htmlPage.getElementById("deleted");
            final String historyPage = ((HtmlPage) deletedLink.click()).asXml();
            assertFalse("Check whether configuration data is found.",
                    historyPage.contains("No configuration history"));
            assertTrue("Verify entry for creation exists.", historyPage.contains("Created"));
            assertTrue("Verify entry for deletion exists.",    historyPage.contains("Deleted"));
        } catch (Exception ex) {
            fail("Unable to complete testHistoryPage: " + ex);
        }
    }

    public void testGetOldConfigXmlWithWrongParameters() {
        final JobConfigHistoryRootAction rootAction = new JobConfigHistoryRootAction();
        try {
            rootAction.getOldConfigXml("bla", "bogus");
            fail("Expected " + IllegalArgumentException.class + " because of invalid timestamp.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }

        try {
            final String timestamp = new SimpleDateFormat(
                    JobConfigHistoryConsts.ID_FORMATTER)
                    .format(new GregorianCalendar().getTime());
            rootAction.getOldConfigXml("bla..blubb", timestamp);
            fail("Expected " + IllegalArgumentException.class + " because of '..' in parameter name.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }

    public void testDeletedAfterDisabled() throws Exception {
        final String description = "All your base";
        final FreeStyleProject project = createFreeStyleProject("Test");
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();

        final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final HtmlAnchor rawLink = (HtmlAnchor) htmlPage.getAnchorByText("(RAW)");
        final String rawPage = ((TextPage) rawLink.click()).getContent();
        assertTrue("Verify config file is shown", rawPage.contains(description));
    }
    
    
    /**
     * Tests if restoring a project that was disabled before deletion works.
     * @throws Exception
     */
    public void testRestoreAfterDisabled() throws Exception {
        final String description = "bla";
        final String name = "TestProject";
        final FreeStyleProject project = createFreeStyleProject(name);
        project.setDescription(description);
        project.disable();
        Thread.sleep(SLEEP_TIME);
        project.delete();
        
        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);
        
        final HtmlPage historyPage = webClient.goTo("job/" + name + "/" + JobConfigHistoryConsts.URLNAME);
        System.out.println(historyPage.asXml());
        assertTrue("History page should contain 'Deleted' entry", historyPage.asXml().contains("Deleted"));
        final List<HtmlAnchor> hrefs = historyPage.getByXPath("//a[contains(@href, \"configOutput?type=xml\")]");
        assertTrue(hrefs.size() > 2);
    }
    
    /**
     * Tests whether finding a new name for a restored project works 
     * if the old name is already occupied.
     * @throws Exception
     */
    public void testRestoreWithSameName() throws Exception {
        final String description = "blubb";
        final String name = "TestProject";
        final FreeStyleProject project = createFreeStyleProject(name);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();
        
        createFreeStyleProject(name);

        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, description);
        WebAssert.assertTextPresent(jobPage, name + "_1");
    }

    /**
     * Tests that project gets restored even without previous configs,
     * because there is one saved at the time of deletion.
     * @throws Exception
     */
    @LocalData
    public void testRestoreWithoutConfigs() throws Exception {
        final String name = "JobWithNoConfigHistory";
        final FreeStyleProject project = (FreeStyleProject) hudson.getItem(name);
        final String description = project.getDescription();
        Thread.sleep(SLEEP_TIME);
        project.delete();
        
        final HtmlPage jobPage = restoreProject();
        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);
    }

    /**
     * A project will not be restored if there are no configs present 
     * and it has been disabled at the time of deletion.
     * @throws Exception
     */
    @LocalData
    public void testNoRestoreLinkWhenNoConfigs() throws Exception {
        final String name = "DisabledJobWithNoConfigHistory";
        final FreeStyleProject project = (FreeStyleProject) hudson.getItem(name);
        Thread.sleep(SLEEP_TIME);
        project.delete();
        
        final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        WebAssert.assertElementNotPresentByXPath(htmlPage, ("//img[contains(@src, \"restore.png\")]"));
    }
 
    private HtmlPage restoreProject() throws Exception {
        final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final HtmlAnchor restoreLink = (HtmlAnchor) htmlPage.getElementById("restore");
        final HtmlPage reallyRestorePage = (HtmlPage) restoreLink.click();
        final HtmlForm restoreForm = reallyRestorePage.getFormByName("restore");
        final HtmlPage jobPage = submit(restoreForm, "Submit");
        return jobPage;
    }
    
    /**
     * Tests whether the 'Restore project' button on the history page works as well.
     * @throws Exception
     */
    public void testRestoreFromHistoryPage() throws Exception {
        final String description = "All your base";
        final String name = "TestProject";
        final FreeStyleProject project = createFreeStyleProject(name);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.delete();
        
        final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=deleted");
        final List<HtmlAnchor> hrefs = htmlPage.getByXPath("//a[contains(@href, \"TestProject_deleted_\")]");
        final HtmlPage historyPage = (HtmlPage) hrefs.get(0).click();
        final HtmlPage reallyRestorePage = submit(historyPage.getFormByName("forward"), "Submit");
        final HtmlPage jobPage = submit(reallyRestorePage.getFormByName("restore"), "Submit");

        WebAssert.assertTextPresent(jobPage, name);
        WebAssert.assertTextPresent(jobPage, description);
    }
}
