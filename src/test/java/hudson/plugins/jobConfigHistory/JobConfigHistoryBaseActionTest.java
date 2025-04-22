package hudson.plugins.jobConfigHistory;

import hudson.security.AccessControlled;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistoryBaseAction.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
class JobConfigHistoryBaseActionTest {

    private final Jenkins jenkinsMock = mock(Jenkins.class);
    private final StaplerRequest2 staplerRequestMock = mock(
            StaplerRequest2.class);

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void testGetDisplayName() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "Job Config History";
        String result = sut.getDisplayName();
        assertEquals(expResult, result);
    }

    @Test
    void testGetUrlName() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "jobConfigHistory";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    @Test
    void testGetOutputTypeXml() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        when(staplerRequestMock.getParameter("type")).thenReturn("xml");
        String expResult = "xml";
        String result = sut.getOutputType();
        assertEquals(expResult, result);
    }

    @Test
    void testGetOutputTypeOther() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        when(staplerRequestMock.getParameter("type"))
                .thenReturn("does not matter");
        String expResult = "plain";
        String result = sut.getOutputType();
        assertEquals(expResult, result);
    }

    @Test
    void testCheckTimestamp() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        assertFalse(sut.checkTimestamp("null"));
        assertFalse(sut.checkTimestamp(null));
        assertTrue(sut.checkTimestamp("2013-08-31_23-59-59"));
    }

    @Test
    void testGetRequestParameter() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        final String parameterName = "type";
        when(staplerRequestMock.getParameter(parameterName)).thenReturn("xml");
        String expResult = "xml";
        String result = sut.getRequestParameter(parameterName);
        assertEquals(expResult, result);
    }

    @Test
    void testCheckConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        sut.checkConfigurePermission();
    }

    @Test
    void testHasConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        boolean expResult = false;
        boolean result = sut.hasConfigurePermission();
        assertEquals(expResult, result);
    }

    @Test
    void testGetAccessControlledObject() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        AccessControlled expResult = null;
        AccessControlled result = sut.getAccessControlledObject();
        assertEquals(expResult, result);
    }

    @Test
    void testGetDiffLines() throws Exception {
        final String resourceName = "diff.txt";
        final List<String> lines = TUtils.readResourceLines(resourceName);
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        List<SideBySideView.Line> result = sut.getDiffLines(lines);
        assertEquals(24, result.size());
    }

    @Test
    void testGetDiffAsString() throws IOException {
        String result = testGetDiffAsString("file1.txt", "file2.txt");
        assertThat(result, endsWith("@@ -1,1 +1,1 @@\n-a\n+b\n"));
        assertThat(result, containsString("--- "));
        assertThat(result, containsString("+++ "));
    }

    @Test
    void testGetDiffAsStringOfEqualFiles() throws IOException {
        String result = testGetDiffAsString("file1.txt", "file1.txt");
        assertEquals("\n", result);
    }

    @Test
    void testGetMaxEntriesPerPage() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        assertEquals(JobConfigHistoryConsts.DEFAULT_MAX_ENTRIES_PER_PAGE, sut.getMaxEntriesPerPage());
    }

    @Test
    void testGetRelevantPageNums() {
        //assuming JobConfigHistoryConsts.PAGING_EPSILON == 2
        assertArrayEquals(
                new Integer[]{0, 1, 2, -1, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(0, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, -1, 24, 25, 26, 27, 28, -1, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(26, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, -1, 48, 49, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(50, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, -1, 46, 47, 48, 49, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(48, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, -1, 44, 45, 46, 47, 48, -1, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(46, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, -1, 2, 3, 4, 5, 6, -1, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(4, 50).toArray()
        );
        assertArrayEquals(
                new Integer[]{0, 1, 2, 3, 4, -1, 50},
                new JobConfigHistoryBaseActionImpl().getRelevantPageNums(2, 50).toArray()
        );
    }

    private String testGetDiffAsString(final String file1txt,
                                       final String file2txt) throws IOException {
        File file1 = new File(JobConfigHistoryBaseActionTest.class
                .getResource(file1txt).getPath());
        File file2 = new File(JobConfigHistoryBaseActionTest.class
                .getResource(file2txt).getPath());
        String[] file1Lines = TUtils.readResourceLines(file1txt)
                .toArray(new String[]{});
        String[] file2Lines = TUtils.readResourceLines(file2txt)
                .toArray(new String[]{});
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        return sut.getDiffAsString(file1, file2, file1Lines, file2Lines);
    }

    public class JobConfigHistoryBaseActionImpl
            extends
            JobConfigHistoryBaseAction {

        public void checkConfigurePermission() {
        }

        @Override
        public boolean hasAdminPermission() {
            return false;
        }

        @Override
        public boolean hasDeleteEntryPermission() {
            return getAccessControlledObject().hasPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
        }

        @Override
        protected void checkDeleteEntryPermission() {
            getAccessControlledObject().checkPermission(JobConfigHistory.DELETEENTRY_PERMISSION);
        }

        public boolean hasConfigurePermission() {
            return false;
        }

        @Override
        public int getRevisionAmount() {
            return -1;
        }

        public AccessControlled getAccessControlledObject() {
            return null;
        }

        public String getIconFileName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<SideBySideView.Line> getLines(boolean useRegex) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected StaplerRequest2 getCurrentRequest() {
            return staplerRequestMock;
        }
    }

}
