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

import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import static org.mockito.BDDMockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryRootActionTest {

    @ClassRule
    public final static UnpackResourceZip UNPACK_RESOURCE_ZIP = UnpackResourceZip.create();

    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Hudson mockedHudson = mock(Hudson.class);
    private final ACL mockedACL = mock(ACL.class);
    private final StaplerRequest mockedStaplerRequest = mock(StaplerRequest.class);
    private final File configHistory = UNPACK_RESOURCE_ZIP.getResource("config-history");

    public JobConfigHistoryRootActionTest() {
    }

    @Before
    public void setFieldsFromUnpackResource() {
        given(mockedHudson.getACL()).willReturn(mockedACL);
    }

    /**
     * Test of getUrlName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetUrlName() {
        JobConfigHistoryRootAction sut = createSut();
        String expResult = "/jobConfigHistory";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileNameWithoutPermissions() {
        assertNull(createSut().getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileName() {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        assertNotNull(createSut().getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileNameWithJobPermission() {
        given(mockedACL.hasPermission(Item.CONFIGURE)).willReturn(true);
        assertNotNull(createSut().getIconFileName());
    }

    /**
     * Test of getConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetConfigs() throws Exception {
        given(mockedStaplerRequest.getParameter("filter")).willReturn(null);
        assertEquals(0, createSut().getConfigs().size());
        given(mockedStaplerRequest.getParameter("filter")).willReturn("system");
        assertEquals(0, createSut().getConfigs().size());
        given(mockedStaplerRequest.getParameter("filter")).willReturn("all");
        assertEquals(0, createSut().getConfigs().size());
        given(mockedStaplerRequest.getParameter("filter")).willReturn("other");
        assertEquals(0, createSut().getConfigs().size());
    }

    /**
     * Test of getSystemConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetSystemConfigs() throws Exception {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        assertEquals(5, createSut().getSystemConfigs().size());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetJobConfigs() throws Exception {
        given(mockedACL.hasPermission(Item.CONFIGURE)).willReturn(true);
        assertEquals(8, createSut().getJobConfigs("").size());
        assertEquals(1, createSut().getJobConfigs("deleted").size());
    }

    /**
     * Test of getSingleConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetSingleConfigs() throws Exception {
        given(mockedPlugin.getConfiguredHistoryRootDir()).willReturn(configHistory.toURI().toURL());
        assertEquals(5, createSut().getSingleConfigs("config").size());
        assertEquals(3, createSut().getSingleConfigs("Foo_deleted_20130830_223932_071").size());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileWithoutPermissions() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("jobs/Test1");
        assertEquals("No permission to view config files", createSut().getFile());
        given(mockedStaplerRequest.getParameter("name")).willReturn("Foo_deleted_20130830_223932_071");
        assertEquals("No permission to view config files", createSut().getFile());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFile() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("jobs/Test1");
        given(mockedStaplerRequest.getParameter("timestamp")).willReturn("2012-11-21_11-42-05");
        given(mockedPlugin.getConfiguredHistoryRootDir()).willReturn(configHistory.toURI().toURL());
        given(mockedPlugin.getConfigFile(any(File.class))).willReturn(
                UNPACK_RESOURCE_ZIP.getResource("config-history/jobs/Test1/2012-11-21_11-42-05/config.xml"));
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        final String result = createSut().getFile();
        assertTrue(result.startsWith("<?xml"));
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileDeleted() throws Exception {
        final String name = "Foo_deleted_20130830_223932_071";
        given(mockedStaplerRequest.getParameter("name")).willReturn(name);
        final String timestamp = "2013-08-30_22-39-32";
        given(mockedStaplerRequest.getParameter("timestamp")).willReturn(timestamp);
        final String jobHistoryRoot = "config-history/jobs";
        given(mockedPlugin.getConfigFile(any(File.class))).willReturn(
                UNPACK_RESOURCE_ZIP.getResource(jobHistoryRoot + "/" + name + "/" + timestamp + "/config.xml"));
        given(mockedACL.hasPermission(Item.CONFIGURE)).willReturn(true);
        final String result = createSut().getFile();
        assertTrue(result.startsWith("<?xml"));
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFilesDeleted() {
        final ConfigInfo config = mock(ConfigInfo.class);
        given(config.getJob()).willReturn("Foo_deleted_20130830_223932_071");
        given(config.getDate()).willReturn("2013-08-30_22-39-32");
        String expResult = "configOutput?type=&name=Foo_deleted_20130830_223932_071&timestamp=2013-08-30_22-35-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, ""));
        given(config.getJob()).willReturn("Unknown_deleted_20130830_223932_072");
        assertEquals(null, createSut().createLinkToFiles(config, ""));
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
        given(mockedHudson.getRootUrl()).willReturn("/jenkins/");
        String expResult = "/jenkins/job/Test1/jobConfigHistory/configOutput?type=xml&timestamp=2012-11-21_11-42-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, "xml"));
        given(config.getJob()).willReturn("config");
        given(config.getDate()).willReturn("2012-11-21_11-42-05");
        given(config.getIsJob()).willReturn(false);
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
     * Test of getAccessControlledObject method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetAccessControlledObject() {
        assertEquals(mockedHudson, createSut().getAccessControlledObject());
    }

    /**
     * Test of checkConfigurePermission method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCheckConfigurePermission() {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        createSut().checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testHasConfigurePermission() {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        assertTrue(createSut().hasConfigurePermission());
    }

    /**
     * Test of hasJobConfigurePermission method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testHasJobConfigurePermission() {
        given(mockedACL.hasPermission(Item.CONFIGURE)).willReturn(true);
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
        given(mockedStaplerRequest.getContentType()).willReturn("multipart/form-data; boundary=" + boundary);
        given(mockedStaplerRequest.getInputStream()).willReturn(TUtils.createServletInputStreamFromMultiPartFormData(boundary));
        StaplerResponse rsp = mock(StaplerResponse.class);
        JobConfigHistoryRootAction sut = createSut();
        sut.doDiffFiles(mockedStaplerRequest, rsp);
        verify(rsp).sendRedirect("showDiffFiles?name=foo&timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLines() throws Exception {
        final String name = "jobs/Test1";
        final String timestamp1 = "2012-11-21_11-29-12";
        final String timestamp2 = "2012-11-21_11-35-12";

        final File jobHistory = new File(configHistory, name);
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true, true);
        given(mockedPlugin.getConfiguredHistoryRootDir()).willReturn(configHistory.toURI().toURL());
        given(mockedPlugin.getConfigFile(any(File.class))).willReturn(
                new File(jobHistory, "2012-11-21_11-29-12/config.xml"),
                new File(jobHistory, "2012-11-21_11-35-12/config.xml"));
        given(mockedStaplerRequest.getParameter("name")).willReturn(name);
        given(mockedStaplerRequest.getParameter("timestamp1")).willReturn(timestamp1);
        given(mockedStaplerRequest.getParameter("timestamp2")).willReturn(timestamp2);
        final List<SideBySideView.Line> result = createSut().getLines();
        assertEquals(8, result.size());

    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLinesNoPermissions() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("Test1");
        assertEquals(0, createSut().getLines().size());

    }

    /**
     * Test of findNewName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testFindNewName() throws Exception {
        given(mockedHudson.getItem("foo")).willReturn(null);
        assertEquals("foo", createSut().findNewName("foo"));
        given(mockedHudson.getItem("foo")).willReturn(mock(TopLevelItem.class));
        assertEquals("foo_1", createSut().findNewName("foo"));
        given(mockedHudson.getItem("foo")).willReturn(mock(TopLevelItem.class));
        given(mockedHudson.getItem("foo_1")).willReturn(mock(TopLevelItem.class));
        assertEquals("foo_2", createSut().findNewName("foo"));
    }

    /**
     * Test of getOldConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetOldConfigXml() {
        final String name = "jobs/Test1";
        final String timestamp = "2012-11-21_11-35-12";
        final File jobConfig = new File(configHistory, name + "/" + timestamp + "/config.xml");
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        assertEquals(jobConfig, createSut().getOldConfigXml(name, timestamp).getFile());
    }

    /**
     * Test of getOldConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetOldConfigXmlNonExisting() {
        final String name = "jobs/I_DO_NOT_EXIST";
        final String timestamp = "2012-11-21_11-35-12";
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        createSut().getOldConfigXml(name, timestamp);
    }

    /**
     * Test of doRestore method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testDoRestore() throws Exception {
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryRootAction sut = createSut();
        sut.doRestore(req, rsp);
    }

    /**
     * Test of getLastAvailableConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLastAvailableConfigXml() {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        String name = "jobs/Test1";
        assertThat(createSut().getLastAvailableConfigXml(name).getFile().getPath(),
                TUtils.pathEndsWith("Test1/2012-11-21_11-41-14/config.xml"));
    }

    /**
     * Test of getLastAvailableConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLastAvailableConfigXmlNoConfigs() {
        given(mockedACL.hasPermission(Permission.CONFIGURE)).willReturn(true);
        String name = "jobs/I_DO_NOT_EXIST";
        assertNull(createSut().getLastAvailableConfigXml(name));
    }

    /**
     * Test of doForwardToRestoreQuestion method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testDoForwardToRestoreQuestion() throws Exception {
        given(mockedStaplerRequest.getParameter("name")).willReturn("foo");
        StaplerResponse mockedResponse = mock(StaplerResponse.class);
        JobConfigHistoryRootAction sut = createSut();
        sut.doForwardToRestoreQuestion(mockedStaplerRequest, mockedResponse);
        verify(mockedResponse).sendRedirect("restoreQuestion?name=foo");
    }

    JobConfigHistoryRootAction createSut() {
        return new JobConfigHistoryRootAction() {
            @Override
            JobConfigHistory getPlugin() {
                return mockedPlugin;
            }

            @Override
            protected Hudson getHudson() {
                return mockedHudson;
            }

            @Override
            StaplerRequest getCurrentRequest() {
                return mockedStaplerRequest;
            }

            @Override
            OverviewHistoryDao getOverviewHistoryDao() {
                return new FileHistoryDao(
                        UNPACK_RESOURCE_ZIP.getResource("config-history"),
                        UNPACK_RESOURCE_ZIP.getRoot(),
                        null,
                        0,
                        true);
            }

            @Override
            HistoryDao getHistoryDao() {
                return new FileHistoryDao(
                        UNPACK_RESOURCE_ZIP.getResource("config-history"),
                        UNPACK_RESOURCE_ZIP.getRoot(),
                        null,
                        0,
                        true);
            }
        };
    }


}
