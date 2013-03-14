/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;
import hudson.security.AccessControlled;
import hudson.security.LegacyAuthorizationStrategy;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.jvnet.hudson.test.Bug;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.security.Permission;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.tasks.LogRotator;

/**
 * @author mfriedenhagen
 *
 */
public class JobConfigHistoryBaseActionTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;
    // we need to sleep between saves so we don't overwrite the history directories
    // (which are saved with a granularity of one second)
    private static final int SLEEP_TIME = 1100;

    private final File file1 = new File("old/config.xml");
    private final File file2 = new File("new/config.xml");
    private String oldLineSeparator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
        oldLineSeparator = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.setProperty("line.separator", oldLineSeparator);
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffAsString(File, File, String[], String[])}.
     */
    public void testGetDiffFileStringStringSameLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,2 @@\n 123\n-346\n+3467\n",
                makeResultPlatformIndependent(action.getDiffAsString(file1, file2, s1.split("\n"), s2.split("\n"))));
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringEmpty() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", makeResultPlatformIndependent(action.getDiffAsString(file1, file2, new String[0], new String[0])));
    }

    /**
     * @return
     */
    JobConfigHistoryBaseAction createJobConfigHistoryBaseAction() {
        final JobConfigHistoryBaseAction action = new JobConfigHistoryBaseAction() {
            
            @Override
            protected AccessControlled getAccessControlledObject() {
                return getHudson();
            }
            @Override
            protected void checkConfigurePermission() {
                getAccessControlledObject().checkPermission(Permission.CONFIGURE);
            }
            @Override
            protected boolean hasConfigurePermission() {
                return getAccessControlledObject().hasPermission(Permission.CONFIGURE);
            }
            public String getIconFileName() {
                return null;
            }
        };
        return action;
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffAsString(File, File, String[], String[])}.
     */
    public void testGetDiffFileStringStringDifferentLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();        
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", makeResultPlatformIndependent(action.getDiffAsString(file1, file2, "123\n346".split("\n"), "123\n346\n".split("\n"))));
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,3 @@\n 123\n 346\n+123\n", makeResultPlatformIndependent(action.getDiffAsString(file1, file2, "123\n346".split("\n"), "123\n346\n123".split("\n"))));
    }

    private String makeResultPlatformIndependent(final String result) {
        return result.replace("\\", "/");
    }
    
    public void testGetConfigXmlIllegalArgumentExceptionNonExistingJobName() throws IOException, SAXException {
        TextPage page = (TextPage) webClient.goTo(JobConfigHistoryConsts.URLNAME 
                    + "/configOutput?type=raw&name=bogus&timestamp=2013-01-11_17-26-27", "text/plain");
        assertTrue("Page should be empty.", page.getContent().trim().isEmpty());
    }
    
    public void testGetConfigXmlIllegalArgumentExceptionInvalidTimestamp() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        try {
            action.checkParameters("bla", "bogus");
            fail("Expected " + IllegalArgumentException.class + " because of invalid timestamp.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }
    
    public void testGetConfigXmlIllegalArgumentExceptionDotsInName() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        try {
            final String timestamp = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).format(new GregorianCalendar().getTime());
            action.checkParameters("bla..blubb", timestamp);
            fail("Expected " + IllegalArgumentException.class + " because of '..' in parameter name.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }
    
    @Bug(5534)
    public void testSecuredAccessToJobConfigHistoryPage() throws IOException, SAXException {
        // without security the jobConfigHistory-badge should show.
        final HtmlPage withoutSecurity = webClient.goTo("/");
        assertThat(withoutSecurity.asXml(), containsString(JobConfigHistoryConsts.ICONFILENAME));
        withoutSecurity.getAnchorByHref("/" + JobConfigHistoryConsts.URLNAME);
        // with security enabled the jobConfigHistory-badge should not show anymore.
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(false, false, null));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        final HtmlPage withSecurityEnabled = webClient.goTo("/");
        assertThat(withSecurityEnabled.asXml(), not(containsString(JobConfigHistoryConsts.ICONFILENAME)));
        try {
            withSecurityEnabled.getAnchorByHref("/" + JobConfigHistoryConsts.URLNAME);
            fail("Expected a " + ElementNotFoundException.class + " to be thrown");
        } catch (ElementNotFoundException e) {
            System.err.println(e);
        }
    }
    
    @Bug(17124)
    public void testClearDuplicateLines() throws Exception {
        final String jobName = "Test";
        
        final FreeStyleProject project = createFreeStyleProject(jobName);
        project.setLogRotator(new LogRotator(42, 42, -1, -1));
        project.save();
        Thread.sleep(SLEEP_TIME);
        assertEquals(project.getLogRotator().getDaysToKeep(), 42);
        
        project.setLogRotator(new LogRotator(47, 47, -1, -1));
        project.save();
        Thread.sleep(SLEEP_TIME);
        assertEquals(project.getLogRotator().getDaysToKeep(), 47);

        final HtmlPage historyPage = webClient.goTo("job/" + jobName + "/" + JobConfigHistoryConsts.URLNAME);
        final HtmlForm diffFilesForm = historyPage.getFormByName("diffFiles");
        final HtmlPage diffPage = (HtmlPage) diffFilesForm.submit((HtmlButton) last(diffFilesForm.getHtmlElementsByTagName("button")));
        assertStringContains(diffPage.asText(), "<daysToKeep>42</daysToKeep>");
        assertStringContains(diffPage.asText(), "<numToKeep>42</numToKeep>");
        assertStringContains(diffPage.asText(), "<daysToKeep>47</daysToKeep>");
        assertStringContains(diffPage.asText(), "<numToKeep>47</numToKeep>");
    }
}