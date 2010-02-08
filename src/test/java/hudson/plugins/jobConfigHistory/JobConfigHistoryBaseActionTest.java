/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import hudson.security.AccessControlled;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.Permission;

import java.io.IOException;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author mfriedenhagen
 *
 */
public class JobConfigHistoryBaseActionTest extends HudsonTestCase {

    private WebClient webClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        webClient = createWebClient();
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getConfigXml(java.lang.String)}
     * .
     *
     * @throws SAXException
     * @throws IOException
     */
    public void testGetConfigXml() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = new JobConfigHistoryBaseAction() {
            @Override
            protected AccessControlled getAccessControlledObject() {
                return hudson;
            }
        };
        try {
            action.getConfigXml(hudson.getRootDir().getAbsolutePath());
            fail("Expected " + IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        }
        try {
            action.getConfigXml("/etc");
            fail("Expected " + IllegalArgumentException.class);
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
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(false));
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        final HtmlPage withSecurityEnabled = webClient.goTo("/");
        assertThat(withSecurityEnabled.asXml(), not(containsString(JobConfigHistoryConsts.ICONFILENAME)));
        try {
            withSecurityEnabled.getAnchorByHref("/" + JobConfigHistoryConsts.URLNAME);
            fail("Expected a " + ElementNotFoundException.class + " to be thrown");
        } catch (ElementNotFoundException e) {
            // Expected
        }
    }

    /**
     * See whether the current user may read configurations.
     */
    protected void checkConfigurePermission() {
        final AccessControlled accessControled = hudson;
        final Permission permission = Permission.CONFIGURE;
        accessControled.checkPermission(permission);
    }

    /**
     * See whether the current user may read configurations.
     * @return true if the current user may read configurations.
     */
    protected boolean hasConfigurePermission() {
        final AccessControlled accessControled = hudson;
        final Permission permission = Permission.CONFIGURE;
        return accessControled.hasPermission(permission);
    }

}
