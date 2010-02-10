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

    public void testGetConfigXmlIllegalArgumentExceptionNoConfigHistory() throws IOException, SAXException {
        // config-history not in diffDir
        testGetConfigXmlIllegalArgumentException(hudson.getRootDir().getAbsolutePath());
    }

    public void testGetConfigXmlIllegalArgumentExceptionNoHudsonHome() throws IOException, SAXException {
        // HUDSON_HOME not in diffDir
        testGetConfigXmlIllegalArgumentException("/etc/config-history/");
    }

    public void testGetConfigXmlIllegalArgumentExceptionHasDoubleDots() throws IOException, SAXException {
        // diffDir has double dots.
        testGetConfigXmlIllegalArgumentException(hudson.getRootDir().getAbsolutePath() + "/../config-history/2010_02_09");
    }

    private void testGetConfigXmlIllegalArgumentException(final String diffDir) {
        final JobConfigHistoryBaseAction action = new JobConfigHistoryBaseAction() {
            @Override
            protected AccessControlled getAccessControlledObject() {
                return getHudson();
            }
        };
        try {
            action.getConfigXml(diffDir);
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
            System.err.println(e);
        }
    }

}
