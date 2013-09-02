package hudson.plugins.jobConfigHistory;

import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import java.util.List;
import org.acegisecurity.AccessDeniedException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.mockito.Mockito.*;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryProjectActionTest {

    public JobConfigHistoryProjectActionTest() {
    }
    private final MavenModule mockedMavenModule = mock(MavenModule.class);
    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Hudson mockedHudson = mock(hudson.model.Hudson.class);
    private final AbstractItem mockedProject = mock(AbstractItem.class);
    //private final StaplerResponse mockedResponse = mock(StaplerResponse.class);
    //private final StaplerRequest mockedRequest = mock(StaplerRequest.class);

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
    @Ignore
    public void testGetJobConfigs() throws Exception {
        System.out.println("getJobConfigs");
        JobConfigHistoryProjectAction sut = null;
        List<ConfigInfo> expResult = null;
        List<ConfigInfo> result = sut.getJobConfigs();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFile method, of class JobConfigHistoryProjectAction.
     */
    @Test
    @Ignore
    public void testGetFile() throws Exception {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);
        JobConfigHistoryProjectAction sut = createAction();
        String expResult = "";
        String result = sut.getFile();
        assertEquals(expResult, result);
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
    @Ignore
    public void testDoDiffFiles() throws Exception {
        System.out.println("doDiffFiles");
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryProjectAction sut = null;
        sut.doDiffFiles(req, rsp);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLines method, of class JobConfigHistoryProjectAction.
     */
    @Test
    @Ignore
    public void testGetLines() throws Exception {
        System.out.println("getLines");
        JobConfigHistoryProjectAction sut = null;
        List<JobConfigHistoryBaseAction.SideBySideView.Line> expResult = null;
        List<JobConfigHistoryBaseAction.SideBySideView.Line> result = sut.getLines();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of doRestore method, of class JobConfigHistoryProjectAction.
     */
    @Test
    @Ignore
    public void testDoRestore() throws Exception {
        System.out.println("doRestore");
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryProjectAction sut = null;
        sut.doRestore(req, rsp);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of doForwardToRestoreQuestion method, of class JobConfigHistoryProjectAction.
     */
    @Test
    @Ignore
    public void testDoForwardToRestoreQuestion() throws Exception {
        System.out.println("doForwardToRestoreQuestion");
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryProjectAction sut = null;
        sut.doForwardToRestoreQuestion(req, rsp);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    private JobConfigHistoryProjectAction createAction() {
        when(mockedHudson.getPlugin(JobConfigHistory.class)).thenReturn(mockedPlugin);
        return new JobConfigHistoryProjectAction(mockedHudson, mockedProject);
    }

    private JobConfigHistoryProjectAction createActionForMavenModule() {
        when(mockedHudson.getPlugin(JobConfigHistory.class)).thenReturn(mockedPlugin);
        return new JobConfigHistoryProjectAction(mockedHudson, mockedMavenModule);
    }
}
