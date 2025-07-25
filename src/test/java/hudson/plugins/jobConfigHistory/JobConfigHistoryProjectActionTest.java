package hudson.plugins.jobConfigHistory;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import jenkins.model.AbstractTopLevelItem;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistoryProjectAction.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class JobConfigHistoryProjectActionTest {

    private final MavenModule mockedMavenModule = mock(MavenModule.class);
    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final AbstractTopLevelItem mockedProject = mock(AbstractTopLevelItem.class);
    private final StaplerRequest2 mockedRequest = mock(StaplerRequest2.class);
    private final StaplerResponse2 mockedResponse = mock(StaplerResponse2.class);

    private UnpackResourceZip unpackResourceZip;
    private HistoryDao historyDao;

    private JenkinsRule jenkinsRule;


    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception{
        jenkinsRule = rule;
        unpackResourceZip = UnpackResourceZip.create();
        Jenkins mockedJenkins = mock(Jenkins.class);
        when(mockedJenkins.getFullName()).thenReturn("");
        when(mockedProject.getParent()).thenReturn(mockedJenkins);
        when(mockedProject.getFullName()).thenReturn("Test1");

        historyDao = new FileHistoryDao(
                unpackResourceZip.getResource("config-history"),
                unpackResourceZip.getRoot(), null, 0, false);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (unpackResourceZip != null) {
            unpackResourceZip.cleanUp();
        }
    }

    @Test
    void testGetIconFileNameNoPermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertNull(sut.getIconFileName());
    }

    @Test
    void testGetIconFileNameSaveProjectNonMavenModules() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(JobConfigHistoryConsts.ICONFILENAME,
                sut.getIconFileName());
    }

    @Test
    void testGetIconFileNameSaveMavenModules() {
        assertNotNull(jenkinsRule.jenkins.getPlugin("maven-plugin"));
        when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(true);
        JobConfigHistoryProjectAction sut = createActionForMavenModule();
        assertEquals(JobConfigHistoryConsts.ICONFILENAME,
                sut.getIconFileName());
    }

    @Test
    void testGetIconFileNameDoNotSaveMavenModules() {
        when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
        JobConfigHistoryProjectAction sut = createActionForMavenModule();
        assertNull(sut.getIconFileName());
    }

    @Test
    void testGetIconFileNameMatrixProject() {
        MatrixProject project = mock(MatrixProject.class);
        when(project.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);

        JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(project);
        assertEquals(JobConfigHistoryConsts.ICONFILENAME,
                action.getIconFileName());
    }

    @Test
    void testGetIconFileNameMatrixConfiguration() {
        MatrixConfiguration configuration = mock(MatrixConfiguration.class);
        when(configuration.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);

        JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(configuration);
        assertNull(action.getIconFileName());
    }

    @Test
    void testGetJobConfigs() {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("");
        testJobXHasYHistoryEntries("jobs/Test1", 5);
    }

    @Test
    void testGetJobConfigsLimitedTo3() {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("3");
        testJobXHasYHistoryEntries("jobs/Test1", 3);
    }

    @Test
    void testGetJobConfigsLimitedTo1000() {
        when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("1000");
        testJobXHasYHistoryEntries("jobs/Test1", 5);
    }

    @Test
    void testGetJobConfigsDeleted() {
        final List<ConfigInfo> historyEntries = testJobXHasYHistoryEntries(
                "jobs/Foo_deleted_20130830_223932_071", 3);
        assertEquals("Deleted", historyEntries.get(0).getOperation());
    }

    @Test
    void testGetJobConfigsEmpty() throws Exception {
        FileUtils.cleanDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"));
        testJobXHasYHistoryEntries("jobs/Test1", 0);
    }

    @Test
    void testGetRevisionAmount() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("project");
        final JobConfigHistoryProjectAction sut = createJenkinsRuleAction(project);
        assertEquals(2, sut.getRevisionAmount());

        project.renameTo("pr0ject");
        assertEquals(3, sut.getRevisionAmount());
    }

    @Test
    void testGetMaxPageNum() throws IOException {

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("project");
        final JobConfigHistoryProjectAction sut = createJenkinsRuleAction(project);
        project.renameTo("asd");
        project.renameTo("asdf");
        project.renameTo("asd");
        project.renameTo("asdf");
        //revision amount == 6

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(10));
        assertEquals(0, sut.getMaxPageNum());

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(6));
        assertEquals(0, sut.getMaxPageNum());

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(5));
        assertEquals(1, sut.getMaxPageNum());

        project.renameTo("pr0ject");
        for (int i = 0; i < 13; ++i) {
            project.renameTo("p." + i);
        }
        //revision amount == 20

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(5));
        assertEquals(3, sut.getMaxPageNum());

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(4));
        assertEquals(4, sut.getMaxPageNum());

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(20));
        assertEquals(0, sut.getMaxPageNum());

        when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(10));
        assertEquals(1, sut.getMaxPageNum());
    }

    @Test
    void testGetJobConfigs_fromTo() {
        testJobXHasYHistoryEntries("jobs/Test1", 5, 0, 200);
        testJobXHasYHistoryEntries("jobs/Test1", 4, 1, 200);
        testJobXHasYHistoryEntries("jobs/Test1", 3, 2, 200);
        testJobXHasYHistoryEntries("jobs/Test1", 1, 4, 200);
        testJobXHasYHistoryEntries("jobs/Test1", 0, 5, 200);

        testJobXHasYHistoryEntries("jobs/Test1", 4, 0, 4);
        testJobXHasYHistoryEntries("jobs/Test1", 5, 0, 5);
        testJobXHasYHistoryEntries("jobs/Test1", 5, 0, 6);

        testJobXHasYHistoryEntries("jobs/Test1", 1, 2, 3);
    }

    private List<ConfigInfo> testJobXHasYHistoryEntries(final String jobDir,
                                                        final int noOfHistoryEntries) {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedProject.getRootDir())
                .thenReturn(unpackResourceZip.getResource(jobDir));
        when(mockedProject.getConfigFile()).thenCallRealMethod();
        final JobConfigHistoryProjectAction sut = createAction();
        final List<ConfigInfo> result = sut.getJobConfigs();
        assertEquals(noOfHistoryEntries, result.size());
        return result;
    }

    private List<ConfigInfo> testJobXHasYHistoryEntries(final String jobDir,
                                                        final int noOfHistoryEntries,
                                                        int from, int to) {
        reset(mockedProject);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedProject.getRootDir())
                .thenReturn(unpackResourceZip.getResource(jobDir));
        when(mockedProject.getConfigFile()).thenCallRealMethod();
        final JobConfigHistoryProjectAction sut = createAction();
        final List<ConfigInfo> result = sut.getJobConfigs(from, to);
        assertEquals(noOfHistoryEntries, result.size());
        return result;
    }

    @Test
    void testGetFile() throws Exception {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedProject.getRootDir())
                .thenReturn(unpackResourceZip.getResource("jobs/Test1"));
        when(mockedProject.getConfigFile()).thenCallRealMethod();
        when(mockedRequest.getParameter("timestamp"))
                .thenReturn("2012-11-21_11-40-28");
        final JobConfigHistoryProjectAction sut = createAction();
        String result = sut.getFile();
        assertThat(result, Matchers.startsWith("<?xml version="));
        assertThat(result, Matchers.endsWith("</project>"));
    }

    @Test
    void testGetProject() {
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(mockedProject, sut.getProject());
    }

    @Test
    void testGetAccessControlledObject() {
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals(mockedProject, sut.getAccessControlledObject());
    }

    @Test
    void testCheckConfigurePermission() {
        JobConfigHistoryProjectAction sut = createAction();
        verify(mockedProject, times(0)).checkPermission(AbstractItem.CONFIGURE);
        sut.checkConfigurePermission();
        verify(mockedProject, times(1)).checkPermission(AbstractItem.CONFIGURE);
    }

    @Test
    void testHasConfigurePermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        JobConfigHistoryProjectAction sut = createAction();
        assertTrue(sut.hasConfigurePermission());
    }

    @Test
    void testHasNoConfigurePermission() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(false);
        JobConfigHistoryProjectAction sut = createAction();
        assertFalse(sut.hasConfigurePermission());
    }

    @Test
    void testDoDiffFiles() throws Exception {
        when(mockedRequest.getParameter("timestamp1"))
                .thenReturn("2014-02-05_10-42-37");
        when(mockedRequest.getParameter("timestamp2"))
                .thenReturn("2014-03-12_11-02-12");
        JobConfigHistoryProjectAction sut = createAction();
        sut.doDiffFiles(mockedRequest, mockedResponse);
        verify(mockedResponse).sendRedirect(
                "showDiffFiles?timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
    }

    @Test
    void testGetTimestamp() {
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedRequest.getParameter(any(String.class)))
                .thenReturn("2012-11-21_11-41-14");
        JobConfigHistoryProjectAction sut = createAction();
        assertEquals("2012-11-21_11-41-14", sut.getTimestamp(1));
    }

    @Test
    void testGetLines() throws Throwable {
        final String timestamp1 = "2012-11-21_11-41-14";
        final String timestamp2 = "2012-11-21_11-42-05";
        List<SideBySideView.Line> result = prepareGetLines(timestamp1,
                timestamp2);
        assertEquals(8, result.size());
    }

    @Test
    void testGetLinesNonExistingTimestamp() {
        final String timestamp1 = "2012-11-21_11-41-14";
        final String timestamp2 = "2013-11-21_11-42-05";
        assertThrows(IllegalArgumentException.class, () ->
            prepareGetLines(timestamp1, timestamp2));
    }

    private List<SideBySideView.Line> prepareGetLines(final String timestamp1,
                                                      final String timestamp2) throws IOException {
        when(mockedRequest.getParameter("timestamp1")).thenReturn(timestamp1);
        when(mockedRequest.getParameter("timestamp2")).thenReturn(timestamp2);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedProject.getRootDir())
                .thenReturn(unpackResourceZip.getResource("jobs/Test1"));
        when(mockedProject.getConfigFile()).thenCallRealMethod();
        JobConfigHistoryProjectAction sut = createAction();
        return sut.getLines();
    }

    @Test
    void testDoRestore() throws Exception {
        when(mockedRequest.getParameter("timestamp"))
                .thenReturn("2012-11-21_11-41-14");
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
                .thenReturn(true);
        when(mockedProject.getRootDir())
                .thenReturn(unpackResourceZip.getResource("jobs/Test1"));
        when(mockedProject.getConfigFile()).thenCallRealMethod();
        JobConfigHistoryProjectAction sut = createAction();
        sut.doRestore(mockedRequest, mockedResponse);
        verify(mockedProject).save();
    }

    private JobConfigHistoryProjectAction createAction() {
        return new JobConfigHistoryProjectActionImpl(mockedProject);
    }

    private JobConfigHistoryProjectAction createJenkinsRuleAction(Project<?, ?> project) {
        return new JobConfigHistoryProjectActionJrImpl(project);
    }

    private JobConfigHistoryProjectAction createActionForMavenModule() {
        return new JobConfigHistoryProjectActionImpl(mockedMavenModule);
    }

    private class JobConfigHistoryProjectActionImpl extends JobConfigHistoryProjectAction {

        public JobConfigHistoryProjectActionImpl(AbstractItem project) {
            super(project);
        }

        @Override
        protected JobConfigHistory getPlugin() {
            return mockedPlugin;
        }

        @Override
        protected StaplerRequest2 getCurrentRequest() {
            return mockedRequest;
        }

        @Override
        protected HistoryDao getHistoryDao() {
            return historyDao;
        }
    }

    private class JobConfigHistoryProjectActionJrImpl extends JobConfigHistoryProjectAction {

        public JobConfigHistoryProjectActionJrImpl(AbstractItem project) {
            super(project);
        }

        @Override
        protected StaplerRequest2 getCurrentRequest() {
            return mockedRequest;
        }
    }
}
