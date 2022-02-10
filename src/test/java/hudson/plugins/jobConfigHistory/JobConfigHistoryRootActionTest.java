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

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.XmlFile;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Project;
import hudson.security.AccessControlled;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryRootActionTest {

    private final StaplerRequest mockedStaplerRequest = mock(StaplerRequest.class);
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Test of getUrlName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetUrlName() {
        JobConfigHistoryRootAction sut = createSut();
        String expResult = "/" + JobConfigHistoryConsts.URLNAME;
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileNameWithoutPermissions() {
        assertNull(createUnauthorizedStaplerMockedSut().getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileName() {
        assertNotNull(createSut().getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileNameWithJobPermission() {
        assertNotNull(createSut().getIconFileName());
    }

    /* TODO Fixme
    @Test
    public void testGetConfigs_fromTo() throws IOException, InterruptedException {
//        given(mockedStaplerRequest.getRequestURI()).willReturn("/jenkins/" + JobConfigHistoryConsts.URLNAME + "/history"); todo test for that!
        given(mockedStaplerRequest.getRequestURI()).willReturn("/jenkins/" + JobConfigHistoryConsts.URLNAME);
        Project<FreeStyleProject, FreeStyleBuild> project = jenkinsRule.createFreeStyleProject("Test1");
        JobConfigHistoryRootAction sut = createStaplerMockedSut();

        given(mockedStaplerRequest.getParameter("filter")).willReturn(null);
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertTrue(createStaplerMockedSut().getConfigs(0, 10).size() >= 8); // either 8 or 9

        given(mockedStaplerRequest.getParameter("filter")).willReturn("system");
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertTrue(createStaplerMockedSut().getConfigs(0, 10).size() >= 8); // either 8 or 9

        given(mockedStaplerRequest.getParameter("filter")).willReturn("all");
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertTrue(createStaplerMockedSut().getConfigs(0, 10).size() >= 10);

        given(mockedStaplerRequest.getParameter("filter")).willReturn("jobs");
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertEquals(2, createStaplerMockedSut().getConfigs(0, 10).size());
        project.renameTo("asd");
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertEquals(3, createStaplerMockedSut().getConfigs(0, 10).size());

        given(mockedStaplerRequest.getParameter("filter")).willReturn("deleted");
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertEquals(0, createStaplerMockedSut().getConfigs().size());
        project.delete();
        assertEquals(sut.getRevisionAmount(), sut.getConfigs(0, sut.getRevisionAmount()).size());
        assertEquals(1, createStaplerMockedSut().getConfigs().size());

    }
     */

    @Test
    public void testGetSingleConfigs_fromTo() throws Exception {
        given(mockedStaplerRequest.getRequestURI()).willReturn("/jenkins/" + JobConfigHistoryConsts.URLNAME + "/history");
        given(mockedStaplerRequest.getParameter("name")).willReturn("config");
        JobConfigHistoryRootAction sut = createStaplerMockedSut();


        assertEquals(3, sut.getSingleConfigs("config", 0, sut.getRevisionAmount()).size());
        assertEquals(2, sut.getSingleConfigs("config", 0, 2).size());

        //create a deleted project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        freeStyleProject.delete();
//        String deletedFolderName = PluginUtils.getHistoryDao().getDeletedJobs()[0].getName();
//        assertEquals(4, sut.getSingleConfigs(deletedFolderName, 0, sut.getRevisionAmount()).size());

    }

    /**
     * Test of getConfigs method, of class JobConfigHistoryRootAction.
     */
    /* TODO Fixme
    @Test
    public void testGetConfigs() throws Exception {
        jenkinsRule.createFreeStyleProject("Test1");

        given(mockedStaplerRequest.getParameter("filter")).willReturn(null);
        //either 8 or 9...
        assertTrue(createStaplerMockedSut().getConfigs().size() >= 8);
        given(mockedStaplerRequest.getParameter("filter")).willReturn("system");
        assertTrue(createStaplerMockedSut().getConfigs().size() >= 8);
        given(mockedStaplerRequest.getParameter("filter")).willReturn("all");
        assertTrue(createStaplerMockedSut().getConfigs().size() >= 10);
        given(mockedStaplerRequest.getParameter("filter")).willReturn("other");
        assertEquals(2, createStaplerMockedSut().getConfigs().size());
    }

     */

    /**
     * Test of getSystemConfigs method, of class JobConfigHistoryRootAction.
     * This is kind of an integration test, too, testing if root histories are saved.
     */
    @Test
    public void testGetSystemConfigs() throws Exception {
        JobConfigHistoryRootAction sut = createSut();
        //magic number. Seems to be there are 4 config changes on startup.
        assertEquals(3,
                sut.getSystemConfigs().stream()
                        .filter(configInfo -> configInfo.getJob().equals("config")).count()
        );

        //change the config once.
        JenkinsRule.WebClient jenkinsWebClient = jenkinsRule.createWebClient();
        HtmlPage configurePage = jenkinsWebClient.goTo("configure");
        HtmlForm configForm = configurePage.getFormByName("config");

        configForm.getTextAreaByName("system_message").setText("This is not an integration test");

        DomElement buttonDomElement = configurePage.getElementByName("Submit").getFirstElementChild().getFirstElementChild();
        if (!(buttonDomElement instanceof HtmlButton)) {
            fail("submit button not found in config page.");
        }
        HtmlButton configFormSubmit = (HtmlButton) buttonDomElement;
        configFormSubmit.click();

        assertEquals(4,
                sut.getSystemConfigs().stream()
                        .filter(configInfo -> configInfo.getJob().equals("config")).count()
        );
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetJobConfigs() throws Exception {
        //no projects
        assertEquals(0, createSut().getJobConfigs("").size());

        //one project (always 2 history entries on creation)
        jenkinsRule.createFreeStyleProject("Test1");
        assertEquals(2, createSut().getJobConfigs("").size());

        //one project and one deleted project.
        jenkinsRule.createFreeStyleProject("Test2").delete();
        assertEquals(6, createSut().getJobConfigs("").size());
        assertEquals(1, createSut().getJobConfigs("deleted").size());
    }

    /**
     * Test of getSingleConfigs method, of class JobConfigHistoryRootAction.
     */
    /* TODO Fixme
    @Test
    public void testGetSingleConfigs() throws Exception {
        assertEquals(4, createSut().getSingleConfigs("config").size());

        //create a deleted project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        freeStyleProject.delete();
        String deletedFolderName = PluginUtils.getHistoryDao().getDeletedJobs()[0].getName();
        assertEquals(4, createSut()
                .getSingleConfigs(deletedFolderName).size());

    }

     */

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileWithoutPermissions() throws Exception {
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
    public void testGetFile() throws Exception {
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

        //test equality
        assertEquals(result, result2);
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileDeleted() throws Exception {

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

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFilesDeleted() throws IOException, InterruptedException {
        //create a project
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        freeStyleProject.delete();


        //get the expected timestamp
        List<File> revisionsFromDeletedProject = Arrays.stream(PluginUtils.getHistoryDao().getDeletedJobs()[0].listFiles())
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());

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

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFiles() {
        final ConfigInfo config = mock(ConfigInfo.class);
        given(config.getJob()).willReturn("Test1");
        given(config.getDate()).willReturn("2012-11-21_11-42-05");
        given(config.getIsJob()).willReturn(true);
        String expectedRegex = "http://localhost:[0-9]{1,5}?/jenkins/job/Test1/jobConfigHistory/configOutput\\?type=xml&timestamp=[0-9\\-_]+";
        assertTrue(createSut().createLinkToFiles(config, "xml").matches(expectedRegex));
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFilesSystem() {
        final ConfigInfo config = mock(ConfigInfo.class);
        given(config.getJob()).willReturn("config");
        given(config.getDate()).willReturn("2012-11-21_11-42-05");
        given(config.getIsJob()).willReturn(false);
        String expResult = "configOutput?type=xml&name=config&timestamp=2012-11-21_11-42-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, "xml"));
    }

    /**
     * Test of getAccessControlledObject method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testGetAccessControlledObject() {
        assertEquals(jenkinsRule.getInstance(), createSut().getAccessControlledObject());
    }

    /**
     * Test of checkConfigurePermission method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testCheckConfigurePermission() {
        createSut().checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testHasConfigurePermission() {
        assertTrue(createSut().hasConfigurePermission());
    }

    /**
     * Test of hasJobConfigurePermission method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testHasJobConfigurePermission() {
        assertTrue(createSut().hasJobConfigurePermission());
    }

    /**
     * Test of checkParameters method, of class JobConfigHistoryRootAction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCheckParametersIAE() {
        createSut().checkParameters("foo", "bar");
    }

    /**
     * Test of checkParameters method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCheckParametersNameIsNull() {
        assertFalse(createSut().checkParameters(null, "2013-01-18_18-24-33"));
        assertFalse(createSut().checkParameters("null", "2013-01-18_18-24-33"));
    }

    /**
     * Test of checkParameters method, of class JobConfigHistoryRootAction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCheckParametersNameHasDots() {
        createSut().checkParameters("../foo", "2013-01-18_18-24-33");
    }

    /**
     * Test of checkParameters method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCheckParameters() {
        assertTrue(createSut().checkParameters("foo", "2013-01-18_18-24-33"));
    }

    /**
     * Test of doDiffFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testDoDiffFiles() throws Exception {
        final String boundary = "AAAA";
        given(mockedStaplerRequest.getContentType())
                .willReturn("multipart/form-data; boundary=" + boundary);
        given(mockedStaplerRequest.getInputStream()).willReturn(
                TUtils.createServletInputStreamFromMultiPartFormData(boundary));
        StaplerResponse rsp = mock(StaplerResponse.class);
        JobConfigHistoryRootAction sut = createStaplerMockedSut();
        sut.doDiffFiles(mockedStaplerRequest, rsp);
        verify(rsp).sendRedirect(
                "showDiffFiles?name=foo&timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLines() throws Exception {

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");

        freeStyleProject.setDescription("newDescription");
        freeStyleProject.setJDK(new JDK("name", "javahome"));
        testLines(0, freeStyleProject, 0, 0, false);
        testLines(0, freeStyleProject, 0, 1, false);
        testLines(7, freeStyleProject, 1, 2, false);
//        testLines(6, freeStyleProject, 2, 3, false);
    }

    private void testLines(long expected, FreeStyleProject freeStyleProject, int i, int j, boolean hideVersionDiffs) throws IOException {
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        assertTrue("i or j are too high", timestamps.size() > Math.max(i, j));

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

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLinesNoPermissions() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("Test1");
        assertEquals(0, createUnauthorizedStaplerMockedSut().getLines().size());

    }

    /**
     * Test of findNewName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testFindNewName() throws Exception {
        FreeStyleProject test1 = jenkinsRule.createFreeStyleProject("Test1");
        assertEquals("Test1_1", createSut().findNewName("Test1"));

        jenkinsRule.createFreeStyleProject("Test1_1");
        assertEquals("Test1_2", createSut().findNewName("Test1"));

        test1.delete();
        assertEquals("Test1", createSut().findNewName("Test1"));
    }

    /**
     * Test of getOldConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetOldConfigXml() throws IOException, InterruptedException {
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

    /**
     * Test of getOldConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetOldConfigXmlNonExisting() {
        final String name = "jobs/I_DO_NOT_EXIST";
        final String timestamp = "2012-11-21_11-35-12";
        createSut().getOldConfigXml(name, timestamp);
    }

    /**
     * Test of doRestore method, of class JobConfigHistoryRootAction.
     */
    /* TODO Fixme
    @Test
    public void testDoRestore() throws Exception {
        final StaplerRequest req = mock(StaplerRequest.class);
        final StaplerResponse rsp = mock(StaplerResponse.class);

        JobConfigHistoryRootAction sut = createSut();

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        assertNotNull(jenkinsRule.getInstance().getItem("Test1"));
        assertEquals("Test1", jenkinsRule.getInstance().getItem("Test1").getName());

        freeStyleProject.delete();
        assertNull(jenkinsRule.getInstance().getItem("Test1"));
        final String deletedTestProjectName = PluginUtils.getHistoryDao().getDeletedJobs()[0].getName();

        given(req.getParameter("name")).willReturn(deletedTestProjectName);
        sut.doRestore(req, rsp);
        assertNotNull(jenkinsRule.getInstance().getItem("Test1"));
        assertEquals("Test1", jenkinsRule.getInstance().getItem("Test1").getName());
    }
     */

    /**
     * Test of getLastAvailableConfigXml method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLastAvailableConfigXml() throws IOException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        List<String> timestamps = getCreatedTimestamps(freeStyleProject);
        final String timestamp = timestamps.get(0);
        final String expectedSuffix = "Test1" + File.separator + timestamp + File.separator + "config.xml";

        String name = "jobs/Test1";
        assertThat(
                createSut().getLastAvailableConfigXml(name).getFile().getPath(),
                TUtils.pathEndsWith(expectedSuffix));
    }

    /**
     * Test of getLastAvailableConfigXml method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLastAvailableConfigXmlNoConfigs() {
        String name = "jobs/I_DO_NOT_EXIST";
        assertNull(createSut().getLastAvailableConfigXml(name));
    }

    /**
     * Test of doForwardToRestoreQuestion method, of class
     * JobConfigHistoryRootAction.
     */
    @Test
    public void testDoForwardToRestoreQuestion() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("foo");
        StaplerResponse mockedResponse = mock(StaplerResponse.class);
        JobConfigHistoryRootAction sut = createStaplerMockedSut();
        sut.doForwardToRestoreQuestion(mockedStaplerRequest, mockedResponse);
        verify(mockedResponse).sendRedirect("restoreQuestion?name=foo");
    }

    JobConfigHistoryRootAction createSut() {
        return new JobConfigHistoryRootAction();
    }

    JobConfigHistoryRootAction createStaplerMockedSut() {
        return new JobConfigHistoryRootAction() {
            @Override
            protected StaplerRequest getCurrentRequest() {
                return mockedStaplerRequest;
            }
        };
    }

    JobConfigHistoryRootAction createUnauthorizedStaplerMockedSut() {
        return new JobConfigHistoryRootAction() {
            @Override
            protected StaplerRequest getCurrentRequest() {
                return mockedStaplerRequest;
            }

            @Override
            public AccessControlled getAccessControlledObject() {
                return mock(Jenkins.class);
            }
        };
    }

}
