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
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.util.List;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import static org.mockito.Mockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryRootActionTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.INSTANCE;

    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final Hudson mockedHudson = mock(Hudson.class);
    private final ACL mockedACL = mock(ACL.class);
    private final StaplerRequest mockedStaplerRequest = mock(StaplerRequest.class);

    public JobConfigHistoryRootActionTest() {
    }

    @Before
    public void setFieldsFromUnpackResource() {
        when(mockedHudson.getACL()).thenReturn(mockedACL);
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
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true);
        assertNotNull(createSut().getIconFileName());
    }

    /**
     * Test of getIconFileName method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetIconFileNameWithJobPermission() {
        when(mockedACL.hasPermission(Item.CONFIGURE)).thenReturn(true);
        assertNotNull(createSut().getIconFileName());
    }

    /**
     * Test of getConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetConfigs() throws Exception {
        when(mockedStaplerRequest.getParameter("filter")).thenReturn(null);
        assertEquals(0, createSut().getConfigs().size());
        when(mockedStaplerRequest.getParameter("filter")).thenReturn("system");
        assertEquals(0, createSut().getConfigs().size());
        when(mockedStaplerRequest.getParameter("filter")).thenReturn("all");
        assertEquals(0, createSut().getConfigs().size());
        when(mockedStaplerRequest.getParameter("filter")).thenReturn("other");
        assertEquals(0, createSut().getConfigs().size());
    }

    /**
     * Test of getSystemConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetSystemConfigs() throws Exception {
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true);
        assertEquals(5, createSut().getSystemConfigs().size());
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetJobConfigs() throws Exception {
        when(mockedACL.hasPermission(Item.CONFIGURE)).thenReturn(true);
        assertEquals(8, createSut().getJobConfigs("").size());
        assertEquals(1, createSut().getJobConfigs("deleted").size());
    }

    /**
     * Test of getSingleConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetSingleConfigs() throws Exception {
        when(mockedPlugin.getJobHistoryRootDir()).thenReturn(unpackResourceZip.getResource("config-history/jobs"));
        when(mockedPlugin.getConfiguredHistoryRootDir()).thenReturn(unpackResourceZip.getResource("config-history"));
        assertEquals(5, createSut().getSingleConfigs("config").size());
        assertEquals(3, createSut().getSingleConfigs("Foo_deleted_20130830_223932_071").size());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileWithoutPermissions() throws Exception {
        when(mockedStaplerRequest.getParameter("name")).thenReturn("jobs/Test1");
        assertEquals("No permission to view config files", createSut().getFile());
        when(mockedStaplerRequest.getParameter("name")).thenReturn("Foo_deleted_20130830_223932_071");
        assertEquals("No permission to view config files", createSut().getFile());
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFile() throws Exception {
        when(mockedStaplerRequest.getParameter("name")).thenReturn("jobs/Test1");
        when(mockedStaplerRequest.getParameter("timestamp")).thenReturn("2012-11-21_11-42-05");
        when(mockedPlugin.getConfiguredHistoryRootDir()).thenReturn(unpackResourceZip.getResource("config-history"));
        when(mockedPlugin.getConfigFile(any(File.class))).thenReturn(
                unpackResourceZip.getResource("config-history/jobs/Test1/2012-11-21_11-42-05/config.xml"));
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true);
        final String result = createSut().getFile();
        assertTrue(result.startsWith("<?xml"));
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetFileDeleted() throws Exception {
        final String name = "Foo_deleted_20130830_223932_071";
        when(mockedStaplerRequest.getParameter("name")).thenReturn(name);
        final String timestamp = "2013-08-30_22-39-32";
        when(mockedStaplerRequest.getParameter("timestamp")).thenReturn(timestamp);
        final String jobHistoryRoot = "config-history/jobs";
        when(mockedPlugin.getJobHistoryRootDir()).thenReturn(unpackResourceZip.getResource(jobHistoryRoot));
        when(mockedPlugin.getConfigFile(any(File.class))).thenReturn(
                unpackResourceZip.getResource(jobHistoryRoot + "/" + name + "/" + timestamp + "/config.xml"));
        when(mockedACL.hasPermission(Item.CONFIGURE)).thenReturn(true);
        final String result = createSut().getFile();
        assertTrue(result.startsWith("<?xml"));
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFilesDeleted() {
        final ConfigInfo config = mock(ConfigInfo.class);
        when(config.getJob()).thenReturn("Foo_deleted_20130830_223932_071");
        when(config.getDate()).thenReturn("2013-08-30_22-39-32");
        when(mockedPlugin.getJobHistoryRootDir()).thenReturn(unpackResourceZip.getResource("config-history/jobs"));
        String expResult = "configOutput?type=&name=Foo_deleted_20130830_223932_071&timestamp=2013-08-30_22-35-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, ""));
        when(config.getJob()).thenReturn("Unknown_deleted_20130830_223932_072");
        assertEquals(null, createSut().createLinkToFiles(config, ""));
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFiles() {
        final ConfigInfo config = mock(ConfigInfo.class);
        when(config.getJob()).thenReturn("Test1");
        when(config.getDate()).thenReturn("2012-11-21_11-42-05");
        when(config.getIsJob()).thenReturn(true);
        when(mockedHudson.getRootUrl()).thenReturn("/jenkins/");
        String expResult = "/jenkins/job/Test1/jobConfigHistory/configOutput?type=xml&timestamp=2012-11-21_11-42-05";
        assertEquals(expResult, createSut().createLinkToFiles(config, "xml"));
        when(config.getJob()).thenReturn("config");
        when(config.getDate()).thenReturn("2012-11-21_11-42-05");
        when(config.getIsJob()).thenReturn(false);
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testCreateLinkToFilesSystem() {
        final ConfigInfo config = mock(ConfigInfo.class);
        when(config.getJob()).thenReturn("config");
        when(config.getDate()).thenReturn("2012-11-21_11-42-05");
        when(config.getIsJob()).thenReturn(false);
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
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true);
        createSut().checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testHasConfigurePermission() {
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true);
        assertTrue(createSut().hasConfigurePermission());
    }

    /**
     * Test of hasJobConfigurePermission method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testHasJobConfigurePermission() {
        when(mockedACL.hasPermission(Item.CONFIGURE)).thenReturn(true);
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
        when(mockedStaplerRequest.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);
        when(mockedStaplerRequest.getInputStream()).thenReturn(TUtils.createServletInputStreamFromMultiPartFormData(boundary));
        StaplerResponse rsp = mock(StaplerResponse.class);
        JobConfigHistoryRootAction sut = createSut();
        sut.doDiffFiles(mockedStaplerRequest, rsp);
        verify(rsp).sendRedirect("showDiffFiles?name=foo&timestamp1=111&timestamp2=112");
    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLines() throws Exception {
        final String name = "jobs/Test1";
        final String timestamp1 = "2012-11-21_11-29-12";
        final String timestamp2 = "2012-11-21_11-35-12";
        final File configHistory = unpackResourceZip.getResource("config-history");
        final File jobHistory = new File(configHistory, name);
        when(mockedACL.hasPermission(Permission.CONFIGURE)).thenReturn(true, true);
        when(mockedPlugin.getConfiguredHistoryRootDir()).thenReturn(configHistory);
        when(mockedPlugin.getConfigFile(any(File.class))).thenReturn(
                new File(jobHistory, "2012-11-21_11-29-12/config.xml"),
                new File(jobHistory, "2012-11-21_11-35-12/config.xml"));
        when(mockedStaplerRequest.getParameter("name")).thenReturn(name);
        when(mockedStaplerRequest.getParameter("timestamp1")).thenReturn(timestamp1);
        when(mockedStaplerRequest.getParameter("timestamp2")).thenReturn(timestamp2);
        final List<SideBySideView.Line> result = createSut().getLines();
        assertEquals(8, result.size());

    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    public void testGetLinesNoPermissions() throws Exception {
        when(mockedStaplerRequest.getParameter("name")).thenReturn("Test1");
        assertEquals(0, createSut().getLines().size());

    }

    /**
     * Test of getOldConfigXml method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetOldConfigXml() {
        String name = "";
        String timestamp = "";
        JobConfigHistoryRootAction sut = createSut();
        XmlFile expResult = null;
        XmlFile result = sut.getOldConfigXml(name, timestamp);
        assertEquals(expResult, result);
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
    @Ignore
    public void testGetLastAvailableConfigXml() {
        String name = "";
        JobConfigHistoryRootAction sut = createSut();
        XmlFile expResult = null;
        XmlFile result = sut.getLastAvailableConfigXml(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of doForwardToRestoreQuestion method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testDoForwardToRestoreQuestion() throws Exception {
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryRootAction sut = createSut();
        sut.doForwardToRestoreQuestion(req, rsp);
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
                        unpackResourceZip.getResource("config-history"),
                        unpackResourceZip.getRoot(),
                        null,
                        0,
                        true);
            }
        };
    }


}
