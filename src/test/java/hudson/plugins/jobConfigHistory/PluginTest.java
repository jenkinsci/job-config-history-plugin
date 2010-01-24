/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author mfriedenhagen
 */
public class PluginTest extends HudsonTestCase {

    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    @LocalData
    public void testJobPage() throws IOException, SAXException {
        final String jobConfigHistoryLink = "/job/bar/jobConfigHistory";
        final HtmlPage jobPage = webClient.goTo("/job/bar/");
        assertXPath(jobPage, "//a[@href=\"" + jobConfigHistoryLink + "\"]");
        final HtmlPage jobConfigHistoryPage = (HtmlPage) jobPage.getAnchorByHref(jobConfigHistoryLink).click();
        assertEquals("Job Configuration History [Hudson]", jobConfigHistoryPage.getTitleText());
    }

    @LocalData
    public void testSaveConfiguration() throws IOException, SAXException {
        final HtmlPage configuration = webClient.goTo("/job/bar/configure");
        final String asXml = configuration.asXml();
//        assertXPath(configuration, "//a[@href=\"/job/bar/jobConfigHistory\"]");
//        System.err.println(asXml);
    }

}
