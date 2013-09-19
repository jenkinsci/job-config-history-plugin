package hudson.plugins.jobConfigHistory;

import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletInputStream;
import org.acegisecurity.AccessDeniedException;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import static org.mockito.Mockito.*;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryProjectActionTest {

    @Rule
    public UnpackResourceZip testConfigs = UnpackResourceZip.INSTANCE;

    private final ItemGroup mockedItemGroup = mock(ItemGroup.class);
    private final MavenModule mockedMavenModule = mock(MavenModule.class);
    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Hudson mockedHudson = mock(hudson.model.Hudson.class);
    private final AbstractItem mockedProject = mock(AbstractItem.class);
    private final StaplerRequest mockedRequest = mock(StaplerRequest.class);
    private final StaplerResponse mockedResponse = mock(StaplerResponse.class);
    private HistoryDao historyDao;


    public JobConfigHistoryProjectActionTest() {
        when(mockedItemGroup.getFullName()).thenReturn("");
        when(mockedProject.getParent()).thenReturn(mockedItemGroup);
        when(mockedProject.getFullName()).thenReturn("Test1");
    }

    @Before
    public void createHistoryDao() {
        historyDao = new FileHistoryDao(testConfigs.getResource("config-history"), testConfigs.getRoot(), null, 0, false);
    }
    /**
     * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetIconFileNameNoPermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertNull(sut.getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetIconFileNameSaveProjectNonMavenModules() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(JobConfigHistoryConsts.ICONFILENAME, sut.getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetIconFileNameSaveMavenModules() {
        when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(true);
        JobConfigHistoryProjectAction sut = createActionForMavenModule();
        assertEquals(JobConfigHistoryConsts.ICONFILENAME, sut.getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetIconFileNameDoNotSaveMavenModules() {
        when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
        JobConfigHistoryProjectAction sut = createActionForMavenModule();
        assertNull(sut.getIconFileName());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigs() throws Exception {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource("jobs/Test1"));
        final JobConfigHistoryProjectAction sut = createAction();
        final List<ConfigInfo> result = sut.getJobConfigs();
        assertEquals(5, result.size());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigsEmpty() throws Exception {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource("jobs/Test1"));
        FileUtils.cleanDirectory(testConfigs.getResource("config-history/jobs/Test1"));
        final JobConfigHistoryProjectAction sut = createAction();
        final List<ConfigInfo> result = sut.getJobConfigs();
        assertEquals(0, result.size());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetFile() throws Exception {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource("jobs/Test1"));
        when(mockedRequest.getParameter("timestamp")).thenReturn("2012-11-21_11-40-28");
        final JobConfigHistoryProjectAction sut = createAction();
        String result = sut.getFile();
        assertThat(result, CoreMatchers.startsWith("<?xml version="));
        assertThat(result, CoreMatchers.endsWith("</project>"));
    }

    /**
     * Test of getProject method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetProject() {
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(mockedProject, sut.getProject());
    }

    /**
     * Test of getAccessControlledObject method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetAccessControlledObject() {
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(mockedProject, sut.getAccessControlledObject());
    }

    /**
     * Test of checkConfigurePermission method, of class JobConfigHistoryProjectAction.
     */
    @Test(expected = AccessDeniedException.class)
    public void testCheckConfigurePermission() {
        doThrow(new AccessDeniedException("Oops")).when(mockedProject).checkPermission(AbstractItem.CONFIGURE);
        JobConfigHistoryProjectAction sut = createAction();
        sut.checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testHasConfigurePermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        JobConfigHistoryProjectAction sut = createAction();
        assertTrue(sut.hasConfigurePermission());
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testHasNoConfigurePermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertFalse(sut.hasConfigurePermission());
    }

    /**
     * Test of doDiffFiles method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testDoDiffFiles() throws Exception {
        final String boundary = "AAAA";
        final String body =
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"timestamp1\"\r\n\r\n" +
                "111\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"timestamp2\"\r\n\r\n" +
                "112\r\n" +
                "--" + boundary + "--\r\n";
        final ByteArrayInputStream bodyByteStream = new ByteArrayInputStream(body.getBytes());
        final ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bodyByteStream.read();
            }
        };

        when(mockedRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        when(mockedRequest.getInputStream()).thenReturn(servletInputStream);
        JobConfigHistoryProjectAction sut = createAction();
        sut.doDiffFiles(mockedRequest, mockedResponse);
        verify(mockedResponse).sendRedirect("showDiffFiles?timestamp1=111&timestamp2=112");
    }

    /**
     * Test of getLines method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetLines() throws Exception, Throwable {
        final String timestamp1 = "2012-11-21_11-41-14";
        final String timestamp2 = "2012-11-21_11-42-05";
        List<SideBySideView.Line> result = prepareGetLines(timestamp1, timestamp2);
        assertEquals(8, result.size());
    }

    /**
     * Test of getLines method, of class JobConfigHistoryProjectAction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetLinesNonExistingTimestamp() throws IOException {
        final String timestamp1 = "2012-11-21_11-41-14";
        final String timestamp2 = "2013-11-21_11-42-05";
        prepareGetLines(timestamp1, timestamp2);
    }

    private List<SideBySideView.Line> prepareGetLines(final String timestamp1, final String timestamp2) throws IOException {
        when(mockedRequest.getParameter("timestamp1")).thenReturn(timestamp1);
        when(mockedRequest.getParameter("timestamp2")).thenReturn(timestamp2);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource("jobs/Test1"));
        JobConfigHistoryProjectAction sut = createAction();
        List<SideBySideView.Line> result = sut.getLines();
        return result;
    }

    /**
     * Test of doRestore method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testDoRestore() throws Exception {
        when(mockedRequest.getParameter("timestamp")).thenReturn("2012-11-21_11-41-14");
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource("jobs/Test1"));
        JobConfigHistoryProjectAction sut = createAction();
        sut.doRestore(mockedRequest, mockedResponse);
        verify(mockedProject).save();
    }

    /**
     * Test of doForwardToRestoreQuestion method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testDoForwardToRestoreQuestion() throws Exception {
        when(mockedRequest.getParameter("timestamp")).thenReturn("2012-11-21_11-41-14");
        JobConfigHistoryProjectAction sut = createAction();
        sut.doForwardToRestoreQuestion(mockedRequest, mockedResponse);
        verify(mockedResponse).sendRedirect(any(String.class));
    }

    private JobConfigHistoryProjectAction createAction() {
        return new JobConfigHistoryProjectActionImpl(mockedHudson, mockedProject);
    }

    private JobConfigHistoryProjectAction createActionForMavenModule() {
        return new JobConfigHistoryProjectActionImpl(mockedHudson, mockedMavenModule);
    }

    private class JobConfigHistoryProjectActionImpl extends JobConfigHistoryProjectAction {

        public JobConfigHistoryProjectActionImpl(Hudson hudson, AbstractItem project) {
            super(hudson, project);
        }

        @Override
        JobConfigHistory getPlugin() {
            return mockedPlugin;
        }

        @Override
        StaplerRequest getCurrentRequest() {
            return mockedRequest;
        }

        @Override
        HistoryDao getHistoryDao() {
            return historyDao;
        }
    }
}
