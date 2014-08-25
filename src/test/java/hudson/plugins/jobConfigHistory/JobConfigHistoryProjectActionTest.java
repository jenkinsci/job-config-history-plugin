package hudson.plugins.jobConfigHistory;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ItemGroup;

import java.io.IOException;
import java.util.List;

import jenkins.model.AbstractTopLevelItem;

import org.acegisecurity.AccessDeniedException;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Before;
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
    public UnpackResourceZip testConfigs = UnpackResourceZip.create();

    private final ItemGroup mockedItemGroup = mock(ItemGroup.class);
    private final MavenModule mockedMavenModule = mock(MavenModule.class);
    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Hudson mockedHudson = mock(hudson.model.Hudson.class);
    private final AbstractTopLevelItem mockedProject = mock(AbstractTopLevelItem.class);
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

    @Test
    public void testGetIconFileNameMatrixProject() {
        MatrixProject project = mock(MatrixProject.class);
        when(project.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);

        JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(mockedHudson, project);
        assertEquals(JobConfigHistoryConsts.ICONFILENAME, action.getIconFileName());
    }

    @Test
    public void testGetIconFileNameMatrixConfiguration() {
        MatrixConfiguration configuration = mock(MatrixConfiguration.class);
        when(configuration.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);

        JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(mockedHudson, configuration);
        assertNull(action.getIconFileName());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigs() throws Exception {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("");
        testJobXHasYHistoryEntries("jobs/Test1", 5);
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigsLimitedTo3() throws Exception {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("3");
        testJobXHasYHistoryEntries("jobs/Test1", 3);
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigsLimitedTo1000() throws Exception {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("1000");
        testJobXHasYHistoryEntries("jobs/Test1", 5);
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigsDeleted() throws Exception {
        final List<ConfigInfo> historyEntries = testJobXHasYHistoryEntries("jobs/Foo_deleted_20130830_223932_071", 3);
        assertEquals("Deleted", historyEntries.get(0).getOperation());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
     */
    @Test
    public void testGetJobConfigsEmpty() throws Exception {
        FileUtils.cleanDirectory(testConfigs.getResource("config-history/jobs/Test1"));
        testJobXHasYHistoryEntries("jobs/Test1", 0);
    }

    private List<ConfigInfo> testJobXHasYHistoryEntries(final String jobDir, final int noOfHistoryEntries) throws IOException {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedProject.getRootDir()).thenReturn(testConfigs.getResource(jobDir));
        final JobConfigHistoryProjectAction sut = createAction();
        final List<ConfigInfo> result = sut.getJobConfigs();
        assertEquals(noOfHistoryEntries, result.size());
        return result;
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
        when(mockedRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        when(mockedRequest.getInputStream()).thenReturn(TUtils.createServletInputStreamFromMultiPartFormData(boundary));
        JobConfigHistoryProjectAction sut = createAction();
        sut.doDiffFiles(mockedRequest, mockedResponse);
        verify(mockedResponse).sendRedirect("showDiffFiles?timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
    }
    
    @Test
    public void testGetTimestamp() throws Exception, Throwable {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        when(mockedRequest.getParameter(any(String.class))).thenReturn("2012-11-21_11-41-14");
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals("2012-11-21_11-41-14", sut.getTimestamp(1));
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
