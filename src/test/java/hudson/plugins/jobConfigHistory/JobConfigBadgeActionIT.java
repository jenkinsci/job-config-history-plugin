package hudson.plugins.jobConfigHistory;

import org.acegisecurity.context.SecurityContextHolder;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.PresetData;
import org.jvnet.hudson.test.recipes.PresetData.DataSet;

import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigBadgeActionIT extends AbstractHudsonTestCaseDeletingInstanceDir {
    
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
        final String oldName = "firstjobname";
        final String newName = "secondjobname";

        final FreeStyleProject project = createFreeStyleProject(oldName);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        project.renameTo(newName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + newName);
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage.asText().contains("No lines changed"));
    }
    
    
    public void testCorrectLinkTargetsAfterRename() throws Exception {
        final String oldName = "jobname1";
        final String newName = "jobname2";
        final String oldDescription = "first description";
        final String newDescription = "second description";

        final FreeStyleProject project = createFreeStyleProject(oldName);
        project.setDescription(oldDescription);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        project.setDescription(newDescription);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + oldName);
        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage.asText().contains("Older"));

        project.renameTo(newName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        //Test whether build badge link that was created before rename still leads to correct page
        final HtmlPage htmlPage2 = webClient.goTo("job/" + newName);
        final HtmlAnchor oldShowDiffLink = (HtmlAnchor) htmlPage2.getByXPath("//a[@id='showDiff']").get(1);
        final HtmlPage showDiffPage2 = oldShowDiffLink.click();
        assertTrue("ShowDiffFiles page should be reached now", showDiffPage2.asText().contains("Older"));
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

    @PresetData(DataSet.ANONYMOUS_READONLY)
    public void testBadgeConfigurationAnonymous() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = createFreeStyleProject(jobName);

        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        
        hudson.setSecurityRealm(createDummySecurityRealm());
        SecurityContextHolder.getContext().setAuthentication(Hudson.ANONYMOUS);
        shouldPageContainBadge("anonymous");
    }

    @LocalData
    public void testBadgeConfigurationWithPermissions() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = createFreeStyleProject(jobName);

        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        
        hudson.setSecurityRealm(createDummySecurityRealm());
        webClient.login("configUser");
        shouldPageContainBadge("configUser");
        
        webClient.login("administrator");
        shouldPageContainBadge("admin");
    }

    private void shouldPageContainBadge(String user) throws Exception{
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        HtmlPage htmlPage = webClient.goTo("job/newjob");
        
        //default = always
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        jch.setShowBuildBadges("never");
        htmlPage = (HtmlPage) htmlPage.refresh();
        assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        
        jch.setShowBuildBadges("userWithConfigPermission");
        htmlPage = (HtmlPage) htmlPage.refresh();
        
        if (("configUser").equals(user) || ("admin").equals(user)) {
            assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        } else {
            assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        }

        jch.setShowBuildBadges("adminUser");
        htmlPage = (HtmlPage) htmlPage.refresh();
        
        if (("admin").equals(user)) {
            assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        } else {
            assertFalse("Page should not contain build badge", htmlPage.asXml().contains("buildbadge.png"));
        }
    }

    public void testCorrectShowDiffLinkWithSingleChange() throws Exception {
        final String jobName = "testjob";
        final FreeStyleProject project = createFreeStyleProject(jobName);
        project.setDescription("first description");
        Thread.sleep(SLEEP_TIME);

        final String secondDescription = "second description";
        project.setDescription(secondDescription);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        
        final String lastDescription = "last description";
        project.setDescription(lastDescription);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertTrue("Page should contain build badge", htmlPage.asXml().contains("buildbadge.png"));

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        final String page = showDiffPage.asText();
        assertTrue("ShowDiffFiles page should be reached now", page.contains("Older"));
        assertTrue("ShowDiff page should contain second description", page.contains(secondDescription));
        assertTrue("ShowDiff page should contain last description", page.contains(lastDescription));
    }

    public void testCorrectShowDiffLinkWithMultipleChanges() throws Exception {
        final String jobName = "testjob";

        final FreeStyleProject project = createFreeStyleProject(jobName);
        project.setDescription("first description");
        Thread.sleep(SLEEP_TIME);
        final String secondDescription = "second description";
        project.setDescription(secondDescription);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        for (int i = 3; i < 6; i++) {
            project.setDescription("decription no. " + i);
            Thread.sleep(SLEEP_TIME);
        }
        final String lastDescription = "last description";
        project.setDescription(lastDescription);
        Thread.sleep(SLEEP_TIME);
        assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        final String page = showDiffPage.asText();
        assertTrue("ShowDiffFiles page should be reached now", page.contains("Older"));
        assertTrue("ShowDiff page should contain second description", page.contains(secondDescription));
        assertTrue("ShowDiff page should contain last description", page.contains(lastDescription));
    }
}
