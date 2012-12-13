package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigBadgeActionTest extends AbstractHudsonTestCaseDeletingInstanceDir {
    
    private WebClient webClient;
    private static final int SLEEP_TIME = 1100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = new WebClient();
    }

    public void testBadgeAction() throws Exception {
        
        final String jobName = "newjob";
        final String description1 = "a description";
        final String description2 = "a different description";

        final FreeStyleProject project = createFreeStyleProject(jobName);
        project.setDescription(description1);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertFalse("Page does not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        
        project.setDescription(description2);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();
        
        final HtmlPage htmlPage2 = webClient.goTo("job/" + jobName);
        assertTrue("Page contains build badge", htmlPage2.asXml().contains("buildbadge.png"));
    }
}
