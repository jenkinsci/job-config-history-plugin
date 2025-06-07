/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import org.htmlunit.TextPage;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlRadioButtonInput;
import org.htmlunit.html.HtmlTextArea;
import org.htmlunit.xml.XmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mfriedenhagen
 */
@WithJenkins
class PluginIT extends AbstractHudsonTestCaseDeletingInstanceDir {

    private static final String JOB_NAME = "bar";
    private JenkinsRule.WebClient webClient;

    @Override
    void setUp(JenkinsRule rule) throws Exception {
        super.setUp(rule);
        webClient = rule.createWebClient();
    }

    @Test
    void testAllProjectsConfigurationHistoryPage()
            throws IOException, SAXException {
        new AllJobConfigHistoryPage(webClient.goTo("jobConfigHistory/"));
    }

    @LocalData
    @Test
    void testJobPage() throws IOException, SAXException {
        new JobPage().checkHistoryLink();
    }

    @LocalData
    @Test
    void testHistoryPageWithOutEntries()
            throws IOException, SAXException {
        new HistoryPage().assertNoHistoryEntriesAvailable();
    }

    @LocalData
    @Test
    void testSaveConfiguration() throws Exception {
        final String firstDescription = "just a test";
        final String secondDescription = "just a second test";
        {
            new ConfigPage().setNewDescription(firstDescription);
            final List<HtmlAnchor> hrefs = new HistoryPage()
                    .getConfigOutputLinks("xml");
            assertFalse(hrefs.isEmpty());
            final HtmlAnchor xmlAnchor = hrefs.get(0);
            final XmlPage xmlPage = xmlAnchor.click();
            assertThat(xmlPage.asXml(), containsString(firstDescription));
        }
        {
            new ConfigPage().setNewDescription(secondDescription);
            final List<HtmlAnchor> hrefs = new HistoryPage()
                    .getConfigOutputLinks("raw");
            assertTrue(hrefs.size() >= 2);
            final HtmlAnchor rawAnchor = hrefs.get(0);
            final TextPage firstRaw = rawAnchor.click();
            assertThat(firstRaw.getContent(),
                    containsString(secondDescription));
        }
        final AllJobConfigHistoryPage allJobConfigHistoryPage = new AllJobConfigHistoryPage(
                webClient.goTo("jobConfigHistory/?filter=jobs"));
        final List<HtmlAnchor> allRawHRefs = allJobConfigHistoryPage
                .getConfigOutputLinks("raw");
        assertTrue(allRawHRefs.size() >= 2);
        final List<? extends HtmlAnchor> allXmlHRefs = allJobConfigHistoryPage
                .getConfigOutputLinks("xml");
        assertTrue(allXmlHRefs.size() >= 2);
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
    void testHistoryPageOfSingleSystemConfig() {
        final String firstDescription = "just a test";
        final String secondDescription = "just a second test";
        final int sleepTime = 1100;

        assertDoesNotThrow(() -> {
            rule.jenkins.setSystemMessage(firstDescription);
            Thread.sleep(sleepTime);
            rule.jenkins.setSystemMessage(secondDescription);
            Thread.sleep(sleepTime);
        }, "Unable to prepare Jenkins instance: ");

        assertDoesNotThrow(() -> {
            final HtmlPage historyPage = webClient.goTo(
                    JobConfigHistoryConsts.URLNAME + "/history?name=config");
            final List<HtmlAnchor> allRawHRefs = historyPage.getByXPath(
                    "//a[contains(@href, \"configOutput?type=raw\")]");
            assertTrue(
                    allRawHRefs.size() >= 2,
                    "Check that there are at least 2 links for raw output.");
            final TextPage firstRawOfAll = allRawHRefs.get(0).click();
            assertThat(
                    "Check that the first raw output link leads to the right target.",
                    firstRawOfAll.getContent(),
                    containsString(secondDescription));
        }, "Unable to complete testHistoryPageOfSingleSystemConfig: ");
    }

    private static class BasicHistoryPage {

        final HtmlPage historyPage;

        public BasicHistoryPage(HtmlPage historyPage) {
            this.historyPage = historyPage;
            assertThat(historyPage.getTitleText(),
                    containsString("Configuration History [Jenkins]"));
        }

        public List<HtmlAnchor> getConfigOutputLinks(final String type) {
            return historyPage.getByXPath("//a[contains(@href, \"configOutput?type=" + type + "\")]");
        }

    }

    private static class AllJobConfigHistoryPage extends BasicHistoryPage {

        public AllJobConfigHistoryPage(HtmlPage historyPage) {
            super(historyPage);
            assertEquals("All Configuration History [Jenkins]",
                    historyPage.getTitleText());
        }

    }

    private class JobPage {

        private final HtmlPage jobPage;
        private final String jobConfigHistoryLink;

        public JobPage() throws IOException, SAXException {
            this.jobPage = webClient.goTo("job/" + JOB_NAME);
            this.jobConfigHistoryLink = getJobConfigHistoryLink();
        }

        public final String getJobConfigHistoryLink() {
            return "job/" + JOB_NAME + "/jobConfigHistory";
        }

        public void checkHistoryLink() {
            rule.assertXPath(jobPage,
                    "//a[@href=\"/" + jobConfigHistoryLink + "\"]");
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
            rule.submit(configForm);
        }
    }

    private class HistoryPage extends BasicHistoryPage {

        public HistoryPage() throws IOException, SAXException {
            super(webClient.goTo("job/" + JOB_NAME + "/jobConfigHistory"));
            assertEquals("Job Configuration History [Jenkins]",
                    historyPage.getTitleText());
        }

        public HtmlPage getDiffPage() throws IOException {
            final HtmlForm diffFilesForm = historyPage
                    .getFormByName("diffFiles");
            return rule.last(diffFilesForm.getElementsByTagName("button")).click();
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
}
