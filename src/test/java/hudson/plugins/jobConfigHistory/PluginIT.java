/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

/**
 * @author mfriedenhagen
 */
public class PluginIT {

	@Rule
	public JenkinsRule j = new JenkinsRuleWithDeletingInstanceDir();

	private WebClient webClient;
	private static final String JOB_NAME = "bar";

	private class JobPage {

		private final HtmlPage jobPage;
		private final String jobConfigHistoryLink;

		public JobPage() throws IOException, SAXException {
			this.jobPage = webClient.goTo("job/" + JOB_NAME);
			this.jobConfigHistoryLink = getJobConfigHistoryLink();
		}

		public final String getJobConfigHistoryLink() {
			return j.contextPath + "/job/" + JOB_NAME + "/jobConfigHistory";
		}

		public void checkHistoryLink() {
			j.assertXPath(jobPage, "//a[@href=\"" + jobConfigHistoryLink + "\"]");
		}
	}

	private class ConfigPage {

		private final HtmlPage configPage;

		/**
		 * @throws SAXException
		 * @throws IOException
		 */
		public ConfigPage() throws IOException, SAXException {
			this.configPage = webClient.goTo("job/" + JOB_NAME + "/configure");
		}

		public void setNewDescription(final String text) throws Exception {
			final HtmlForm configForm = configPage.getFormByName("config");
			final HtmlTextArea descriptionTextArea = configForm
					.getTextAreaByName("description");
			descriptionTextArea.setText(text);
			j.submit(configForm);
		}
	}

	private class BasicHistoryPage {

		final HtmlPage historyPage;

		public BasicHistoryPage(HtmlPage historyPage)
				throws IOException, SAXException {
			this.historyPage = historyPage;
			assertThat(historyPage.getTitleText(),
					containsString("Configuration History [Jenkins]"));
		}

		public List<HtmlAnchor> getConfigOutputLinks(final String type) {
			return historyPage.getByXPath("//a[contains(@href, \"configOutput?type=" + type + "\")]");
		}
	}

	private class AllJobConfigHistoryPage extends BasicHistoryPage {

		public AllJobConfigHistoryPage(HtmlPage historyPage)
				throws IOException, SAXException {
			super(historyPage);
			Assert.assertEquals("All Configuration History [Jenkins]",
					historyPage.getTitleText());
		}
	}

	private class HistoryPage extends BasicHistoryPage {

		public HistoryPage() throws IOException, SAXException {
			super(webClient.goTo("job/" + JOB_NAME + "/jobConfigHistory"));
			Assert.assertEquals("Job Configuration History [Jenkins]",
					historyPage.getTitleText());
		}

		public HtmlPage getDiffPage() throws Exception {
			final HtmlForm diffFilesForm = historyPage
					.getFormByName("diffFiles");
			return j.submit(diffFilesForm);
		}

		public void setCheckedTimestamp1RadioButton(int index,
				boolean isChecked) {
			getTimestampRadioButton("timestamp1", index).setChecked(isChecked);
		}

		public void setCheckedTimestamp2RadioButton(int index,
				boolean isChecked) {
			getTimestampRadioButton("timestamp2", index).setChecked(isChecked);
		}

		public void assertNoHistoryEntriesAvailable() {
			assertThat(historyPage.asXml(),
					containsString("No job configuration history available"));
		}

		private HtmlRadioButtonInput getTimestampRadioButton(final String name,
				int index) {
			final HtmlForm diffFilesForm = historyPage
					.getFormByName("diffFiles");
			return (HtmlRadioButtonInput) diffFilesForm.getInputsByName(name)
					.get(index);
		}

	}

	@Before
	public void before() {
		webClient = j.createWebClient();
	}

	@Test
	public void testAllProjectsConfigurationHistoryPage()
			throws IOException, SAXException {
		new AllJobConfigHistoryPage(webClient.goTo("jobConfigHistory/"));
	}

	@LocalData
	@Test
	public void testJobPage() throws IOException, SAXException {
		new JobPage().checkHistoryLink();
	}

	@LocalData
	@Test
	public void testHistoryPageWithOutEntries()
			throws IOException, SAXException {
		new HistoryPage().assertNoHistoryEntriesAvailable();
	}

	@LocalData
	@Test
	public void testSaveConfiguration() throws Exception {
		final String firstDescription = "just a test";
		final String secondDescription = "just a second test";
		{
			new ConfigPage().setNewDescription(firstDescription);
			final List<HtmlAnchor> hrefs = new HistoryPage()
					.getConfigOutputLinks("xml");
			Assert.assertTrue(hrefs.size() >= 1);
			final HtmlAnchor xmlAnchor = hrefs.get(0);
			final XmlPage xmlPage = xmlAnchor.click();
			assertThat(xmlPage.asXml(), containsString(firstDescription));
		}
		{
			new ConfigPage().setNewDescription(secondDescription);
			final List<HtmlAnchor> hrefs = new HistoryPage()
					.getConfigOutputLinks("raw");
			Assert.assertTrue(hrefs.size() >= 2);
			final HtmlAnchor rawAnchor = hrefs.get(0);
			final TextPage firstRaw = rawAnchor.click();
			assertThat(firstRaw.getContent(),
					containsString(secondDescription));
		}
		final AllJobConfigHistoryPage allJobConfigHistoryPage = new AllJobConfigHistoryPage(
				webClient.goTo("jobConfigHistory/?filter=jobs"));
		final List<HtmlAnchor> allRawHRefs = allJobConfigHistoryPage
				.getConfigOutputLinks("raw");
		Assert.assertTrue(allRawHRefs.size() >= 2);
		final List<? extends HtmlAnchor> allXmlHRefs = allJobConfigHistoryPage
				.getConfigOutputLinks("xml");
		Assert.assertTrue(allXmlHRefs.size() >= 2);
		final TextPage firstRawOfAll = allRawHRefs.get(0).click();
		assertThat(firstRawOfAll.getContent(),
				containsString(secondDescription));
		final HistoryPage historyPage = new HistoryPage();
		historyPage.setCheckedTimestamp1RadioButton(0, true);
		historyPage.setCheckedTimestamp2RadioButton(1, true);
		final HtmlPage diffPage = historyPage.getDiffPage();
		final String diffPageContent = diffPage.asXml();
		assertThat(diffPageContent,
				containsString("<td class=\"diff_original\">"));
		assertThat(diffPageContent,
				containsString("<td class=\"diff_revised\">"));
		assertThat(diffPageContent, containsString(
				"&lt;description&gt;just a test&lt;/description&gt;"));
		assertThat(diffPageContent, containsString(
				"&lt;description&gt;just a second test&lt;/description&gt;"));
	}

	/**
	 * Checks whether history of a single system configuration is displayed
	 * correctly and contains correct link targets.
	 */
	@Test
	public void testHistoryPageOfSingleSystemConfig() {
		final String firstDescription = "just a test";
		final String secondDescription = "just a second test";
		final int sleepTime = 1100;

		try {
			j.jenkins.setSystemMessage(firstDescription);
			Thread.sleep(sleepTime);
			j.jenkins.setSystemMessage(secondDescription);
			Thread.sleep(sleepTime);
		} catch (Exception ex) {
			Assert.fail("Unable to prepare Jenkins instance: " + ex);
		}

		try {
			final HtmlPage historyPage = webClient.goTo(
					JobConfigHistoryConsts.URLNAME + "/history?name=config");
			final List<HtmlAnchor> allRawHRefs = historyPage.getByXPath(
					"//a[contains(@href, \"configOutput?type=raw\")]");
			Assert.assertTrue(
					"Check that there are at least 2 links for raw output.",
					allRawHRefs.size() >= 2);
			final TextPage firstRawOfAll = allRawHRefs.get(0).click();
			assertThat(
					"Check that the first raw output link leads to the right target.",
					firstRawOfAll.getContent(),
					containsString(secondDescription));
		} catch (Exception ex) {
			Assert.fail(
					"Unable to complete testHistoryPageOfSingleSystemConfig: "
							+ ex);
		}
	}
}
