/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.javascript.host.Event;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

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
        final HtmlPage configuration1 = webClient.goTo("job/bar/configure");
        assertXPath(configuration1, "//a[@href=\"/" + JOB_CONFIG_HISTORY_LINK + "\"]");
        final HtmlForm configForm1 = configuration1.getFormByName("config");
        final HtmlTextArea descriptionTextArea1 = configForm1.getTextAreaByName("description");
        descriptionTextArea1.setText("just a test");
        final HtmlPage jobPage = submit(configForm1);
        final HtmlPage historyPage = goToJobConfigurationHistoryPage(jobPage);
        System.out.println(historyPage.asXml());
//        final FreeStyleProject project = createFreeStyleProject("foo");
//        final HtmlPage fooPage = webClient.getPage(project);
    }

}
