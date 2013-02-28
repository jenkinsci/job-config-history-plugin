package hudson.plugins.jobConfigHistory;

import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.LegacyAuthorizationStrategy;

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

        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        htmlPage = (HtmlPage)htmlPage.refresh();
        assertFalse("Page should still not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        
        htmlPage = (HtmlPage)htmlPage.refresh();
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));
    }
    
    public void testBadgeAfterRename() throws Exception {
        final String oldJobName = "firstjobname";
        final String newJobName = "secondjobname";

        final FreeStyleProject project = createFreeStyleProject(oldJobName);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
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
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
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
    
    public void testProjectWithConfigsButMissingBuilds() throws Exception {
        final FreeStyleProject project = createFreeStyleProject();
        Thread.sleep(SLEEP_TIME);
        project.setDescription("bla");
        Thread.sleep(SLEEP_TIME);
        project.updateNextBuildNumber(5);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }
   
    @LocalData
    public void testBuildWithoutHistoryDir() throws Exception {
        final FreeStyleProject project = (FreeStyleProject) hudson.getItem("Test1");
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }
    
    @LocalData
    public void testBuildWithoutHistoryEntries() throws Exception {
        final FreeStyleProject project = (FreeStyleProject) hudson.getItem("Test2");
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }
    
    public void testBadgeConfigurationOptions() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = createFreeStyleProject(jobName);

        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        
        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        //default = always
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        jch.setShowBuildBadges("never");
        htmlPage = (HtmlPage)htmlPage.refresh();
        assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(false, false, null));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        jch.setShowBuildBadges("userWithConfigPermission");
        htmlPage = (HtmlPage)htmlPage.refresh();
        assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(true, false, null));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        jch.setShowBuildBadges("userWithConfigPermission");
        htmlPage = (HtmlPage)htmlPage.refresh();
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));
    }
    
}
