package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import hudson.model.FreeStyleProject;
import hudson.model.Project;
import org.acegisecurity.AccessDeniedException;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import jenkins.model.AbstractTopLevelItem;
import jenkins.model.Jenkins;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryProjectActionTest {

	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();

	@Rule
	public UnpackResourceZip testConfigs = UnpackResourceZip.create();

	private final ItemGroup<?> mockedItemGroup = mock(ItemGroup.class);
	private final MavenModule mockedMavenModule = mock(MavenModule.class);
	private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
	private final Jenkins mockedJenkins = mock(Jenkins.class);
	private final AbstractTopLevelItem mockedProject = mock(
			AbstractTopLevelItem.class);
	private final StaplerRequest mockedRequest = mock(StaplerRequest.class);
	private final StaplerResponse mockedResponse = mock(StaplerResponse.class);
	private HistoryDao historyDao;

	public JobConfigHistoryProjectActionTest() {
		when(mockedItemGroup.getFullName()).thenReturn("");
		when(mockedProject.getParent()).thenReturn(mockedItemGroup);
		when(mockedProject.getFullName()).thenReturn("Test1");
	}

	@Before
	public void createHistoryDao() {
		historyDao = new FileHistoryDao(
				testConfigs.getResource("config-history"),
				testConfigs.getRoot(), null, 0, false);
	}
	/**
	 * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetIconFileNameNoPermission() {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(false);
		JobConfigHistoryProjectAction sut = createAction();
		assertNull(sut.getIconFileName());
	}

	/**
	 * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetIconFileNameSaveProjectNonMavenModules() {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
		JobConfigHistoryProjectAction sut = createAction();
		assertEquals(JobConfigHistoryConsts.ICONFILENAME,
				sut.getIconFileName());
	}

	/**
	 * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetIconFileNameSaveMavenModules() {
		assertNotNull(jenkinsRule.jenkins.getPlugin("maven-plugin"));
		when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(true);
		JobConfigHistoryProjectAction sut = createActionForMavenModule();
		assertEquals(JobConfigHistoryConsts.ICONFILENAME,
				sut.getIconFileName());
	}

	/**
	 * Test of getIconFileName method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetIconFileNameDoNotSaveMavenModules() {
		when(mockedMavenModule.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedPlugin.getSaveModuleConfiguration()).thenReturn(false);
		JobConfigHistoryProjectAction sut = createActionForMavenModule();
		assertNull(sut.getIconFileName());
	}

	@Test
	public void testGetIconFileNameMatrixProject() {
		MatrixProject project = mock(MatrixProject.class);
		when(project.hasPermission(AbstractProject.CONFIGURE)).thenReturn(true);

		JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(
				mockedJenkins, project);
		assertEquals(JobConfigHistoryConsts.ICONFILENAME,
				action.getIconFileName());
	}

	@Test
	public void testGetIconFileNameMatrixConfiguration() {
		MatrixConfiguration configuration = mock(MatrixConfiguration.class);
		when(configuration.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);

		JobConfigHistoryProjectActionImpl action = new JobConfigHistoryProjectActionImpl(
				mockedJenkins, configuration);
		assertNull(action.getIconFileName());
	}

	/**
	 * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetJobConfigs() throws Exception {
		when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("");
		testJobXHasYHistoryEntries("jobs/Test1", 5);
	}

	/**
	 * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetJobConfigsLimitedTo3() throws Exception {
		when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("3");
		testJobXHasYHistoryEntries("jobs/Test1", 3);
	}

	/**
	 * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetJobConfigsLimitedTo1000() throws Exception {
		when(mockedPlugin.getMaxEntriesPerPage()).thenReturn("1000");
		testJobXHasYHistoryEntries("jobs/Test1", 5);
	}

	/**
	 * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetJobConfigsDeleted() throws Exception {
		final List<ConfigInfo> historyEntries = testJobXHasYHistoryEntries(
				"jobs/Foo_deleted_20130830_223932_071", 3);
		assertEquals("Deleted", historyEntries.get(0).getOperation());
	}

	/**
	 * Test of getJobConfigs method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetJobConfigsEmpty() throws Exception {
		FileUtils.cleanDirectory(
				testConfigs.getResource("config-history/jobs/Test1"));
		testJobXHasYHistoryEntries("jobs/Test1", 0);
	}

	@Test
	public void testGetRevisionAmount() throws IOException {
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("project");
		final JobConfigHistoryProjectAction sut = createJenkinsRuleAction(project);
		assertEquals(2, sut.getRevisionAmount());

		project.renameTo("pr0ject");
		assertEquals(3, sut.getRevisionAmount());
	}
	@Test
	public void testGetMaxPageNum() throws IOException, InterruptedException {

		FreeStyleProject project = jenkinsRule.createFreeStyleProject("project");
		final JobConfigHistoryProjectAction sut = createJenkinsRuleAction(project);
		project.renameTo("asd");
		project.renameTo("asdf");
		project.renameTo("asd");
		project.renameTo("asdf");
		//revision amount == 6

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(10));
		assertEquals(0, sut.getMaxPageNum());

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(6));
		assertEquals(0, sut.getMaxPageNum());

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(5));
		assertEquals(1, sut.getMaxPageNum());

		project.renameTo("pr0ject");
		for (int i = 0; i < 13; ++i) {
			project.renameTo("p." + i);
		}
		//revision amount == 20

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(5));
		assertEquals(3, sut.getMaxPageNum());

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(4));
		assertEquals(4, sut.getMaxPageNum());

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(20));
		assertEquals(0, sut.getMaxPageNum());

		when(mockedRequest.getParameter("entriesPerPage")).thenReturn(String.valueOf(10));
		assertEquals(1, sut.getMaxPageNum());
	}

	@Test
	public void testGetJobConfigs_fromTo() throws IOException {
		testJobXHasYHistoryEntries("jobs/Test1", 5, 0,200);
		testJobXHasYHistoryEntries("jobs/Test1", 4, 1,200);
		testJobXHasYHistoryEntries("jobs/Test1", 3, 2,200);
		testJobXHasYHistoryEntries("jobs/Test1", 1, 4,200);
		testJobXHasYHistoryEntries("jobs/Test1", 0, 5,200);

		testJobXHasYHistoryEntries("jobs/Test1", 4, 0,4);
		testJobXHasYHistoryEntries("jobs/Test1", 5, 0,5);
		testJobXHasYHistoryEntries("jobs/Test1", 5, 0,6);

		testJobXHasYHistoryEntries("jobs/Test1", 1, 2,3);
	}

	private List<ConfigInfo> testJobXHasYHistoryEntries(final String jobDir,
			final int noOfHistoryEntries) throws IOException {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedProject.getRootDir())
				.thenReturn(testConfigs.getResource(jobDir));
		final JobConfigHistoryProjectAction sut = createAction();
		final List<ConfigInfo> result = sut.getJobConfigs();
		assertEquals(noOfHistoryEntries, result.size());
		return result;
	}

	private List<ConfigInfo> testJobXHasYHistoryEntries(final String jobDir,
														final int noOfHistoryEntries,
														int from, int to) throws IOException {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
			.thenReturn(true);
		when(mockedProject.getRootDir())
			.thenReturn(testConfigs.getResource(jobDir));
		final JobConfigHistoryProjectAction sut = createAction();
		final List<ConfigInfo> result = sut.getJobConfigs(from, to);
		assertEquals(noOfHistoryEntries, result.size());
		return result;
	}

	/**
	 * Test of getFile method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetFile() throws Exception {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedProject.getRootDir())
				.thenReturn(testConfigs.getResource("jobs/Test1"));
		when(mockedRequest.getParameter("timestamp"))
				.thenReturn("2012-11-21_11-40-28");
		final JobConfigHistoryProjectAction sut = createAction();
		String result = sut.getFile();
		assertThat(result, CoreMatchers.startsWith("<?xml version="));
		assertThat(result, CoreMatchers.endsWith("</project>"));
	}

	/**
	 * Test of getProject method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetProject() {
		JobConfigHistoryProjectAction sut = createAction();
		assertEquals(mockedProject, sut.getProject());
	}

	/**
	 * Test of getAccessControlledObject method, of class
	 * JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetAccessControlledObject() {
		JobConfigHistoryProjectAction sut = createAction();
		assertEquals(mockedProject, sut.getAccessControlledObject());
	}

	/**
	 * Test of checkConfigurePermission method, of class
	 * JobConfigHistoryProjectAction.
	 */
	@Test(expected = AccessDeniedException.class)
	public void testCheckConfigurePermission() {
		doThrow(new AccessDeniedException("Oops")).when(mockedProject)
				.checkPermission(AbstractItem.CONFIGURE);
		JobConfigHistoryProjectAction sut = createAction();
		sut.checkConfigurePermission();
	}

	/**
	 * Test of hasConfigurePermission method, of class
	 * JobConfigHistoryProjectAction.
	 */
	@Test
	public void testHasConfigurePermission() {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		JobConfigHistoryProjectAction sut = createAction();
		assertTrue(sut.hasConfigurePermission());
	}

	/**
	 * Test of hasConfigurePermission method, of class
	 * JobConfigHistoryProjectAction.
	 */
	@Test
	public void testHasNoConfigurePermission() {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(false);
		JobConfigHistoryProjectAction sut = createAction();
		assertFalse(sut.hasConfigurePermission());
	}

	/**
	 * Test of doDiffFiles method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testDoDiffFiles() throws Exception {
		when(mockedRequest.getParameter("timestamp1"))
				.thenReturn("2014-02-05_10-42-37");
		when(mockedRequest.getParameter("timestamp2"))
				.thenReturn("2014-03-12_11-02-12");
		JobConfigHistoryProjectAction sut = createAction();
		sut.doDiffFiles(mockedRequest, mockedResponse);
		verify(mockedResponse).sendRedirect(
				"showDiffFiles?timestamp1=2014-02-05_10-42-37&timestamp2=2014-03-12_11-02-12");
	}

	@Test
	public void testGetTimestamp() throws Exception, Throwable {
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedRequest.getParameter(any(String.class)))
				.thenReturn("2012-11-21_11-41-14");
		JobConfigHistoryProjectAction sut = createAction();
		assertEquals("2012-11-21_11-41-14", sut.getTimestamp(1));
	}

	/**
	 * Test of getLines method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testGetLines() throws Exception, Throwable {
		final String timestamp1 = "2012-11-21_11-41-14";
		final String timestamp2 = "2012-11-21_11-42-05";
		List<SideBySideView.Line> result = prepareGetLines(timestamp1,
				timestamp2);
		assertEquals(8, result.size());
	}

	/**
	 * Test of getLines method, of class JobConfigHistoryProjectAction.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testGetLinesNonExistingTimestamp() throws IOException {
		final String timestamp1 = "2012-11-21_11-41-14";
		final String timestamp2 = "2013-11-21_11-42-05";
		prepareGetLines(timestamp1, timestamp2);
	}

	private List<SideBySideView.Line> prepareGetLines(final String timestamp1,
			final String timestamp2) throws IOException {
		when(mockedRequest.getParameter("timestamp1")).thenReturn(timestamp1);
		when(mockedRequest.getParameter("timestamp2")).thenReturn(timestamp2);
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedProject.getRootDir())
				.thenReturn(testConfigs.getResource("jobs/Test1"));
		JobConfigHistoryProjectAction sut = createAction();
		return sut.getLines();
	}

	/**
	 * Test of doRestore method, of class JobConfigHistoryProjectAction.
	 */
	@Test
	public void testDoRestore() throws Exception {
		when(mockedRequest.getParameter("timestamp"))
				.thenReturn("2012-11-21_11-41-14");
		when(mockedProject.hasPermission(AbstractProject.CONFIGURE))
				.thenReturn(true);
		when(mockedProject.getRootDir())
				.thenReturn(testConfigs.getResource("jobs/Test1"));
		JobConfigHistoryProjectAction sut = createAction();
		sut.doRestore(mockedRequest, mockedResponse);
		verify(mockedProject).save();
	}

	/**
	 * Test of doForwardToRestoreQuestion method, of class
	 * JobConfigHistoryProjectAction.
	 */
	@Test
	public void testDoForwardToRestoreQuestion() throws Exception {
		when(mockedRequest.getParameter("timestamp"))
				.thenReturn("2012-11-21_11-41-14");
		JobConfigHistoryProjectAction sut = createAction();
		sut.doForwardToRestoreQuestion(mockedRequest, mockedResponse);
		verify(mockedResponse).sendRedirect(any(String.class));
	}

	private JobConfigHistoryProjectAction createAction() {
		return new JobConfigHistoryProjectActionImpl(mockedJenkins,
				mockedProject);
	}

	private JobConfigHistoryProjectAction createJenkinsRuleAction(Project project) {
		return new JobConfigHistoryProjectActionJrImpl(jenkinsRule.getInstance(), project);
	}

	private JobConfigHistoryProjectAction createActionForMavenModule() {
		return new JobConfigHistoryProjectActionImpl(mockedJenkins,
				mockedMavenModule);
	}

	private class JobConfigHistoryProjectActionImpl
			extends
				JobConfigHistoryProjectAction {

		public JobConfigHistoryProjectActionImpl(Jenkins jenkins,
				AbstractItem project) {
			super(jenkins, project);
		}

		@Override
		protected JobConfigHistory getPlugin() {
			return mockedPlugin;
		}

		@Override
		protected StaplerRequest getCurrentRequest() {
			return mockedRequest;
		}

		@Override
		protected HistoryDao getHistoryDao() {
			return historyDao;
		}
	}

	private class JobConfigHistoryProjectActionJrImpl extends JobConfigHistoryProjectAction {

		public JobConfigHistoryProjectActionJrImpl(Jenkins jenkins, AbstractItem project) {
			super(jenkins, project);
		}

		@Override
		protected StaplerRequest getCurrentRequest() {
			return mockedRequest;
		}
	}
}
