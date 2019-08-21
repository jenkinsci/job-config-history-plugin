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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;

import hudson.model.*;
import jenkins.util.TimeDuration;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Matchers;

import javax.servlet.ServletException;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigBadgeActionTest {

	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();

	private static final String ROOT_URL = "http://example.org/jenkins";
	private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
	private final HistoryDao mockedHistoryDao = mock(HistoryDao.class);
	private final Build mockedBuild = mock(Build.class);
	private final Project mockedProject = mock(Project.class);
	private final String[] configDates = {"2013_01_01", "2013_01_02"};
	private JobConfigBadgeAction sut = createSut();

	public JobConfigBadgeActionTest() {
		final ItemGroup<?> mockedItemGroup = mock(ItemGroup.class);
		when(mockedItemGroup.getUrl()).thenReturn("/jobs/");
		when(mockedProject.getParent()).thenReturn(mockedItemGroup);
		when(mockedProject.getShortUrl()).thenReturn("jobname");
		when(mockedBuild.getProject()).thenReturn(mockedProject);
	}

	/**
	 * Test of onAttached method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testOnAttached() {
		sut.onAttached(mockedBuild);
	}

	/**
	 * Test of onLoad method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testOnLoad() {
		sut.onLoad(mockedBuild);
	}

	/**
	 * Test of showBadge method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testShowBadge() throws Exception {
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
		jenkinsRule.buildAndAssertSuccess(project);
		Run build = project.getBuilds().get(0);

		sut.onAttached(build);
		assertEquals(true, sut.showBadge());
	}

	/**
	 * Test of oldConfigsExist method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testOldConfigsExist() throws Exception {
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
		jenkinsRule.buildAndAssertSuccess(project);
		Run build = project.getBuilds().get(0);

		SortedMap<String, HistoryDescr> revisions = PluginUtils.getHistoryDao().getJobHistory(project.getFullName());
		assertEquals(2, revisions.size());

		JobConfigBadgeAction customSut = createSut(revisions.lastKey(), revisions.firstKey());
		customSut.onAttached(build);
		assertEquals(true, customSut.oldConfigsExist());
	}

	@Test
	public void testOldConfigsExistFalse() throws Exception {
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
		jenkinsRule.buildAndAssertSuccess(project);
		Run build = project.getBuilds().get(0);

		SortedMap<String, HistoryDescr> revisions = PluginUtils.getHistoryDao().getJobHistory(project.getFullName());
		assertEquals(2, revisions.size());

		sut.onAttached(build);
		assertEquals(false, sut.oldConfigsExist());
	}


	/**
	 * Test of createLink method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testCreateLink() throws Exception {
		String timestampRegex = "[0-9\\-_]+";

		String expectedRegex =
			"http://localhost:[0-9]{1,5}?/jenkins/job/Test1/jobConfigHistory/showDiffFiles\\?timestamp1=" + timestampRegex
			+ "&timestamp2=" + timestampRegex;
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("Test1");
		jenkinsRule.buildAndAssertSuccess(project);
		Run build = project.getBuilds().get(0);

		sut.onAttached(build);
		assertTrue(sut.createLink().matches(expectedRegex));
	}

	/**
	 * Test of getTooltip method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testGetTooltip() {
		String expResult = "Config changed since last build";
		String result = sut.getTooltip();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getIcon method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testGetIcon() {
		String expResult = "/plugin/jobConfigHistory/img/buildbadge.png";
		String result = sut.getIcon();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getIconFileName method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testGetIconFileName() {
		String result = sut.getIconFileName();
		assertNull(result);
	}

	/**
	 * Test of getDisplayName method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testGetDisplayName() {
		String result = sut.getDisplayName();
		assertNull(result);
	}

	/**
	 * Test of getUrlName method, of class JobConfigBadgeAction.
	 */
	@Test
	public void testGetUrlName() {
		String expResult = "";
		String result = sut.getUrlName();
		assertEquals(expResult, result);
	}

	@Test
	public void testListenerOnStartedBuildNumberLessThan2() {
		final JobConfigBadgeAction.Listener lsut = createListenerSut();
		lsut.onStarted(mockedBuild, TaskListener.NULL);
	}

	@Test
	public void testListenerOnStartedGreaterThan2ButNoPreviousBuild() {
		final JobConfigBadgeAction.Listener psut = createListenerSut();
		when(mockedProject.getNextBuildNumber()).thenReturn(3);
		when(mockedProject.getLastBuild()).thenReturn(mockedBuild);
		psut.onStarted(mockedBuild, TaskListener.NULL);
	}

	@Test
	public void testListenerOnStarted() {
		final JobConfigBadgeAction.Listener psut = createListenerSut();
		when(mockedProject.getNextBuildNumber()).thenReturn(3);
		when(mockedProject.getLastBuild()).thenReturn(mockedBuild);
		// when(mockedHistoryDao.getRevisions(any(XmlFile.class))).thenReturn(null)
		final GregorianCalendar calendar = new GregorianCalendar(2013, 9, 30,
				23, 0, 5);
		final Build<?, ?> previousBuild = new Build(mockedProject, calendar) {
			@Override
			public Project<?, ?> getParent() {
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
