/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author mfriedenhagen
 */
public class PluginTest extends HudsonTestCase {

    private WebClient webClient;
    private static final String JOB_CONFIG_HISTORY_LINK = "/job/bar/jobConfigHistory";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    @LocalData
    public void testAllProjectsConfigurationHistoryPage() throws IOException, SAXException {
        final HtmlPage allProjectsHistory = webClient.goTo("/jobConfigHistory/");
        assertEquals("Job Configuration History [Hudson]", allProjectsHistory.getTitleText());
        assertXPath(allProjectsHistory, "//h1[text()=\"All Jobs Configuration History\"]");
    }

    @LocalData
    public void testJobPage() throws IOException, SAXException {
        final HtmlPage jobPage = webClient.goTo("/job/bar/");
        assertXPath(jobPage, "//a[@href=\"" + JOB_CONFIG_HISTORY_LINK + "\"]");
        goToJobConfigurationHistoryPage(jobPage);
    }

    /**
     * @param jobPage
     * @throws IOException
     */
    private HtmlPage goToJobConfigurationHistoryPage(final HtmlPage jobPage) throws IOException {
        final HtmlPage jobConfigHistoryPage = (HtmlPage) jobPage.getAnchorByHref(JOB_CONFIG_HISTORY_LINK).click();
        assertEquals("Job Configuration History [Hudson]", jobConfigHistoryPage.getTitleText());
        return jobConfigHistoryPage;
    }

    @LocalData
    public void testSaveConfiguration() throws IOException, SAXException {
        final HtmlPage configuration = webClient.goTo("/job/bar/configure");
        assertXPath(configuration, "//a[@href=\"" + JOB_CONFIG_HISTORY_LINK + "\"]");
        final HtmlForm configForm = configuration.getFormByName("config");
        configForm.getTextAreaByName("description").setText("just a test");
//        System.err.println(configForm.asXml());
        final HtmlButton submitButton = (HtmlButton) configuration.getFirstByXPath("//button[@title=\"Click to submit form.\"]");
        final HtmlPage jobPage = (HtmlPage) submitButton.click();
        final HtmlPage jobConfigHistoryPage = goToJobConfigurationHistoryPage(jobPage);
//        System.err.println(jobConfigHistoryPage.asXml());

//        System.err.println(asXml);
    }

}
