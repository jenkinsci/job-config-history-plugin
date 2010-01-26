/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.IOException;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

/**
 * @author mfriedenhagen
 */
public class PluginTest extends HudsonTestCase {

    private WebClient webClient;

    private static final String JOB_CONFIG_HISTORY_LINK = "job/bar/jobConfigHistory";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    @LocalData
    public void testAllProjectsConfigurationHistoryPage() throws IOException, SAXException {
        final HtmlPage allProjectsHistory = webClient.goTo("jobConfigHistory/");
        assertEquals("Job Configuration History [Hudson]", allProjectsHistory.getTitleText());
        assertXPath(allProjectsHistory, "//h1[text()=\"All Jobs Configuration History\"]");
    }

    @LocalData
    public void testJobPage() throws IOException, SAXException {
        final HtmlPage jobPage = webClient.goTo("job/bar/");
        assertXPath(jobPage, "//a[@href=\"/" + JOB_CONFIG_HISTORY_LINK + "\"]");
        goToJobConfigurationHistoryPage(jobPage);
    }

    /**
     * @param jobPage
     * @throws IOException
     */
    private HtmlPage goToJobConfigurationHistoryPage(final HtmlPage jobPage) throws IOException {
        final HtmlPage jobConfigHistoryPage = (HtmlPage) jobPage.getAnchorByHref("/" + JOB_CONFIG_HISTORY_LINK).click();
        assertEquals("Job Configuration History [Hudson]", jobConfigHistoryPage.getTitleText());
        return jobConfigHistoryPage;
    }

    @LocalData
    public void testSaveConfiguration() throws Exception {
        final List<? extends HtmlAnchor> hrefs1 = setNewDescriptionAndGetHistoryLinks("just a test", "xml");
        assertEquals(1, hrefs1.size());
        final List<? extends HtmlAnchor> hrefs2 = setNewDescriptionAndGetHistoryLinks("just a second test", "xml");
        assertEquals(2, hrefs2.size());
        final List<? extends HtmlAnchor> hrefs3 = setNewDescriptionAndGetHistoryLinks("just a test", "raw");
        assertEquals(3, hrefs3.size());
        final List<? extends HtmlAnchor> hrefs4 = setNewDescriptionAndGetHistoryLinks("just a second test", "raw");
        assertEquals(4, hrefs4.size());
//        XmlPage page = (XmlPage) hrefs2.get(1).click();
//        System.out.println(page.asText());
//        final FreeStyleProject project = createFreeStyleProject("foo");
//        final HtmlPage fooPage = webClient.getPage(project);
    }

    /**
     * @param text
     * @param type
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws Exception
     */
    private List<? extends HtmlAnchor> setNewDescriptionAndGetHistoryLinks(final String text, final String type)
            throws IOException, SAXException, Exception {
        final HtmlPage configuration = webClient.goTo("job/bar/configure");
        assertXPath(configuration, "//a[@href=\"/" + JOB_CONFIG_HISTORY_LINK + "\"]");
        final HtmlForm configForm = configuration.getFormByName("config");
        final HtmlTextArea descriptionTextArea = configForm.getTextAreaByName("description");
        descriptionTextArea.setText(text);
        final HtmlPage jobPage = submit(configForm);
        final HtmlPage historyPage = goToJobConfigurationHistoryPage(jobPage);
        final List<? extends HtmlAnchor> hrefs = (List<? extends HtmlAnchor>) historyPage.getByXPath("//a[contains(@href, \"configOutput?type=" + type +"\")]");
        return hrefs;
    }

}
