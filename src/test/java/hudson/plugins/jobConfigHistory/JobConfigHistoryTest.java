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
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistory.
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.create();
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    private StaplerRequest mockStaplerRequest() throws ServletException {
        Stapler stapler = mock(Stapler.class, CALLS_REAL_METHODS);
        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
        stapler.init(servletConfig);
        HttpServletRequest rawRequest = mock(HttpServletRequest.class);
        return new RequestImpl(stapler, rawRequest, Collections.emptyList(), null);
    }

    @Test
    public void testConfigure() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
    }

    @Test
    public void testGetHistoryRootDir() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        assertThat(sut.getHistoryRootDir(), endsWith("config-history"));
    }

    @Test
    public void testGetDefaultRootDir() {
        JobConfigHistory sut = createSut();
        assertThat(sut.getDefaultRootDir(), endsWith("config-history"));
    }

    @Test
    public void testGetMaxHistoryEntries() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String expResult = "5";
        String result = sut.getMaxHistoryEntries();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetMaxHistoryEntries() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries(null);
        assertNull(sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("");
        assertNull(sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("4");
        assertEquals("4", sut.getMaxHistoryEntries());
        sut.setMaxHistoryEntries("-2");
        assertEquals("4", sut.getMaxHistoryEntries());
    }

    @Test
    public void testGetEntriesPerSite() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String expResult = "50";
        String result = sut.getMaxEntriesPerPage();
        assertEquals(expResult, result);
    }

    @Test
    public void testSetMaxEntriesPerSite() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage(null);
        assertNull(sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("");
        assertNull(sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("50");
        assertEquals("50", sut.getMaxEntriesPerPage());
        sut.setMaxEntriesPerPage("-2");
        assertEquals("50", sut.getMaxEntriesPerPage());
    }

    @Test
    public void testGetMaxDaysToKeepEntries() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        assertEquals("5", sut.getMaxDaysToKeepEntries());
    }

    @Test
    public void testSetMaxDaysToKeepEntries() {
        JobConfigHistory sut = createSut();
        assertNull(sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries(null);
        assertNull(sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("");
        assertNull(sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("4");
        assertEquals("4", sut.getMaxDaysToKeepEntries());
        sut.setMaxDaysToKeepEntries("-1");
        assertEquals("4", sut.getMaxDaysToKeepEntries());
    }

    @Test
    public void testIsPositiveInteger() {
        JobConfigHistory sut = createSut();
        assertFalse(sut.isPositiveInteger(""));
        assertFalse(sut.isPositiveInteger("-1"));
        assertTrue(sut.isPositiveInteger("0"));
        assertTrue(sut.isPositiveInteger("1"));
    }

    @Test
    public void testGetSkipDuplicateHistory() {
        JobConfigHistory sut = createSut();
        boolean expResult = true;
        boolean result = sut.getSkipDuplicateHistory();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetExcludePattern() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String result = sut.getExcludePattern();
        assertEquals(JobConfigHistoryConsts.DEFAULT_EXCLUDE, result);
    }

    @Test
    public void testGetSaveModuleConfiguration() {
        JobConfigHistory sut = createSut();
        boolean result = sut.getSaveModuleConfiguration();
        assertFalse(result);
    }

    @Test
    public void testGetShowBuildBadges() {
        JobConfigHistory sut = createSut();
        String expResult = "always";
        String result = sut.getShowBuildBadges();
        assertEquals(expResult, result);
    }

    @Test
    public void testShowBuildBadgesAlways() {
        AbstractProject<?, ?> mockedProject = mock(AbstractProject.class);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("always");
        assertTrue(sut.showBuildBadges(mockedProject));
    }

    @Test
    public void testShowBuildBadgesUserWithConfigPermission() {
        AbstractProject<?, ?> mockedProject = mock(AbstractProject.class);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true, false);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("userWithConfigPermission");
        assertTrue(sut.showBuildBadges(mockedProject));
        assertFalse(sut.showBuildBadges(mockedProject));
    }

    @Test
    public void testShowBuildBadgesAdminUser() throws IOException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");

        JobConfigHistory sut = createSut();
        JobConfigHistory unauthorizedSut = createUnauthorizedSut();

        //default: always
        assertTrue(sut.showBuildBadges(freeStyleProject));
        assertTrue(unauthorizedSut.showBuildBadges(freeStyleProject));

        sut.setShowBuildBadges("adminUser");
        unauthorizedSut.setShowBuildBadges("adminUser");
        assertTrue(sut.showBuildBadges(freeStyleProject));
        assertFalse(unauthorizedSut.showBuildBadges(freeStyleProject));
    }

    @Test
    public void testGetExcludeRegexpPattern() {
        JobConfigHistory sut = createSut();
        Pattern expResult = Pattern.compile(JobConfigHistoryConsts.DEFAULT_EXCLUDE);
        Pattern result = sut.getExcludeRegexpPattern();
        assertEquals(expResult.pattern(), result.pattern());
    }

    @Test
    public void testGetConfiguredHistoryRootDir() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        assertEquals(
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName(),
                "config-history");
        JSONObject formData = createFormData();
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName(),
                "config-history");
        formData.put("historyRootDir", "");
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName(),
                "config-history");
        formData.put("historyRootDir", "/tmp/");
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName(),
                "config-history");
    }

    @Test
    public void testIsSaveable() throws Exception {
        XmlFile xmlFile = new XmlFile(
                unpackResourceZip.getResource("jobs/Test1/config.xml"));
        XmlFile sysConfig = new XmlFile(new File(jenkinsRule.getInstance().getRootDir(), "config.xml"));
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject("Test1");
        MatrixProject matrixProject = jenkinsRule.createProject(MatrixProject.class, "MatrixProjectTest1");

        JobConfigHistory sut = createNonSavingSut();
        JSONObject formData = createFormData();

        sut.configure(mockStaplerRequest(), formData);
        assertTrue(sut.isSaveable(freeStyleProject, xmlFile));
        assertTrue(sut.isSaveable(matrixProject, xmlFile));
        assertTrue(sut.isSaveable(mock(MockFolder.class), xmlFile));
        assertTrue(sut.isSaveable(null, sysConfig));
        assertTrue(sut.isSaveable(freeStyleProject, sysConfig));
        assertFalse(sut.isSaveable(null, xmlFile));
        assertFalse(sut.isSaveable(mock(MatrixConfiguration.class), xmlFile));
    }

    @Test
    public void testIsSaveableWithExcludesPattern()
            throws IOException, ServletException, Descriptor.FormException {
        JobConfigHistory sut = createNonSavingSut();
        File jenkinsHome = sut.getJenkinsHome();
        String filePath = String.join(File.separator, "jobs", "multiple-branch", "branches", "master", "config.xml");
        XmlFile xmlFile = new XmlFile(new File(jenkinsHome, filePath));
        assertTrue(sut.isSaveable(mock(TopLevelItem.class), xmlFile));
        JSONObject formData = createFormData();
        formData.put("excludePattern", Pattern.quote(File.separator + "branches" + File.separator));
        sut.configure(mockStaplerRequest(), formData);
        assertFalse(sut.isSaveable(mock(TopLevelItem.class), xmlFile));
    }

    @Test
    public void testDoCheckMaxHistoryEntries() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries("5"));
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("A"));
    }

    @Test
    public void testDoCheckMaxEntriesPerPage() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage(""));
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage("5"));
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxEntriesPerPage("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxEntriesPerPage("A"));
    }

    @Test
    public void testDoCheckMaxDaysToKeepEntries() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("5"));
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("A"));
    }

    @Test
    public void testDoCheckExcludePattern() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckExcludePattern(""));
        assertNotEquals(expectedResult, sut.doCheckExcludePattern("[.*"));
    }

    private JobConfigHistory createUnauthorizedSut() {
        return new JobConfigHistory() {
            @Override
            public Jenkins getJenkins() {
                return mock(Jenkins.class);
            }
        };
    }

    private JobConfigHistory createSut() {
        return new JobConfigHistory();
    }

    private JobConfigHistory createNonSavingSut() {
        return new JobConfigHistory() {
            @Override
            public void save() {
                //do nothing
            }
        };
    }

    private JSONObject createFormData() {
        JSONObject obj = new JSONObject();
        obj.put("historyRootDir", unpackResourceZip.getResource("config-history").getPath());
        obj.put("maxHistoryEntries", "5");
        obj.put("maxDaysToKeepEntries", "5");
        obj.put("maxEntriesPerPage", "50");
        obj.put("skipDuplicateHistory", true);
        obj.put("saveModuleConfiguration", true);
        obj.put("showBuildBadges", "5");
        obj.put("excludedUsers", "user1,user2");
        obj.put("excludePattern", JobConfigHistoryConsts.DEFAULT_EXCLUDE);
        obj.put("showChangeReasonCommentWindow", true);
        return obj;
    }
}
