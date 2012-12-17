package hudson.plugins.jobConfigHistory;

import java.util.ArrayList;
import java.util.List;

import hudson.model.Build;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;

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
    
    public void testCorrectLinkTargets() throws Exception {
        final String oldJobName = "firstjobname";
        final String newJobName = "secondjobname";

        final FreeStyleProject project = createFreeStyleProject(oldJobName);
        Thread.sleep(SLEEP_TIME);
        AbstractBuild<?,?> build = project.scheduleBuild2(0).get();
        assertTrue("Build should succeed", build.getResult().equals(Result.SUCCESS));

        project.renameTo(newJobName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + newJobName);
        System.out.println(htmlPage.asXml());
        
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage.asText().contains("Restore old version"));
        
        //Same procedure again in order to test whether the link 
        //is still correct on the showDiffFiles page
        final HtmlAnchor showDiffLink2 = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage2 = showDiffLink2.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage2.asText().contains("Restore old version"));
    }
}
