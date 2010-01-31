/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

/**
 * @author mfriedenhagen
 *
 */
public class JobConfigHistoryBaseActionTest extends HudsonTestCase {

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryBaseAction#getConfigXml(java.lang.String)}.
     *
     * @throws SAXException
     * @throws IOException
     */
    public void testGetConfigXml() throws IOException, SAXException {
        final JobConfigHistoryBaseAction action = new JobConfigHistoryBaseAction() {
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

}
