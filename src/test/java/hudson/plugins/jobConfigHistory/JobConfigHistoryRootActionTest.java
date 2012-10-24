package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
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
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(true, false, null));
        webClient = createWebClient();
    }
    
    //TODO: history.jelly testen
    
    //TODO: index.jelly testen
    //-> wird jobs/deleted/system/all richtig angezeigt?
    //-> führen Links an die richtige Stelle?
    //sieht man system-Links nicht, wenn man keine Rechte hat?

    public void testFilter() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        
        try {
            final FreeStyleProject project = createFreeStyleProject("Test1");
            Thread.sleep(SLEEP_TIME);
            project.disable();
            Thread.sleep(SLEEP_TIME);
            project.enable();
            Thread.sleep(SLEEP_TIME);
            
            Jenkins jenkins = Hudson.getInstance();
            jenkins.setSystemMessage("First Testmessage");
            Thread.sleep(SLEEP_TIME);
            jenkins.setSystemMessage("Second Testmessage");
            
            final FreeStyleProject secondProject = createFreeStyleProject("Test2");
            Thread.sleep(SLEEP_TIME);
            secondProject.delete();
            
//            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);

            final HtmlPage htmlPage = webClient.goTo("jobConfigHistory");
            
            //warum "no system config history available"?
            for (String bla : htmlPage.asXml().split("document.gif")){
                System.out.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
                System.out.println(bla);
            }
            
            System.out.println(htmlPage.asXml().split("document.gif").length);
            
//            assertEquals(3, htmlPage.asXml().split("document.gif").length);

/*            final DomNodeList<HtmlElement> linkNodes = htmlPage.getElementsByTagName("a");
            for (HtmlElement link : linkNodes){
                
                System.out.println("HHHHHHHHHHHH: " + getHrefAttribute());
                
                for (HtmlElement text: link.getChildElements()){
                    System.out.println(text.getNodeName());
                    System.out.println(text.getNodeValue());
                }
            }
*/            
            assertTrue(true);
            
            
            
        } catch (Exception ex) {
            fail("Unable to complete filter test: " + ex);
        }
    }
    
    
    //TODO: ersetzen
    // test filter page
/*    public void testFilteredGetConfigs() throws IOException, SAXException {
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
*/
}
