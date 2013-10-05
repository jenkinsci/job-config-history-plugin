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
     * Test of getConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetConfigs() throws Exception {
        JobConfigHistoryRootAction sut = createSut();
        List<ConfigInfo> expResult = null;
        List<ConfigInfo> result = sut.getConfigs();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSystemConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetSystemConfigs() throws Exception {
        JobConfigHistoryRootAction sut = createSut();
        List<ConfigInfo> expResult = null;
        List<ConfigInfo> result = sut.getSystemConfigs();
        assertEquals(expResult, result);
    }

    /**
     * Test of getJobConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetJobConfigs() throws Exception {
        String type = "";
        JobConfigHistoryRootAction sut = createSut();
        List<ConfigInfo> expResult = null;
        List<ConfigInfo> result = sut.getJobConfigs(type);
        assertEquals(expResult, result);
    }

    /**
     * Test of getSingleConfigs method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetSingleConfigs() throws Exception {
        String name = "";
        JobConfigHistoryRootAction sut = createSut();
        List<ConfigInfo> expResult = null;
        List<ConfigInfo> result = sut.getSingleConfigs(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFile method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetFile() throws Exception {
        JobConfigHistoryRootAction sut = createSut();
        String expResult = "";
        String result = sut.getFile();
        assertEquals(expResult, result);
    }

    /**
     * Test of createLinkToFiles method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testCreateLinkToFiles() {
        ConfigInfo config = null;
        String type = "";
        JobConfigHistoryRootAction sut = createSut();
        String expResult = "";
        String result = sut.createLinkToFiles(config, type);
        assertEquals(expResult, result);
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
    @Ignore
    public void testDoDiffFiles() throws Exception {
        StaplerRequest req = null;
        StaplerResponse rsp = null;
        JobConfigHistoryRootAction sut = createSut();
        sut.doDiffFiles(req, rsp);
    }

    /**
     * Test of getLines method, of class JobConfigHistoryRootAction.
     */
    @Test
    @Ignore
    public void testGetLines() throws Exception {
        JobConfigHistoryRootAction sut = createSut();
        List<SideBySideView.Line> expResult = null;
        List<SideBySideView.Line> result = sut.getLines();
        assertEquals(expResult, result);

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

        };
    }


}
