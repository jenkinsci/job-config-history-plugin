package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigHistoryProjectActionTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;
    // we need to sleep between saves so we don't overwrite the history directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    public void testRestore() {
        final String firstDescription = "first test";
        final String secondDescription = "second test";
        
        try {
            final FreeStyleProject project = createFreeStyleProject("Test1");
            Thread.sleep(SLEEP_TIME);
            project.setDescription(firstDescription);
            Thread.sleep(SLEEP_TIME);
            project.setDescription(secondDescription);
            Thread.sleep(SLEEP_TIME);
/*            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);
            
            for (ConfigInfo config : projectAction.getJobConfigs()){
                System.out.println("Operation: " + config.getOperation());
                System.out.println("Timestamp: " + config.getDate());
            }
*/
            assertEquals(project.getDescription(), secondDescription);

            final HtmlPage htmlpage = webClient.goTo("job/Test1/jobConfigHistory");
            final HtmlAnchor restoreLink = (HtmlAnchor)htmlpage.getElementById("restore2");
            final HtmlPage reallyRestorePage = restoreLink.click();
            final HtmlForm restoreForm = reallyRestorePage.getFormByName("restore");
            final HtmlPage results = submit(restoreForm, "Submit");
            
            assertStringContains(results.getTitleText(), "Test1");
            assertEquals(project.getDescription(), firstDescription);
            
        } catch (Exception ex) {
            fail("Unable to complete restore config test: " + ex);
        }
    }
}
