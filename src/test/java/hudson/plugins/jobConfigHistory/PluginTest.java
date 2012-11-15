/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.IOException;
import java.util.List;

import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

/**
 * @author mfriedenhagen
 */
public class PluginTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private class JobPage {

        private final HtmlPage jobPage;
        private final String jobConfigHistoryLink;

        public JobPage() throws IOException, SAXException {
            this.jobPage = webClient.goTo("job/" + JOB_NAME);
            this.jobConfigHistoryLink = getJobConfigHistoryLink();
        }

        /**
         * @return
         */
        public final String getJobConfigHistoryLink() {
            return "job/" + JOB_NAME + "/jobConfigHistory";
        }

        public void checkHistoryLink() {
            assertXPath(jobPage, "//a[@href=\"/" + jobConfigHistoryLink + "\"]");
        }
    }

    private class ConfigPage {

        private final HtmlPage configPage;

        /**
         * @param configPage
         * @throws SAXException
         * @throws IOException
         */
        public ConfigPage() throws IOException, SAXException {
            this.configPage = webClient.goTo("job/" + JOB_NAME + "/configure");
        }

        public void setNewDescription(final String text) throws Exception {
            final HtmlForm configForm = configPage.getFormByName("config");
            final HtmlTextArea descriptionTextArea = configForm.getTextAreaByName("description");
            descriptionTextArea.setText(text);
            submit(configForm);
        }
    }

    private class BasicHistoryPage {

        final HtmlPage historyPage;

        public BasicHistoryPage(HtmlPage historyPage) throws IOException, SAXException {
            this.historyPage = historyPage;
            assertThat(historyPage.getTitleText(), containsString("Configuration History [Jenkins]"));
        }

        public List<HtmlAnchor> getConfigOutputLinks(final String type) {
            final List<HtmlAnchor> hrefs = historyPage
                    .getByXPath("//a[contains(@href, \"configOutput?type=" + type + "\")]");
            return hrefs;
        }

    }

    private class AllJobConfigHistoryPage extends BasicHistoryPage {

        public AllJobConfigHistoryPage(HtmlPage historyPage) throws IOException, SAXException {
            super(historyPage);
            assertEquals("All Configuration History [Jenkins]", historyPage.getTitleText());
        }

    }

    private class HistoryPage extends BasicHistoryPage {

        public HistoryPage() throws IOException, SAXException {
            super(webClient.goTo("job/" + JOB_NAME + "/jobConfigHistory"));
            assertEquals("Job Configuration History [Jenkins]", historyPage.getTitleText());
        }

        public HtmlPage getDiffPage() throws IOException {
            final HtmlForm diffFilesForm = historyPage.getFormByName("diffFiles");
            return  (HtmlPage) diffFilesForm.submit((HtmlButton) last(diffFilesForm.getHtmlElementsByTagName("button")));
        }

        public void setCheckedHistDir1RadioButton(int index, boolean isChecked) {
            getHistDirRadioButton("histDir1", index).setChecked(isChecked);
        }

        public void setCheckedHistDir2RadioButton(int index, boolean isChecked) {
            getHistDirRadioButton("histDir2", index).setChecked(isChecked);
        }

        public void assertNoHistoryEntriesAvailable() {
            assertThat(historyPage.asXml(), containsString("No job configuration history available"));
        }

        private HtmlRadioButtonInput getHistDirRadioButton(final String name, int index) {
            final HtmlForm diffFilesForm = historyPage.getFormByName("diffFiles");
            return (HtmlRadioButtonInput) diffFilesForm.getInputsByName(name).get(index);
        }

    }

    private WebClient webClient;

    private static final String JOB_NAME = "bar";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    public void testAllProjectsConfigurationHistoryPage() throws IOException, SAXException {
        new AllJobConfigHistoryPage(webClient.goTo("jobConfigHistory/"));
    }

    @LocalData
    public void testJobPage() throws IOException, SAXException {
        new JobPage().checkHistoryLink();
    }

    @LocalData
    public void testHistoryPageWithOutEntries() throws IOException, SAXException {
        new HistoryPage().assertNoHistoryEntriesAvailable();
    }

    @LocalData
    public void testSaveConfiguration() throws Exception {
        final String firstDescription = "just a test";
        final String secondDescription = "just a second test";
        {
            new ConfigPage().setNewDescription(firstDescription);
            final List<HtmlAnchor> hrefs = new HistoryPage().getConfigOutputLinks("xml");
            assertTrue(hrefs.size() >= 1);
            final HtmlAnchor xmlAnchor = hrefs.get(0);
            final XmlPage xmlPage = (XmlPage) xmlAnchor.click();
            assertThat(xmlPage.asXml(), containsString(firstDescription));
        }
        {
            new ConfigPage().setNewDescription(secondDescription);
            final List<HtmlAnchor> hrefs = new HistoryPage().getConfigOutputLinks("raw");
            assertTrue(hrefs.size() >= 2);
            final HtmlAnchor rawAnchor = hrefs.get(0);
            final TextPage firstRaw = (TextPage) rawAnchor.click();
            assertThat(firstRaw.getContent(), containsString(secondDescription));
        }
        final AllJobConfigHistoryPage allJobConfigHistoryPage = new AllJobConfigHistoryPage(webClient.goTo("jobConfigHistory/?filter=jobs"));
        List<HtmlAnchor> allRawHRefs = allJobConfigHistoryPage.getConfigOutputLinks("raw");
        assertTrue(allRawHRefs.size() >= 2);
        List<? extends HtmlAnchor> allXmlHRefs = allJobConfigHistoryPage.getConfigOutputLinks("xml");
        assertTrue(allXmlHRefs.size() >= 2);
        final TextPage firstRawOfAll = (TextPage) allRawHRefs.get(0).click();
        assertThat(firstRawOfAll.getContent(), containsString(secondDescription));
        final HistoryPage historyPage = new HistoryPage();
        historyPage.setCheckedHistDir1RadioButton(0, true);
        historyPage.setCheckedHistDir2RadioButton(1, true);
        final HtmlPage diffPage = historyPage.getDiffPage();
        final String diffPageContent = diffPage.asXml();
        assertThat(diffPageContent, containsString("<td class=\"diff_original\">"));
        assertThat(diffPageContent, containsString("<td class=\"diff_revised\">"));
        assertThat(diffPageContent, containsString("&lt;description&gt;just a test&lt;/description&gt;"));
        assertThat(diffPageContent, containsString("&lt;description&gt;just a second test&lt;/description&gt;"));
    }
}
