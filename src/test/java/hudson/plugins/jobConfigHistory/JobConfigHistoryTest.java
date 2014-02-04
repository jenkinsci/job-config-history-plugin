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
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.security.ACL;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.create();

    private final User mockedUser = mock(User.class);


    /**
     * Test of configure method, of class JobConfigHistory.
     */
    @Test
    public void testConfigure() throws Exception {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
    }

    /**
     * Test of getHistoryRootDir method, of class JobConfigHistory.
     */
    @Test
    public void testGetHistoryRootDir() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
        assertThat(sut.getHistoryRootDir(), endsWith("config-history"));
    }

    /**
     * Test of getDefaultRootDir method, of class JobConfigHistory.
     */
    @Test
    public void testGetDefaultRootDir() {
        JobConfigHistory sut = createSut();
        assertThat(sut.getDefaultRootDir(), endsWith("config-history"));
    }

    /**
     * Test of getMaxHistoryEntries method, of class JobConfigHistory.
     */
    @Test
    public void testGetMaxHistoryEntries() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
        String expResult = "5";
        String result = sut.getMaxHistoryEntries();
        assertEquals(expResult, result);
    }

    /**
     * Test of setMaxHistoryEntries method, of class JobConfigHistory.
     */
    @Test
    public void testSetMaxHistoryEntries() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("");
        assertEquals("", sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("4");
        assertEquals("4", sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("-2");
        assertEquals("4", sut.getMaxHistoryEntries());
    }
    
    
    /**
     * Test of getEntriesPerSite method, of class JobConfigHistory.
     */
    @Test
    public void testGetEntriesPerSite() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
        String expResult = "50";
        String result = sut.getMaxEntriesPerPage();
        assertEquals(expResult, result);
    }

    /**
     * Test of setMaxEntriesPerSite method, of class JobConfigHistory.
     */
    @Test
    public void testSetMaxEntriesPerSite() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("");
        assertEquals("", sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("50");
        assertEquals("50", sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("-2");
        assertEquals("50", sut.getMaxEntriesPerPage());
    }

    /**
     * Test of getMaxDaysToKeepEntries method, of class JobConfigHistory.
     */
    @Test
    public void testGetMaxDaysToKeepEntries() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
        assertEquals("5", sut.getMaxDaysToKeepEntries());
    }

    /**
     * Test of setMaxDaysToKeepEntries method, of class JobConfigHistory.
     */
    @Test
    public void testSetMaxDaysToKeepEntries() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("");
        assertEquals("", sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("4");
        assertEquals("4", sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("-1");
        assertEquals("4", sut.getMaxDaysToKeepEntries());
    }

    /**
     * Test of isPositiveInteger method, of class JobConfigHistory.
     */
    @Test
    public void testIsPositiveInteger() {
        JobConfigHistory sut = createSut();
        assertFalse(sut.isPositiveInteger(""));
        assertFalse(sut.isPositiveInteger("-1"));
        assertTrue(sut.isPositiveInteger("0"));
        assertTrue(sut.isPositiveInteger("1"));

    }

    /**
     * Test of getSaveItemGroupConfiguration method, of class JobConfigHistory.
     */
    @Test
    public void testGetSaveItemGroupConfiguration() {
        JobConfigHistory sut = createSut();
        boolean expResult = false;
        boolean result = sut.getSaveItemGroupConfiguration();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSkipDuplicateHistory method, of class JobConfigHistory.
     */
    @Test
    public void testGetSkipDuplicateHistory() {
        JobConfigHistory sut = createSut();
        boolean expResult = true;
        boolean result = sut.getSkipDuplicateHistory();
        assertEquals(expResult, result);
    }

    /**
     * Test of getExcludePattern method, of class JobConfigHistory.
     */
    @Test
    public void testGetExcludePattern() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        sut.configure(null, createFormData());
        String expResult = "5";
        String result = sut.getExcludePattern();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultExcludePattern method, of class JobConfigHistory.
     */
    @Test
    public void testGetDefaultExcludePattern() {
        JobConfigHistory sut = createSut();
        String expResult = JobConfigHistoryConsts.DEFAULT_EXCLUDE;
        String result = sut.getDefaultExcludePattern();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSaveModuleConfiguration method, of class JobConfigHistory.
     */
    @Test
    public void testGetSaveModuleConfiguration() {
        JobConfigHistory sut = createSut();
        boolean result = sut.getSaveModuleConfiguration();
        assertTrue(result);
    }

    /**
     * Test of getShowBuildBadges method, of class JobConfigHistory.
     */
    @Test
    public void testGetShowBuildBadges() {
        JobConfigHistory sut = createSut();
        String expResult = "always";
        String result = sut.getShowBuildBadges();
        assertEquals(expResult, result);
    }

    /**
     * Test of showBuildBadges method, of class JobConfigHistory.
     */
    @Test
    public void testShowBuildBadgesAlways() {
        AbstractProject mockedProject = mock(AbstractProject.class);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("always");
        assertTrue(sut.showBuildBadges(mockedProject));
    }

    /**
     * Test of showBuildBadges method, of class JobConfigHistory.
     */
    @Test
    public void testShowBuildBadgesUserWithConfigPermission() {
        AbstractProject mockedProject = mock(AbstractProject.class);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true, false);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("userWithConfigPermission");
        assertTrue(sut.showBuildBadges(mockedProject));
        assertFalse(sut.showBuildBadges(mockedProject));
    }

    /**
     * Test of showBuildBadges method, of class JobConfigHistory.
     */
    @Test
    public void testShowBuildBadgesAdminUser() {
        JobConfigHistory sut = createSut();
        AbstractProject mockedProject = mock(AbstractProject.class);
        final ACL mockedACL = mock(ACL.class);
        when(mockedACL.hasPermission(Jenkins.ADMINISTER)).thenReturn(true, false);
        when(sut.getJenkins().getACL()).thenReturn(mockedACL, mockedACL);
        sut.setShowBuildBadges("adminUser");
        assertTrue(sut.showBuildBadges(mockedProject));
        assertFalse(sut.showBuildBadges(mockedProject));
    }

    /**
     * Test of getExcludeRegexpPattern method, of class JobConfigHistory.
     */
    @Test
    public void testGetExcludeRegexpPattern() {
        JobConfigHistory sut = createSut();
        Pattern expResult = null;
        Pattern result = sut.getExcludeRegexpPattern();
        assertEquals(expResult, result);
    }

    /**
     * Test of getConfiguredHistoryRootDir method, of class JobConfigHistory.
     */
    @Test
    public void testGetConfiguredHistoryRootDir() throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createSut();
        assertThat(sut.getConfiguredHistoryRootDir().getPath(), endsWith("config-history"));
        final JSONObject formData = createFormData();
        sut.configure(null, formData);
        assertThat(sut.getConfiguredHistoryRootDir().getPath(), endsWith("config-history"));
        formData.put("historyRootDir", "");
        sut.configure(null, formData);
        assertThat(sut.getConfiguredHistoryRootDir().getPath(), endsWith("config-history"));
        formData.put("historyRootDir", "/tmp/");
        sut.configure(null, formData);
        assertThat(sut.getConfiguredHistoryRootDir().getPath(), endsWith("config-history"));
    }

    /**
     * Test of getConfigFile method, of class JobConfigHistory.
     */
    @Test
    public void testGetConfigFile() {
        final String dirName = "config-history/config/2013-01-18_17-34-22/";
        File historyDir = unpackResourceZip.getResource(dirName);
        JobConfigHistory sut = createSut();
        File result = sut.getConfigFile(historyDir);
        assertThat(result.getPath(), endsWith(dirName + "config.xml"));
    }

    /**
     * Test of isSaveable method, of class JobConfigHistory.
     */
    @Test
    public void testIsSaveable() throws IOException, ServletException, Descriptor.FormException {
        XmlFile xmlFile = new XmlFile(unpackResourceZip.getResource("jobs/Test1/config.xml"));
        JobConfigHistory sut = createSut();
        final JSONObject formData = createFormData();

        sut.configure(null, formData);
        assertTrue(sut.isSaveable(mock(AbstractProject.class), xmlFile));
        assertTrue(sut.isSaveable(null, new XmlFile(unpackResourceZip.getResource("config.xml"))));
        assertTrue(sut.isSaveable(mock(ItemGroup.class), xmlFile));
        assertFalse(sut.isSaveable(null, xmlFile));

        formData.put("saveItemGroupConfiguration", false);
        sut.configure(null, formData);
        assertFalse(sut.isSaveable(mock(ItemGroup.class), xmlFile));
    }

    /**
     * Test of doCheckMaxHistoryEntries method, of class JobConfigHistory.
     */
    @Test
    public void testDoCheckMaxHistoryEntries() {
        JobConfigHistory sut = createSut();
        final FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries("5"));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("A"));
    }

    /**
     * Test of doCheckMaxDaysToKeepEntries method, of class JobConfigHistory.
     */
    @Test
    public void testDoCheckMaxDaysToKeepEntries() {
        JobConfigHistory sut = createSut();
        final FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("5"));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("A"));
    }

    /**
     * Test of doCheckExcludePattern method, of class JobConfigHistory.
     */
    @Test
    public void testDoCheckExcludePattern() {
        JobConfigHistory sut = createSut();
        FormValidation expResult = FormValidation.ok();
        assertEquals(expResult, sut.doCheckExcludePattern(""));
        assertNotEquals(expResult, sut.doCheckExcludePattern("[.*"));
    }

    private JobConfigHistory createSut() {
        return new JobConfigHistory() {

            final Jenkins mockedJenkins = mock(Jenkins.class);

            @Override
            HistoryDao getHistoryDao() {
                return new FileHistoryDao(
                        unpackResourceZip.getResource("config-history"), getJenkinsHome(), mockedUser, 0, false);
            }

            @Override
            File getJenkinsHome() {
                return unpackResourceZip.getRoot();
            }

            @Override
            Jenkins getJenkins() {
                return mockedJenkins;
            }

            @Override
            public void save() throws IOException {
                //
            }

        };
    }

    JSONObject createFormData() {
        return JSONObject.fromObject(
        "{"+
                "\"historyRootDir\": \"" + unpackResourceZip.getResource("config-history").getPath() + "\"," +
                "\"maxHistoryEntries\": \"5\"," +
                "\"maxDaysToKeepEntries\": \"5\"," +
                "\"maxEntriesPerPage\": \"50\"," +
                "\"saveItemGroupConfiguration\": true," +
                "\"skipDuplicateHistory\": true," +
                "\"excludePattern\": \"5\"," +
                "\"saveModuleConfiguration\": true," +
                "\"showBuildBadges\": \"5\"" +
        "}");
    }
}
