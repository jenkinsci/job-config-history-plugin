package hudson.plugins.jobConfigHistory;

import hudson.security.HudsonPrivateSecurityRealm;

import java.io.IOException;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JobConfigHistoryRootActionTest extends HudsonTestCase {

    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(true));
        webClient = createWebClient(); 
    }

    // test filter page
    public void testFilteredGetConfigs() throws IOException, SAXException {
        try {
            HtmlForm form = webClient.goTo("configure").getFormByName("config");
            form.getInputByName("saveSystemConfiguration").setChecked(true);
            submit(form);
        } catch (Exception e) {
            fail("unable to configure save system history" +e);
        }
        // get a history entry for this plugin
        JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        jch.save();

        HtmlPage page = webClient.goTo("jobConfigHistory/");
        assertTrue("Verify jobConfigHistory link shows on unfiltered page.", page.getAnchorByText("jobConfigHistory") != null);

        page = webClient.goTo("jobConfigHistory/?filter=jobConfigHistory");
        assertTrue("Verify jobConfigHistory link shows on filtered page.", page.getAnchorByText("jobConfigHistory") != null);

        page = webClient.goTo("jobConfigHistory/?filter=nosuchobject");
        assertTrue("Verify no history message shown with invalid filter.", page.asText().contains("No job configuration history available"));
    }
}
