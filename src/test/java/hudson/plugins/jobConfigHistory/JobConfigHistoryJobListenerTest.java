/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author mirko
 *
 */
public class JobConfigHistoryJobListenerTest extends HudsonTestCase {
    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    public void testCreation() throws IOException, SAXException {
        System.out.println(hudson.root);
        final FreeStyleProject project = createFreeStyleProject("newjob");
        project.save();
        HtmlPage htmlPage = webClient.getPage(project);
        System.out.println(htmlPage.asXml());
    }

    public void itestRename() throws IOException, SAXException {
        final FreeStyleProject project = createFreeStyleProject("newjob");
        project.save();
        webClient.goTo("job/newjob/jobConfigHistory");
        project.renameTo("renamedob");
        project.save();
        webClient.goTo("job/renamedjob/jobConfigHistory");
    }
}
