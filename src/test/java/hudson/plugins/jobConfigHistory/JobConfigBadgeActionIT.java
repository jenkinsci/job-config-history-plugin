package hudson.plugins.jobConfigHistory;

import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.PresetData;
import org.jvnet.hudson.test.recipes.PresetData.DataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class JobConfigBadgeActionIT {

    private static final int SLEEP_TIME = 1100;
    private JenkinsRule.WebClient webClient;
    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
        webClient = rule.createWebClient();
    }

    @Test
    void testBadgeAction() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = rule.createFreeStyleProject(jobName);

        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertThat(htmlPage.asXml(), not(containsString("symbol-buildbadge")));

        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        htmlPage = (HtmlPage) htmlPage.refresh();
        assertThat(htmlPage.asXml(), not(containsString("symbol-buildbadge")));

        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        htmlPage = (HtmlPage) htmlPage.refresh();
        assertThat(htmlPage.asXml(), containsString("symbol-buildbadge"));
    }

    @Test
    void testBadgeAfterRename() throws Exception {
        final String oldName = "firstjobname";
        final String newName = "secondjobname";

        final FreeStyleProject project = rule.createFreeStyleProject(oldName);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        project.renameTo(newName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + newName);
        assertThat(htmlPage.asXml(), containsString("symbol-buildbadge"));

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
                .getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue(showDiffPage.asNormalizedText().contains("No lines changed"),
                "ShowDiffFiles page should be reached now");
    }

    @Test
    void testCorrectLinkTargetsAfterRename() throws Exception {
        final String oldName = "jobname1";
        final String newName = "jobname2";
        final String oldDescription = "first description";
        final String newDescription = "second description";

        final FreeStyleProject project = rule.createFreeStyleProject(oldName);
        project.setDescription(oldDescription);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        project.setDescription(newDescription);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        final HtmlPage htmlPage = webClient.goTo("job/" + oldName);
        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
                .getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        assertTrue(showDiffPage.asNormalizedText().contains("Older"),
                "ShowDiffFiles page should be reached now");

        project.renameTo(newName);
        Thread.sleep(SLEEP_TIME);
        project.scheduleBuild2(0).get();

        // Test whether build badge link that was created before rename still
        // leads to correct page
        final HtmlPage htmlPage2 = webClient.goTo("job/" + newName);
        final HtmlAnchor oldShowDiffLink = (HtmlAnchor) htmlPage2
                .getByXPath("//a[@id='showDiff']").get(1);
        final HtmlPage showDiffPage2 = oldShowDiffLink.click();
        assertTrue(showDiffPage2.asNormalizedText().contains("Older"),
                "ShowDiffFiles page should be reached now");
    }

    @Test
    void testProjectWithConfigsButMissingBuilds() throws Exception {
        final FreeStyleProject project = rule.createFreeStyleProject();
        Thread.sleep(SLEEP_TIME);
        project.setDescription("bla");
        Thread.sleep(SLEEP_TIME);
        project.updateNextBuildNumber(5);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }

    @LocalData
    @Test
    void testBuildWithoutHistoryDir() throws Exception {
        final FreeStyleProject project = (FreeStyleProject) rule.jenkins
                .getItem("Test1");
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }

    @LocalData
    @Test
    void testBuildWithoutHistoryEntries() throws Exception {
        final FreeStyleProject project = (FreeStyleProject) rule.jenkins
                .getItem("Test2");
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
    }

    @PresetData(DataSet.ANONYMOUS_READONLY)
    @Test
    void testBadgeConfigurationAnonymous() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = rule.createFreeStyleProject(jobName);

        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        SecurityContextHolder.getContext().setAuthentication(Jenkins.ANONYMOUS);
        shouldPageContainBadge("anonymous");
    }

    @LocalData
    @Test
    void testBadgeConfigurationWithPermissions() throws Exception {
        final String jobName = "newjob";
        final String description = "a description";
        final FreeStyleProject project = rule.createFreeStyleProject(jobName);

        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);
        project.setDescription(description);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        webClient.login("configUser");
        shouldPageContainBadge("configUser");

        webClient.login("administrator");
        shouldPageContainBadge("admin");
    }

    private void shouldPageContainBadge(String user) throws Exception {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        HtmlPage htmlPage = webClient.goTo("job/newjob");

        // default = always
        assertTrue(htmlPage.asXml().contains("symbol-buildbadge"),
                "Page should contain build badge");

        jch.setShowBuildBadges("never");
        htmlPage = (HtmlPage) htmlPage.refresh();
        assertFalse(htmlPage.asXml().contains("symbol-buildbadge"),
                "Page should not contain build badge");

        jch.setShowBuildBadges("userWithConfigPermission");
        htmlPage = (HtmlPage) htmlPage.refresh();

        if (("configUser").equals(user) || ("admin").equals(user)) {
            assertTrue(htmlPage.asXml().contains("symbol-buildbadge"),
                    "Page should contain build badge");
        } else {
            assertFalse(htmlPage.asXml().contains("symbol-buildbadge"),
                    "Page should not contain build badge");
        }

        jch.setShowBuildBadges("adminUser");
        htmlPage = (HtmlPage) htmlPage.refresh();

        if (("admin").equals(user)) {
            assertTrue(htmlPage.asXml().contains("symbol-buildbadge"),
                    "Page should contain build badge");
        } else {
            assertFalse(htmlPage.asXml().contains("symbol-buildbadge"),
                    "Page should not contain build badge");
        }
    }

    @Test
    void testCorrectShowDiffLinkWithSingleChange() throws Exception {
        final String jobName = "testjob";
        final FreeStyleProject project = rule.createFreeStyleProject(jobName);
        project.setDescription("first description");
        Thread.sleep(SLEEP_TIME);

        final String secondDescription = "second description";
        project.setDescription(secondDescription);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        final String lastDescription = "last description";
        project.setDescription(lastDescription);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        assertTrue(htmlPage.asXml().contains("symbol-buildbadge"),
                "Page should contain build badge");

        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
                .getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        final String page = showDiffPage.asNormalizedText();
        assertTrue(page.contains("Older"),
                "ShowDiffFiles page should be reached now");
        assertTrue(page.contains(secondDescription),
                "ShowDiff page should contain second description");
        assertTrue(page.contains(lastDescription),
                "ShowDiff page should contain last description");
    }

    @Test
    void testCorrectShowDiffLinkWithMultipleChanges() throws Exception {
        final String jobName = "testjob";

        final FreeStyleProject project = rule.createFreeStyleProject(jobName);
        project.setDescription("first description");
        Thread.sleep(SLEEP_TIME);
        final String secondDescription = "second description";
        project.setDescription(secondDescription);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        Thread.sleep(SLEEP_TIME);

        for (int i = 3; i < 6; i++) {
            project.setDescription("description no. " + i);
            Thread.sleep(SLEEP_TIME);
        }
        final String lastDescription = "last description";
        project.setDescription(lastDescription);
        Thread.sleep(SLEEP_TIME);
        rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

        HtmlPage htmlPage = webClient.goTo("job/" + jobName);
        final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
                .getElementById("showDiff");
        final HtmlPage showDiffPage = showDiffLink.click();
        final String page = showDiffPage.asNormalizedText();
        assertTrue(page.contains("Older"),
                "ShowDiffFiles page should be reached now");
        assertTrue(page.contains(secondDescription),
                "ShowDiff page should contain second description");
        assertTrue(page.contains(lastDescription),
                "ShowDiff page should contain last description");
    }
}
