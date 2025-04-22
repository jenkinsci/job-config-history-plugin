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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Build;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Tests for JobConfigBadgeAction.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
class JobConfigBadgeActionTest {

    private final Build mockedBuild = mock(Build.class);
    private final Project mockedProject = mock(Project.class);
    private final String[] configDates = {"2013_01_01", "2013_01_02"};
    private final JobConfigBadgeAction sut = createSut();

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
        final Jenkins mockedJenkins = mock(Jenkins.class);
        when(mockedProject.getParent()).thenReturn(mockedJenkins);
        when(mockedProject.getShortUrl()).thenReturn("jobname");
        when(mockedBuild.getParent()).thenReturn(mockedProject);
        when(mockedBuild.getProject()).thenReturn(mockedProject);
    }

    @Test
    void testOnAttached() {
        sut.onAttached(mockedBuild);
    }

    @Test
    void testOnLoad() {
        sut.onLoad(mockedBuild);
    }

    @Test
    void testShowBadge() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
        jenkinsRule.buildAndAssertSuccess(project);
        Run<FreeStyleProject, FreeStyleBuild> build = project.getBuilds().getLastBuild();

        sut.onAttached(build);
        assertTrue(sut.showBadge());
    }

    @Test
    void testShowBadgeLink() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
        jenkinsRule.buildAndAssertSuccess(project);
        Run<FreeStyleProject, FreeStyleBuild> build = project.getBuilds().getLastBuild();

        sut.onAttached(build);
        assertTrue(sut.showBadgeLink());
    }

    @Test
    void testOldConfigsExist() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
        jenkinsRule.buildAndAssertSuccess(project);
        Run<FreeStyleProject, FreeStyleBuild> build = project.getBuilds().getLastBuild();

        SortedMap<String, HistoryDescr> revisions = PluginUtils.getHistoryDao().getJobHistory(project.getFullName());
        assertEquals(2, revisions.size());

        JobConfigBadgeAction customSut = createSut(revisions.lastKey(), revisions.firstKey());
        customSut.onAttached(build);
        assertTrue(customSut.oldConfigsExist());
        System.out.println("ACL.SYSTEM-check in PluginUtils: " + Jenkins.get().getUser(ACL.SYSTEM_USERNAME));
    }

    @Test
    void testOldConfigsExistFalse() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
        jenkinsRule.buildAndAssertSuccess(project);
        Run<FreeStyleProject, FreeStyleBuild> build = project.getBuilds().getLastBuild();

        SortedMap<String, HistoryDescr> revisions = PluginUtils.getHistoryDao().getJobHistory(project.getFullName());
        assertEquals(2, revisions.size());

        sut.onAttached(build);
        assertFalse(sut.oldConfigsExist());
    }

    @Test
    void testCreateLink() throws Exception {
        String timestampRegex = "[0-9\\-_]+";

        String expectedRegex =
                "http://localhost:[0-9]{1,5}?/jenkins/job/Test1/jobConfigHistory/showDiffFiles\\?timestamp1=" + timestampRegex
                        + "&timestamp2=" + timestampRegex;
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
        jenkinsRule.buildAndAssertSuccess(project);
        Run<FreeStyleProject, FreeStyleBuild> build = project.getBuilds().getLastBuild();

        sut.onAttached(build);
        assertTrue(sut.createLink().matches(expectedRegex));
    }

    @Test
    void testGetTooltip() {
        String expResult = "Config changed since last build";
        String result = sut.getTooltip();
        assertEquals(expResult, result);
    }

    @Test
    void testGetIcon() {
        String expResult = "symbol-buildbadge plugin-jobConfigHistory";
        String result = sut.getIcon();
        assertEquals(expResult, result);
    }

    @Test
    void testGetIconFileName() {
        System.out.println("plugins");
        jenkinsRule.jenkins.getPluginManager().getPlugins().forEach(
                plugin -> System.out.println(plugin.getDisplayName())
        );
        String result = sut.getIconFileName();
        assertNull(result);
    }

    @Test
    void testGetDisplayName() {
        String result = sut.getDisplayName();
        assertNull(result);
    }

    @Test
    void testGetUrlName() {
        String expResult = "";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    @Test
    void testListenerOnStartedBuildNumberLessThan2() {
        final JobConfigBadgeAction.Listener lsut = createListenerSut();
        lsut.onStarted(mockedBuild, TaskListener.NULL);
    }

    @Test
    void testListenerOnStartedGreaterThan2ButNoPreviousBuild() {
        final JobConfigBadgeAction.Listener psut = createListenerSut();
        when(mockedProject.getNextBuildNumber()).thenReturn(3);
        when(mockedProject.getLastBuild()).thenReturn(mockedBuild);
        psut.onStarted(mockedBuild, TaskListener.NULL);
    }

    @Test
    void testListenerOnStarted() {
        final JobConfigBadgeAction.Listener psut = createListenerSut();
        when(mockedProject.getNextBuildNumber()).thenReturn(3);
        when(mockedProject.getLastBuild()).thenReturn(mockedBuild);
        // when(mockedHistoryDao.getRevisions(any(XmlFile.class))).thenReturn(null)
        final GregorianCalendar calendar = new GregorianCalendar(2013, Calendar.OCTOBER, 30,
                23, 0, 5);
        final Build<?, ?> previousBuild = new Build(mockedProject, calendar) {
            @Override
            public @NonNull Project<?, ?> getParent() {
                return mockedProject;
            }
        };
        when(mockedBuild.getPreviousBuild()).thenReturn(previousBuild);
        psut.onStarted(mockedBuild, TaskListener.NULL);
    }

    private JobConfigBadgeAction.Listener createListenerSut() {
        return new JobConfigBadgeAction.Listener() {

            @Override
            List<HistoryDescr> getRevisions(Job<?, ?> project) {
                return Arrays.asList(
                        new HistoryDescr("user", "userId", "changed",
                                "2013-10-30_23-00-06", null, null),
                        new HistoryDescr("user", "userId", "changed",
                                "2013-10-30_23-00-07", null, null));
            }

        };
    }

    JobConfigBadgeAction createSut() {
        return new JobConfigBadgeAction(configDates);
    }

    JobConfigBadgeAction createSut(String timestamp1, String timestamp2) {
        return new JobConfigBadgeAction(new String[]{timestamp1, timestamp2});
    }
}
