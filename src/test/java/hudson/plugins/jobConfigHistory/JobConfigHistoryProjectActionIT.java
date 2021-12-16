package hudson.plugins.jobConfigHistory;

import org.junit.Assert;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

public class JobConfigHistoryProjectActionIT {

	@Rule
	public JenkinsRule j = new JenkinsRuleWithDeletingInstanceDir();

	private WebClient webClient;
	// we need to sleep between saves so we don't overwrite the history
	// directories
	// (which are saved with a granularity of one second)
	private static final int SLEEP_TIME = 1100;

	@Before
	public void before() {
		webClient = j.createWebClient();
	}

	/**
	 * Tests restore link on job config history page.
	 */
	@Test
	public void testRestore() {
		final String firstDescription = "first test";
		final String secondDescription = "second test";
		final String projectName = "Test1";

		try {
			final FreeStyleProject project = j.createFreeStyleProject(projectName);
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
			final HtmlPage jobPage = j.submit(restoreForm);

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
	@Test
	public void testRestoreFromDiffFiles() {
		final String firstDescription = "first test";
		final String secondDescription = "second test";
		final String projectName = "Test1";
		final FreeStyleProject project;

		try {
			project = j.createFreeStyleProject(projectName);
			Thread.sleep(SLEEP_TIME);
			project.setDescription(firstDescription);
			Thread.sleep(SLEEP_TIME);
			project.setDescription(secondDescription);
			Thread.sleep(SLEEP_TIME);

			Assert.assertEquals(project.getDescription(), secondDescription);

			final HtmlPage htmlPage = webClient.goTo("job/" + projectName + "/"
					+ JobConfigHistoryConsts.URLNAME);
			final HtmlPage diffPage = j.submit(htmlPage.getFormByName("diffFiles"));
			final HtmlPage reallyRestorePage = j.submit(diffPage.getFormByName("forward"));
			final HtmlPage jobPage = j.submit(reallyRestorePage.getFormByName("restore"));

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
