package hudson.plugins.jobConfigHistory;

import org.acegisecurity.context.SecurityContextHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.hudson.test.recipes.PresetData;
import org.jvnet.hudson.test.recipes.PresetData.DataSet;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.model.Jenkins;

public class JobConfigBadgeActionIT {

	@Rule
	public JenkinsRule j = new JenkinsRuleWithDeletingInstanceDir();

	private WebClient webClient;
	private static final int SLEEP_TIME = 1100;

	@Before
	public void before() {
		webClient = j.createWebClient();
	}

	@Test
	public void testBadgeAction() throws Exception {
		final String jobName = "newjob";
		final String description = "a description";
		final FreeStyleProject project = j.createFreeStyleProject(jobName);

		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		HtmlPage htmlPage = webClient.goTo("job/" + jobName);
		Assert.assertFalse("Page should not contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		htmlPage = (HtmlPage) htmlPage.refresh();
		Assert.assertFalse("Page should still not contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		project.setDescription(description);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

		htmlPage = (HtmlPage) htmlPage.refresh();
		Assert.assertTrue("Page should contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));
	}

	@Test
	public void testBadgeAfterRename() throws Exception {
		final String oldName = "firstjobname";
		final String newName = "secondjobname";

		final FreeStyleProject project = j.createFreeStyleProject(oldName);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);

		project.renameTo(newName);
		Thread.sleep(SLEEP_TIME);
		project.scheduleBuild2(0).get();

		final HtmlPage htmlPage = webClient.goTo("job/" + newName);
		Assert.assertTrue("Page should contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
				.getElementById("showDiff");
		final HtmlPage showDiffPage = showDiffLink.click();
		Assert.assertTrue("ShowDiffFiles page should be reached now",
				showDiffPage.asText().contains("No lines changed"));
	}

	@Test
	public void testCorrectLinkTargetsAfterRename() throws Exception {
		final String oldName = "jobname1";
		final String newName = "jobname2";
		final String oldDescription = "first description";
		final String newDescription = "second description";

		final FreeStyleProject project = j.createFreeStyleProject(oldName);
		project.setDescription(oldDescription);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);

		project.setDescription(newDescription);
		Thread.sleep(SLEEP_TIME);
		project.scheduleBuild2(0).get();

		final HtmlPage htmlPage = webClient.goTo("job/" + oldName);
		final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
				.getElementById("showDiff");
		final HtmlPage showDiffPage = showDiffLink.click();
		Assert.assertTrue("ShowDiffFiles page should be reached now",
				showDiffPage.asText().contains("Older"));

		project.renameTo(newName);
		Thread.sleep(SLEEP_TIME);
		project.scheduleBuild2(0).get();

		// Test whether build badge link that was created before rename still
		// leads to correct page
		final HtmlPage htmlPage2 = webClient.goTo("job/" + newName);
		final HtmlAnchor oldShowDiffLink = (HtmlAnchor) htmlPage2
				.getByXPath("//a[@id='showDiff']").get(1);
		final HtmlPage showDiffPage2 = oldShowDiffLink.click();
		Assert.assertTrue("ShowDiffFiles page should be reached now",
				showDiffPage2.asText().contains("Older"));
	}

	@Test
	public void testProjectWithConfigsButMissingBuilds() throws Exception {
		final FreeStyleProject project = j.createFreeStyleProject();
		Thread.sleep(SLEEP_TIME);
		project.setDescription("bla");
		Thread.sleep(SLEEP_TIME);
		project.updateNextBuildNumber(5);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
	}

	@LocalData
	@Test
	public void testBuildWithoutHistoryDir() throws Exception {
		final FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("Test1");
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
	}

	@LocalData
	@Test
	public void testBuildWithoutHistoryEntries() throws Exception {
		final FreeStyleProject project = (FreeStyleProject) j.jenkins.getItem("Test2");
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
	}

	@PresetData(DataSet.ANONYMOUS_READONLY)
	@Test
	public void testBadgeConfigurationAnonymous() throws Exception {
		final String jobName = "newjob";
		final String description = "a description";
		final FreeStyleProject project = j.createFreeStyleProject(jobName);

		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);
		project.setDescription(description);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

		j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
		SecurityContextHolder.getContext().setAuthentication(Jenkins.ANONYMOUS);
		shouldPageContainBadge("anonymous");
	}

	@LocalData
	@Test
	public void testBadgeConfigurationWithPermissions() throws Exception {
		final String jobName = "newjob";
		final String description = "a description";
		final FreeStyleProject project = j.createFreeStyleProject(jobName);

		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);
		project.setDescription(description);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

		j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
		webClient.login("configUser");
		shouldPageContainBadge("configUser");

		webClient.login("administrator");
		shouldPageContainBadge("admin");
	}

	private void shouldPageContainBadge(String user) throws Exception {
		final JobConfigHistory jch = j.jenkins.getPlugin(JobConfigHistory.class);
		HtmlPage htmlPage = webClient.goTo("job/newjob");

		// default = always
		Assert.assertTrue("Page should contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		jch.setShowBuildBadges("never");
		htmlPage = (HtmlPage) htmlPage.refresh();
		Assert.assertFalse("Page should not contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		jch.setShowBuildBadges("userWithConfigPermission");
		htmlPage = (HtmlPage) htmlPage.refresh();

		if (("configUser").equals(user) || ("admin").equals(user)) {
			Assert.assertTrue("Page should contain build badge",
					htmlPage.asXml().contains("buildbadge.png"));
		} else {
			Assert.assertFalse("Page should not contain build badge",
					htmlPage.asXml().contains("buildbadge.png"));
		}

		jch.setShowBuildBadges("adminUser");
		htmlPage = (HtmlPage) htmlPage.refresh();

		if (("admin").equals(user)) {
			Assert.assertTrue("Page should contain build badge",
					htmlPage.asXml().contains("buildbadge.png"));
		} else {
			Assert.assertFalse("Page should not contain build badge",
					htmlPage.asXml().contains("buildbadge.png"));
		}
	}

	@Test
	public void testCorrectShowDiffLinkWithSingleChange() throws Exception {
		final String jobName = "testjob";
		final FreeStyleProject project = j.createFreeStyleProject(jobName);
		project.setDescription("first description");
		Thread.sleep(SLEEP_TIME);

		final String secondDescription = "second description";
		project.setDescription(secondDescription);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);

		final String lastDescription = "last description";
		project.setDescription(lastDescription);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

		HtmlPage htmlPage = webClient.goTo("job/" + jobName);
		Assert.assertTrue("Page should contain build badge",
				htmlPage.asXml().contains("buildbadge.png"));

		final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage.getElementById("showDiff");
		final HtmlPage showDiffPage = showDiffLink.click();
		final String page = showDiffPage.asText();
		Assert.assertTrue("ShowDiffFiles page should be reached now",
				page.contains("Older"));
		Assert.assertTrue("ShowDiff page should contain second description",
				page.contains(secondDescription));
		Assert.assertTrue("ShowDiff page should contain last description",
				page.contains(lastDescription));
	}

	@Test
	public void testCorrectShowDiffLinkWithMultipleChanges() throws Exception {
		final String jobName = "testjob";

		final FreeStyleProject project = j.createFreeStyleProject(jobName);
		project.setDescription("first description");
		Thread.sleep(SLEEP_TIME);
		final String secondDescription = "second description";
		project.setDescription(secondDescription);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		Thread.sleep(SLEEP_TIME);

		for (int i = 3; i < 6; i++) {
			project.setDescription("description no. " + i);
			Thread.sleep(SLEEP_TIME);
		}
		final String lastDescription = "last description";
		project.setDescription(lastDescription);
		Thread.sleep(SLEEP_TIME);
		j.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

		HtmlPage htmlPage = webClient.goTo("job/" + jobName);
		final HtmlAnchor showDiffLink = (HtmlAnchor) htmlPage
				.getElementById("showDiff");
		final HtmlPage showDiffPage = showDiffLink.click();
		final String page = showDiffPage.asText();
		Assert.assertTrue("ShowDiffFiles page should be reached now",
				page.contains("Older"));
		Assert.assertTrue("ShowDiff page should contain second description",
				page.contains(secondDescription));
		Assert.assertTrue("ShowDiff page should contain last description",
				page.contains(lastDescription));
	}
}
