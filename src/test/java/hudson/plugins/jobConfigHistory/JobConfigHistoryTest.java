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
import hudson.model.Item;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mockito;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistory.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
class JobConfigHistoryTest {

    private UnpackResourceZip unpackResourceZip;
    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkinsRule = rule;
        unpackResourceZip = UnpackResourceZip.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (unpackResourceZip != null) {
            unpackResourceZip.cleanUp();
        }
    }

    private StaplerRequest2 mockStaplerRequest() throws ServletException {
        Stapler stapler = mock(Stapler.class, CALLS_REAL_METHODS);
        ServletContext servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
        stapler.init(servletConfig);
        HttpServletRequest rawRequest = mock(HttpServletRequest.class);
        return new RequestImpl(stapler, rawRequest, Collections.emptyList(), null);
    }

    @Test
    void testConfigure() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
    }

    @Test
    void testGetHistoryRootDir() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        assertThat(sut.getHistoryRootDir(), endsWith("config-history"));
    }

    @Test
    void testGetDefaultRootDir() {
        JobConfigHistory sut = createSut();
        assertThat(sut.getDefaultRootDir(), endsWith("config-history"));
    }

    @Test
    void testGetMaxHistoryEntries() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String expResult = "5";
        String result = sut.getMaxHistoryEntries();
        assertEquals(expResult, result);
    }

    @Test
    void testSetMaxHistoryEntries() {
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
    void testGetEntriesPerSite() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String expResult = "50";
        String result = sut.getMaxEntriesPerPage();
        assertEquals(expResult, result);
    }

    @Test
    void testSetMaxEntriesPerSite() {
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
    void testGetMaxDaysToKeepEntries() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        assertEquals("5", sut.getMaxDaysToKeepEntries());
    }

    @Test
    void testSetMaxDaysToKeepEntries() {
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
    void testIsPositiveInteger() {
        JobConfigHistory sut = createSut();
        assertFalse(sut.isPositiveInteger(""));
        assertFalse(sut.isPositiveInteger("-1"));
        assertTrue(sut.isPositiveInteger("0"));
        assertTrue(sut.isPositiveInteger("1"));
    }

    @Test
    void testGetSkipDuplicateHistory() {
        JobConfigHistory sut = createSut();
        boolean expResult = true;
        boolean result = sut.getSkipDuplicateHistory();
        assertEquals(expResult, result);
    }

    @Test
    void testGetExcludePattern() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        sut.configure(mockStaplerRequest(), createFormData());
        String result = sut.getExcludePattern();
        assertEquals(JobConfigHistoryConsts.DEFAULT_EXCLUDE, result);
    }

    @Test
    void testGetSaveModuleConfiguration() {
        JobConfigHistory sut = createSut();
        boolean result = sut.getSaveModuleConfiguration();
        assertFalse(result);
    }

    @Test
    void testGetShowBuildBadges() {
        JobConfigHistory sut = createSut();
        String expResult = "always";
        String result = sut.getShowBuildBadges();
        assertEquals(expResult, result);
    }

    @Test
    void testShowBuildBadgesAlways() {
        AbstractProject<?, ?> mockedProject = mock(AbstractProject.class);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("always");
        assertTrue(sut.showBuildBadges(mockedProject));
    }

    @Test
    void testShowBuildBadgesUserWithConfigPermission() {
        AbstractProject<?, ?> mockedProject = mock(AbstractProject.class);
        when(mockedProject.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true, false);
        JobConfigHistory sut = createSut();
        sut.setShowBuildBadges("userWithConfigPermission");
        assertTrue(sut.showBuildBadges(mockedProject));
        assertFalse(sut.showBuildBadges(mockedProject));
    }

    @Test
    void testShowBuildBadgesAdminUser() throws IOException {
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
    void testShowBuildBadgesLink() {
        AbstractProject<?, ?> mockedProject = mock(AbstractProject.class);

        when(mockedProject.hasPermission(Item.CONFIGURE)).thenReturn(true, false);

        JobConfigHistory sut = createSut();
        JobConfigHistory unauthorizedSut = createUnauthorizedSut();


        // showBuildBadges set to 'never'
        sut.setShowBuildBadges("never");
        assertFalse(sut.showBuildBadgesLink(mockedProject));

        // showBuildBadges set to either 'always', 'userWithConfigPermission' or 'admin'
        sut.setShowBuildBadges("always");

        // should return True as user have config permission
        assertTrue(sut.showBuildBadgesLink(mockedProject));

        // should return False as user do not have config permission
        assertFalse(unauthorizedSut.showBuildBadgesLink(mockedProject));
    }

    @Test
    void testGetExcludeRegexpPattern() {
        JobConfigHistory sut = createSut();
        Pattern expResult = Pattern.compile(JobConfigHistoryConsts.DEFAULT_EXCLUDE);
        Pattern result = sut.getExcludeRegexpPattern();
        assertEquals(expResult.pattern(), result.pattern());
    }

    @Test
    void testGetConfiguredHistoryRootDir() throws Exception {
        JobConfigHistory sut = createNonSavingSut();
        assertEquals(
                "config-history",
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName());
        JSONObject formData = createFormData();
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                "config-history",
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName());
        formData.put("historyRootDir", "");
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                "config-history",
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName());
        formData.put("historyRootDir", "/tmp/");
        sut.configure(mockStaplerRequest(), formData);
        assertEquals(
                "config-history",
                new File(sut.getConfiguredHistoryRootDir().getPath()).getName());
    }

    @Test
    void testIsSaveable() throws Exception {
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
    void testIsSaveableWithExcludesPattern()
            throws ServletException, Descriptor.FormException {
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
    void testDoCheckMaxHistoryEntries() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries("5"));
        assertEquals(expectedResult, sut.doCheckMaxHistoryEntries(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxHistoryEntries("A"));
    }

    @Test
    void testDoCheckMaxEntriesPerPage() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage(""));
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage("5"));
        assertEquals(expectedResult, sut.doCheckMaxEntriesPerPage(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxEntriesPerPage("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxEntriesPerPage("A"));
    }

    @Test
    void testDoCheckMaxDaysToKeepEntries() {
        JobConfigHistory sut = createSut();
        FormValidation expectedResult = FormValidation.ok();
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries(""));
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("5"));
        assertEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries(" 5 "));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("-1"));
        assertNotEquals(expectedResult, sut.doCheckMaxDaysToKeepEntries("A"));
    }

    @Test
    void testDoCheckExcludePattern() {
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
