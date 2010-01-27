/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

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
        createFreeStyleProject("newjob");
    }

    public void testRename() throws IOException, SAXException {
        final FreeStyleProject project = createFreeStyleProject("newjob");
        project.renameTo("renamedob");
        project.save();
    }
}
