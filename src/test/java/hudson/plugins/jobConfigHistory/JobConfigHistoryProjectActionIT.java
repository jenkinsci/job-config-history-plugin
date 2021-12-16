package hudson.plugins.jobConfigHistory;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.junit.Assert;

public class JobConfigHistoryProjectActionIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {

    // we need to sleep between saves so we don't overwrite the history
    // directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;
    private WebClient webClient;

    @Override
    public void before() throws Throwable {
        super.before();
        webClient = createWebClient();
    }

    /**
     * Tests restore link on job config history page.
     */
    public void testRestore() {
        final String firstDescription = "first test";
        final String secondDescription = "second test";
        final String projectName = "Test1";

        try {
            final FreeStyleProject project = createFreeStyleProject(
                    projectName);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(firstDescription);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(secondDescription);
            Thread.sleep(SLEEP_TIME);

            Assert.assertEquals(project.getDescription(), secondDescription);

            final HtmlPage htmlPage = webClient.goTo("job/" + projectName + "/"
                    + JobConfigHistoryConsts.URLNAME);
            final HtmlAnchor restoreLink = (HtmlAnchor) htmlPage
                    .getElementById("restore2");
            final HtmlPage reallyRestorePage = restoreLink.click();
            final HtmlForm restoreForm = reallyRestorePage
                    .getFormByName("restore");
            final HtmlPage jobPage = submit(restoreForm, "Submit");

            Assert.assertTrue(
                    "Verify return to job page and changed description.",
                    jobPage.asText().contains(firstDescription));
            Assert.assertEquals("Verify changed description.",
                    project.getDescription(), firstDescription);

        } catch (Exception ex) {
            Assert.fail("Unable to complete restore config test: " + ex);
        }
    }

    /**
     * Tests restore button on "Really restore?" page.
     */
    public void testRestoreFromDiffFiles() {
        final String firstDescription = "first test";
        final String secondDescription = "second test";
        final String projectName = "Test1";
        final FreeStyleProject project;

        try {
            project = createFreeStyleProject(projectName);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(firstDescription);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(secondDescription);
            Thread.sleep(SLEEP_TIME);

            Assert.assertEquals(project.getDescription(), secondDescription);

            final HtmlPage htmlPage = webClient.goTo("job/" + projectName + "/"
                    + JobConfigHistoryConsts.URLNAME);
            final HtmlPage diffPage = submit(
                    htmlPage.getFormByName("diffFiles"), "Submit");
            final HtmlPage reallyRestorePage = submit(
                    diffPage.getFormByName("forward"), "Submit");
            final HtmlPage jobPage = submit(
                    reallyRestorePage.getFormByName("restore"), "Submit");

            Assert.assertTrue(
                    "Verify return to job page and changed description.",
                    jobPage.asText().contains(firstDescription));
            Assert.assertEquals("Verify changed description.",
                    project.getDescription(), firstDescription);
        } catch (Exception ex) {
            Assert.fail("Unable to complete restore config test: " + ex);
        }
    }
}
