package hudson.plugins.jobConfigHistory;

import hudson.model.Hudson;
import hudson.security.AccessControlled;
import hudson.util.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryBaseActionTest {

    private final Hudson hudsonMock = mock(Hudson.class);
    private final StaplerRequest staplerRequestMock = mock(StaplerRequest.class);

    /**
     * Test of getDisplayName method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetDisplayName() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "Job Config History";
        String result = sut.getDisplayName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUrlName method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetUrlName() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "jobConfigHistory";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOutputType method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetOutputTypeXml() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        when(staplerRequestMock.getParameter("type")).thenReturn("xml");
        String expResult = "xml";
        String result = sut.getOutputType();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOutputType method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetOutputTypeOther() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        when(staplerRequestMock.getParameter("type")).thenReturn("does not matter");
        String expResult = "plain";
        String result = sut.getOutputType();
        assertEquals(expResult, result);
    }
    /**
     * Test of checkTimestamp method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testCheckTimestamp() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        assertFalse(sut.checkTimestamp("null"));
        assertFalse(sut.checkTimestamp(null));
        assertTrue(sut.checkTimestamp("2013-08-31_23-59-59"));
    }

    /**
     * Test of getRequestParameter method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetRequestParameter() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        final String parameterName = "type";
        when(staplerRequestMock.getParameter(parameterName)).thenReturn("xml");
        String expResult = "xml";
        String result = sut.getRequestParameter(parameterName);
        assertEquals(expResult, result);
    }

    /**
     * Test of checkConfigurePermission method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testCheckConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        sut.checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testHasConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        boolean expResult = false;
        boolean result = sut.hasConfigurePermission();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHudson method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetHudson() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        Hudson expResult = hudsonMock;
        Hudson result = sut.getHudson();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAccessControlledObject method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetAccessControlledObject() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        AccessControlled expResult = null;
        AccessControlled result = sut.getAccessControlledObject();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDiffLines method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetDiffLines() throws Exception {
        final String resourceName = "diff.txt";
        final List<String> lines = readResourceLines(resourceName);
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        List<SideBySideView.Line> result = sut.getDiffLines(lines);
        assertEquals(24, result.size());
    }

    /**
     * Test of getDiffAsString method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetDiffAsString() throws IOException {
        String result = testGetDiffAsString("file1.txt", "file2.txt");
        assertThat(result, endsWith("@@ -1,1 +1,1 @@\n-a\n+b\n"));
        assertThat(result, containsString("--- "));
        assertThat(result, containsString("+++ "));
    }

    /**
     * Test of getDiffAsString method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetDiffAsStringOfEqualFiles() throws IOException {
        String result = testGetDiffAsString("file1.txt", "file1.txt");
        assertEquals("\n", result);
    }

    private String testGetDiffAsString(final String file1txt, final String file2txt) throws IOException {
        File file1 = new File(JobConfigHistoryBaseActionTest.class.getResource(file1txt).getPath());
        File file2 = new File(JobConfigHistoryBaseActionTest.class.getResource(file2txt).getPath());
        String[] file1Lines = readResourceLines(file1txt).toArray(new String[]{});
        String[] file2Lines = readResourceLines(file2txt).toArray(new String[]{});
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String result = sut.getDiffAsString(file1, file2, file1Lines, file2Lines);
        return result;
    }

    private List<String> readResourceLines(final String resourceName) throws IOException {
        final InputStream stream = JobConfigHistoryBaseActionTest.class.getResourceAsStream(resourceName);
        try {
            return IOUtils.readLines(stream, "UTF-8");
        } finally {
            stream.close();
        }
    }

    public class JobConfigHistoryBaseActionImpl extends JobConfigHistoryBaseAction {

        public JobConfigHistoryBaseActionImpl() {
            super(hudsonMock);
        }

        public void checkConfigurePermission() {
        }

        public boolean hasConfigurePermission() {
            return false;
        }

        public AccessControlled getAccessControlledObject() {
            return null;
        }

        public String getIconFileName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        StaplerRequest getCurrentRequest() {
            return staplerRequestMock;
        }
    }

}
