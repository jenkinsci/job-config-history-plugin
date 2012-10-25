package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigHistoryRootActionTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;
    // we need to sleep between saves so we don't overwrite the history directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }
    
    public void testFilterWithoutData() {
        try {
            final HtmlPage htmlPage = webClient.goTo("jobConfigHistory");
            assertTrue(htmlPage.asText().contains("No configuration history"));
        } catch (Exception ex) {
            fail("Unable to complete testFilterWithoutData: " + ex);
        }
    }
            
    public void testFilterWithData() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        jch.setSaveSystemConfiguration(true);

        //create some config history data
        try {
            final FreeStyleProject project = createFreeStyleProject("Test1");
            Thread.sleep(SLEEP_TIME);
            project.disable();
            Thread.sleep(SLEEP_TIME);
            
            hudson.setSystemMessage("First Testmessage");
            Thread.sleep(SLEEP_TIME);
            
            final FreeStyleProject secondProject = createFreeStyleProject("Test2");
            Thread.sleep(SLEEP_TIME);
            secondProject.delete();
        } catch (Exception ex) {
            fail("Unable to prepare Hudson instance: " + ex);
        }            

        try {
            checkSystemPage(webClient.goTo(JobConfigHistoryConsts.URLNAME));
            checkSystemPage(webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=system"));
                
            final HtmlPage htmlPageJobs = webClient.goTo(JobConfigHistoryConsts.URLNAME + "/?filter=jobs");
            assertTrue("Verify history entry for job is listed.", htmlPageJobs.getAnchorByText("Test1") != null);
            assertTrue("Verify history entry for deleted job is listed.", htmlPageJobs.asText().contains(JobConfigHistoryConsts.DELETED_MARKER));
            assertFalse("Verify that no history entry for system change is listed.", htmlPageJobs.asText().contains("config (system)"));
            assertTrue("Check link to job page.", htmlPageJobs.asXml().contains("job/Test1/" + JobConfigHistoryConsts.URLNAME));

            final HtmlPage htmlPageDeleted = webClient.goTo("jobConfigHistory/?filter=deleted");
            final String page = htmlPageDeleted.asXml();
            assertTrue("Verify history entry for deleted job is listed.", page.contains(JobConfigHistoryConsts.DELETED_MARKER));
            assertFalse("Verify no history entry for job is listed.", page.contains("Test1"));
            assertFalse("Verify no history entry for system change is listed.", page.contains("(system)"));
            assertTrue("Check link to historypage exists.", page.contains("history?name"));
            assertFalse("Verify that only \'Deleted\' entries are listed.", page.contains("Created") || page.contains("Changed"));
        } catch (Exception ex) {
            fail("Unable to complete testFilterWithData: " + ex);
        }
    }

    private void checkSystemPage(HtmlPage htmlPage){
        final String page = htmlPage.asXml();
        assertTrue("Verify history entry for system change is listed.", htmlPage.getAnchorByText("config") != null);
        assertFalse("Verify no job history entry is listed.", page.contains("Test1"));
        assertFalse("Verify history entry for deleted job is listed.", page.contains(JobConfigHistoryConsts.DELETED_MARKER));
        assertTrue("Check link to historypage exists.", page.contains("history?name"));
    }

    public void testFilterWithoutPermissions(){
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(false, false, null));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        try {
            final HtmlPage htmlPage = webClient.goTo(JobConfigHistoryConsts.URLNAME);
            assertTrue("Verify nothing is shown without permission", htmlPage.asText().contains("No permission to view"));
        } catch (Exception ex) {
            fail("Unable to complete testFilterWithoutPermissions: " + ex);
        }
    }
}
