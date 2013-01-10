package hudson.plugins.jobConfigHistory;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
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
        final String description = "a description";
        final FreeStyleProject project = createFreeStyleProject(jobName);
        AbstractBuild<?,?> build = project.scheduleBuild2(0).get();
        assertTrue("Build should succeed", build.getResult().equals(Result.SUCCESS));

        final HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();
        
        final HtmlPage htmlPage2 = webClient.goTo("job/" + jobName);
        assertTrue("Page should contain build badge", htmlPage2.asXml().contains("buildbadge.png"));
    }
    
    public void testBadgeAfterRename() throws Exception {
        final String oldJobName = "firstjobname";
        final String newJobName = "secondjobname";

        final FreeStyleProject project = createFreeStyleProject(oldJobName);
        AbstractBuild<?,?> build = project.scheduleBuild2(0).get();
        assertTrue("Build should succeed", build.getResult().equals(Result.SUCCESS));
        Thread.sleep(SLEEP_TIME);

        project.renameTo(newJobName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + newJobName);
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage.asText().contains("No lines changed"));
    }
    
    
    public void testCorrectLinkTargetsAfterRename() throws Exception {
        final String oldJobName = "jobname1";
        final String newJobName = "jobname2";
        final String oldDescription = "first description";
        final String newDescription = "second description";

        final FreeStyleProject project = createFreeStyleProject(oldJobName);
        project.setDescription(oldDescription);
        AbstractBuild<?,?> build = project.scheduleBuild2(0).get();
        assertTrue("Build should succeed", build.getResult().equals(Result.SUCCESS));
        Thread.sleep(SLEEP_TIME);

        project.setDescription(newDescription);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + oldJobName);
        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage.asText().contains("Restore old version"));

        project.renameTo(newJobName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        //Test whether build badge link that was created before rename still leads to correct page
        final HtmlPage htmlPage2 = webClient.goTo("job/" + newJobName);
        final HtmlAnchor oldShowDiffLink = (HtmlAnchor) htmlPage2.getByXPath("//a[@id='showDiff']").get(1);
        final HtmlPage showDiffPage2 = oldShowDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage2.asText().contains("Restore old version"));
    }
}
