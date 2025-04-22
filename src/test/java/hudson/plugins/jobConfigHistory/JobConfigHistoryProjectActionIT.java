package hudson.plugins.jobConfigHistory;

import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class JobConfigHistoryProjectActionIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {

    // we need to sleep between saves so we don't overwrite the history
    // directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;
    private JenkinsRule.WebClient webClient;

    @Override
    void setUp(JenkinsRule rule) throws Exception {
        super.setUp(rule);
        webClient = rule.createWebClient();
    }

    /**
     * Tests restore link on job config history page.
     */
    @Test
    void testRestore() {
        final String firstDescription = "first test";
        final String secondDescription = "second test";
        final String projectName = "Test1";

        assertDoesNotThrow(() -> {
            final FreeStyleProject project = rule.createFreeStyleProject(
                    projectName);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(firstDescription);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(secondDescription);
            Thread.sleep(SLEEP_TIME);

            assertEquals(secondDescription, project.getDescription());

            final HtmlPage htmlPage = webClient.goTo("job/" + projectName + "/"
                    + JobConfigHistoryConsts.URLNAME);
            final HtmlAnchor restoreLink = (HtmlAnchor) htmlPage
                    .getElementById("restore2");
            final HtmlPage reallyRestorePage = restoreLink.click();
            final HtmlForm restoreForm = reallyRestorePage
                    .getFormByName("restore");
            final HtmlPage jobPage = rule.submit(restoreForm, "Submit");

            assertTrue(
                    jobPage.asNormalizedText().contains(firstDescription),
                    "Verify return to job page and changed description.");
            assertEquals(firstDescription, project.getDescription(), "Verify changed description.");

        }, "Unable to complete restore config test: ");
    }

    /**
     * Tests restore button on "Really restore?" page.
     */
    @Test
    void testRestoreFromDiffFiles() {
        final String firstDescription = "first test";
        final String secondDescription = "second test";
        final String projectName = "Test1";

        assertDoesNotThrow(() -> {
            final FreeStyleProject project = rule.createFreeStyleProject(projectName);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(firstDescription);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(secondDescription);
            Thread.sleep(SLEEP_TIME);

            assertEquals(secondDescription, project.getDescription());

            final HtmlPage htmlPage = webClient.goTo("job/" + projectName + "/"
                    + JobConfigHistoryConsts.URLNAME);
            final HtmlPage diffPage = rule.submit(
                    htmlPage.getFormByName("diffFiles"), "Submit");
            final HtmlPage reallyRestorePage = rule.submit(
                    diffPage.getFormByName("forward"), "Submit");
            final HtmlPage jobPage = rule.submit(
                    reallyRestorePage.getFormByName("restore"), "Submit");

            assertTrue(
                    jobPage.asNormalizedText().contains(firstDescription),
                    "Verify return to job page and changed description.");
            assertEquals(firstDescription, project.getDescription(), "Verify changed description.");
        }, "Unable to complete restore config test: ");
    }
}
