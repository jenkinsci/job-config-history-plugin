/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.security.AccessControlled;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for JobConfigHistoryRootAction.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class JobConfigHistoryRootActionTest {

    private final StaplerRequest2 mockedStaplerRequest = mock(StaplerRequest2.class);

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void testGetUrlName() {
        JobConfigHistoryRootAction sut = createSut();
        String expResult = "/" + JobConfigHistoryConsts.URLNAME;
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    @Test
    void testGetIconFileNameWithoutPermissions() {
        assertNull(createUnauthorizedStaplerMockedSut().getIconFileName());
    }

    @Test
    void testGetIconFileNameWithJobPermission() {
        assertNotNull(createSut().getIconFileName());
    }

    @Test
    void testGetSingleConfigs_fromTo() throws Exception {
        given(mockedStaplerRequest.getRequestURI()).willReturn("/jenkins/" + JobConfigHistoryConsts.URLNAME + "/history");
        given(mockedStaplerRequest.getParameter("name")).willReturn("config");
        JobConfigHistoryRootAction sut = createStaplerMockedSut();


        assertEquals(2, sut.getSingleConfigs("config", 0, sut.getRevisionAmount()).size());
        assertEquals(1, sut.getSingleConfigs("config", 0, 1).size());

        //create a deleted project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        freeStyleProject.delete();
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    void testGetJobConfigs() throws Exception {
        //no projects
        assertEquals(0, createSut().getJobConfigs("").size());

        //one project (always 2 history entries on creation)
        jenkinsRule.createFreeStyleProject("Test1");
        assertEquals(2, createSut().getJobConfigs("").size());

        //one project and one deleted project.
        jenkinsRule.createFreeStyleProject("Test2").delete();
        assertEquals(5, createSut().getJobConfigs("").size());
        assertEquals(1, createSut().getJobConfigs("deleted").size());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    void testGetFileWithoutPermissions() throws Exception {
        given(mockedStaplerRequest.getParameter("name"))
                .willReturn("jobs/Test1");
        assertEquals("No permission to view config files",
                createUnauthorizedStaplerMockedSut().getFile());
        given(mockedStaplerRequest.getParameter("name"))
                .willReturn("Foo_deleted_20130830_223932_071");
        assertEquals("No permission to view config files",
                createUnauthorizedStaplerMockedSut().getFile());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    void testGetFile() throws Exception {
        //create a job, then feed the stapler request.
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);

        given(mockedStaplerRequest.getParameter("name"))
                .willReturn("jobs/Test1");

        //test first timestamp
        given(mockedStaplerRequest.getParameter("timestamp"))
                .willReturn(timestamps.get(0));
        final String result = createStaplerMockedSut().getFile();
        assertTrue(result.startsWith("<?xml"));

        //test second timestamp
        given(mockedStaplerRequest.getParameter("timestamp"))
                .willReturn(timestamps.get(1));
        final String result2 = createStaplerMockedSut().getFile();
        assertTrue(result2.startsWith("<?xml"));

        assertEquals(result, result2);
    }

    @Test
    void testGetFileDeleted() throws Exception {

        //initialize deleted project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        freeStyleProject.delete();

        //feed stapler mock
        final String name = PluginUtils.getHistoryDao().getDeletedJobs()[0].getName();
        given(mockedStaplerRequest.getParameter("name")).willReturn(name);
        final String timestamp = timestamps.get(0);
        given(mockedStaplerRequest.getParameter("timestamp"))
                .willReturn(timestamp);

        //test
        final String result = createStaplerMockedSut().getFile();
        assertTrue(result.startsWith("<?xml"));
    }

    @Test
    void testCreateLinkToFilesDeleted() throws IOException, InterruptedException {
        //create a project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        freeStyleProject.delete();

        //get the expected timestamp
        List<File> revisionsFromDeletedProject = Arrays.stream(PluginUtils.getHistoryDao().getDeletedJobs()[0].listFiles())
                .sorted(Comparator.naturalOrder()).toList();

        //expected stuff
        final String deletedFolderName = PluginUtils.getHistoryDao().getDeletedJobs()[0].getName();
        final String expTimestamp = revisionsFromDeletedProject.get(revisionsFromDeletedProject.size() - 2).getName();
        final String expResult = "configOutput?type=&name=" + deletedFolderName + "&timestamp=" + expTimestamp;

        //mock configInfo and test
        final ConfigInfo config = mock(ConfigInfo.class);

        given(config.getJob()).willReturn(deletedFolderName);
        assertEquals(expResult, createSut().createLinkToFiles(config, ""));

        given(config.getJob())
                .willReturn("Unknown_deleted_20130830_223932_072");
        assertNull(createSut().createLinkToFiles(config, ""));
    }

    @Test
    void testCreateLinkToFiles() {
        final ConfigInfo config = mock(ConfigInfo.class);
        given(config.getJob()).willReturn("Test1");
        given(config.getDate()).willReturn("2012-11-21_11-42-05");
        given(config.getIsJob()).willReturn(true);
        String expectedRegex = "http://localhost:[0-9]{1,5}?/jenkins/job/Test1/jobConfigHistory/configOutput\\?type=xml&timestamp=[0-9\\-_]+";
        assertTrue(createSut().createLinkToFiles(config, "xml").matches(expectedRegex));
    }

    @Test
    void testCreateLinkToFilesSystem() {
        final ConfigInfo config = mock(ConfigInfo.class);
        given(config.getJob()).willReturn("config");
        given(config.getDate()).willReturn("2012-11-21_11-42-05");
        given(config.getIsJob()).willReturn(false);
        String expResult = "configOutput?type=xml&name=config&timestamp=2012-11-21_11-42-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, "xml"));
    }

    @Test
    void testGetAccessControlledObject() {
        assertEquals(jenkinsRule.getInstance(), createSut().getAccessControlledObject());
    }

    @Test
    void testCheckConfigurePermission() {
        createSut().checkConfigurePermission();
    }

    @Test
    void testHasConfigurePermission() {
        assertTrue(createSut().hasConfigurePermission());
    }

    @Test
    void testHasJobConfigurePermission() {
        assertTrue(createSut().hasJobConfigurePermission());
    }

    @Test
    void testCheckParametersIAE() {
        assertThrows(IllegalArgumentException.class, () ->
            createSut().checkParameters("foo", "bar"));
    }

    @Test
    void testCheckParametersNameIsNull() {
        assertFalse(createSut().checkParameters(null, "2013-01-18_18-24-33"));
        assertFalse(createSut().checkParameters("null", "2013-01-18_18-24-33"));
    }

    @Test
    void testCheckParametersNameHasDots() {
        assertThrows(IllegalArgumentException.class, () ->
            createSut().checkParameters("../foo", "2013-01-18_18-24-33"));
    }

    @Test
    void testCheckParameters() {
        assertTrue(createSut().checkParameters("foo", "2013-01-18_18-24-33"));
    }

    @Test
    void testDoDiffFiles() throws Exception {
        final String boundary = "AAAA";
        given(mockedStaplerRequest.getContentType())
                .willReturn("multipart/form-data; boundary=" + boundary);
        given(mockedStaplerRequest.getInputStream()).willReturn(
                TUtils.createServletInputStreamFromMultiPartFormData(boundary));
        StaplerResponse2 rsp = mock(StaplerResponse2.class);
        JobConfigHistoryRootAction sut = createStaplerMockedSut();
        sut.doDiffFiles(mockedStaplerRequest, rsp);
        verify(rsp).sendRedirect(
                "showDiffFiles?name=foo&timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
    }

    @Test
    void testGetLines() throws Exception {

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");

        freeStyleProject.setDescription("newDescription");
        freeStyleProject.setJDK(new JDK("name", "javahome"));
        testLines(0, freeStyleProject, 0, 0, false);
        testLines(0, freeStyleProject, 0, 1, false);
        testLines(7, freeStyleProject, 1, 2, false);
    }

    private void testLines(long expected, FreeStyleProject freeStyleProject, int i, int j, boolean hideVersionDiffs) throws IOException {
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        assertTrue(timestamps.size() > Math.max(i, j), "i or j are too high");

        final List<SideBySideView.Line> lines = createSut().getLines(
                getConfigXml(freeStyleProject, timestamps.get(i)),
                getConfigXml(freeStyleProject, timestamps.get(j)),
                hideVersionDiffs
        );
        assertEquals(expected, lines.size());
    }

    private List<String> getCreatedTimestamps(FreeStyleProject freeStyleProject) {
        PluginUtils.getHistoryDao();
        File projectHistoryRootDir =
                new File(
                        new File(
                                new File(
                                        jenkinsRule.getInstance().getRootDir(),
                                        JobConfigHistoryConsts.DEFAULT_HISTORY_DIR
                                ),
                                JobConfigHistoryConsts.JOBS_HISTORY_DIR
                        ),
                        freeStyleProject.getName()
                );
        return Arrays.stream(projectHistoryRootDir.listFiles()).map(File::getName)
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private XmlFile getConfigXml(FreeStyleProject freeStyleProject, String timestamp) {
        return PluginUtils.getHistoryDao().getOldRevision(freeStyleProject, timestamp);
    }

    @Test
    void testGetLinesNoPermissions() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("Test1");
        assertEquals(0, createUnauthorizedStaplerMockedSut().getLines().size());
    }

    @Test
    void testFindNewName() throws Exception {
        FreeStyleProject test1 = jenkinsRule.createFreeStyleProject("Test1");
        assertEquals("Test1_1", createSut().findNewName("Test1"));

        jenkinsRule.createFreeStyleProject("Test1_1");
        assertEquals("Test1_2", createSut().findNewName("Test1"));

        test1.delete();
        assertEquals("Test1", createSut().findNewName("Test1"));
    }

    @Test
    void testGetOldConfigXml() throws IOException, InterruptedException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        freeStyleProject.delete();

        final String timestamp = timestamps.get(0);
        final File jobConfig = new File(
                new File(
                        PluginUtils.getHistoryDao().getDeletedJobs()[0],
                        timestamp
                ),
                "config.xml"
        );

        assertEquals(
                jobConfig,
                createSut()
                        .getOldConfigXml(PluginUtils.getHistoryDao().getDeletedJobs()[0].getName(), timestamp)
                        .getFile()
        );
    }

    @Test
    void testGetOldConfigXmlNonExisting() {
        final String name = "jobs/I_DO_NOT_EXIST";
        final String timestamp = "2012-11-21_11-35-12";
        assertThrows(IllegalArgumentException.class, () ->
            createSut().getOldConfigXml(name, timestamp));
    }

    @Test
    void testGetLastAvailableConfigXml() throws IOException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        final String timestamp = timestamps.get(0);
        final String expectedSuffix = "Test1" + File.separator + timestamp + File.separator + "config.xml";

        String name = "jobs/Test1";
        assertThat(
                createSut().getLastAvailableConfigXml(name).getFile().getPath(),
                TUtils.pathEndsWith(expectedSuffix));
    }

    @Test
    void testGetLastAvailableConfigXmlNoConfigs() {
        String name = "jobs/I_DO_NOT_EXIST";
        assertNull(createSut().getLastAvailableConfigXml(name));
    }

    JobConfigHistoryRootAction createSut() {
        return new JobConfigHistoryRootAction();
    }

    JobConfigHistoryRootAction createStaplerMockedSut() {
        return new JobConfigHistoryRootAction() {
            @Override
            protected StaplerRequest2 getCurrentRequest() {
                return mockedStaplerRequest;
            }
        };
    }

    JobConfigHistoryRootAction createUnauthorizedStaplerMockedSut() {
        return new JobConfigHistoryRootAction() {
            @Override
            protected StaplerRequest2 getCurrentRequest() {
                return mockedStaplerRequest;
            }

            @Override
            public AccessControlled getAccessControlledObject() {
                return mock(Jenkins.class);
            }
        };
    }
}
