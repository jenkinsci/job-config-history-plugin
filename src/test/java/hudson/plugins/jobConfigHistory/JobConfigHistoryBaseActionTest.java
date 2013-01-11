/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;
import hudson.security.AccessControlled;
import hudson.security.LegacyAuthorizationStrategy;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.jvnet.hudson.test.Bug;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.security.Permission;
import hudson.security.HudsonPrivateSecurityRealm;

/**
 * @author mfriedenhagen
 *
 */
public class JobConfigHistoryBaseActionTest extends AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;

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
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiff(File, File, String[], String[])}.
     */
    public void testGetDiffFileStringStringSameLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,2 @@\n 123\n-346\n+3467\n",
                makeResultPlatformIndependent(action.getDiff(file1, file2, s1.split("\n"), s2.split("\n"))));
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringEmpty() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", makeResultPlatformIndependent(action.getDiff(file1, file2, new String[0], new String[0])));
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
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getDiff(File, File, String[], String[])}.
     */
    public void testGetDiffFileStringStringDifferentLineLength() {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();        
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", makeResultPlatformIndependent(action.getDiff(file1, file2, "123\n346".split("\n"), "123\n346\n".split("\n"))));
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,3 @@\n 123\n 346\n+123\n", makeResultPlatformIndependent(action.getDiff(file1, file2, "123\n346".split("\n"), "123\n346\n123".split("\n"))));
    }

    private String makeResultPlatformIndependent(final String result) {
        return result.replace("\\", "/");
    }
    
    public void testGetConfigXmlIllegalArgumentExceptionNonExistingJobName() throws IOException, SAXException {
        final String name = "jobName";
        createFreeStyleProject(name);
        
        HtmlPage page = webClient.goTo(JobConfigHistoryConsts.URLNAME 
                    + "/configOutput?type=xml&isJob=true&name=bogus&timestamp=2013-01-11_17-26-27");
        assertTrue("Page should contain XML Parsing Error.", page.asText().contains("XML Parsing Error"));
    }

    public void testGetConfigXmlIllegalArgumentExceptionInvalidTimestamp() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        try {
            action.getConfigXml("bla", "bogus", false);
            fail("Expected " + IllegalArgumentException.class + " because of invalid timestamp.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }
    
    public void testGetConfigXmlIllegalArgumentExceptionDotsInName() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = createJobConfigHistoryBaseAction();
        try {
            final String timestamp = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).format(new GregorianCalendar().getTime());
            action.getConfigXml("bla..blubb", timestamp, false);
            fail("Expected " + IllegalArgumentException.class + " because of '..' in parameter name.");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
    }

    public void testGetConfigXmlIllegalArgumentExceptionNonExistentDirectory() throws IOException, SAXException {
        // request for non-history directory
        final File baseDir = new File(hudson.getRootDir(), "jobConfigHistory");
        TextPage page = (TextPage) webClient.goTo("jobConfigHistory/configOutput?type=raw&file=" + URLEncoder.encode(baseDir.getPath(), "UTF-8"), "text/plain");
        assertTrue("Verify empty return on non-history directory request.", page.getContent().trim().isEmpty());

        // request for non-existent directory
        final File invalidDir = new File(baseDir, "no_such_dir");
        page = (TextPage) webClient.goTo("jobConfigHistory/configOutput?type=raw&file=" + URLEncoder.encode(invalidDir.getPath(), "UTF-8"), "text/plain");
        assertTrue("Verify empty return on non-existent directory request.", page.getContent().trim().isEmpty());
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
}