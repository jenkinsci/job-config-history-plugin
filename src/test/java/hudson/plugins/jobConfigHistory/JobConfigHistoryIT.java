package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlForm;

import hudson.XmlFile;
import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;

/**
 * @author jborghi@cisco.com
 *
 */
public class JobConfigHistoryIT
		extends
			AbstractHudsonTestCaseDeletingInstanceDir {

	private WebClient webClient;
	// we need to sleep between saves so we don't overwrite the history
	// directories
	// (which are saved with a granularity of one second)
	private static final int SLEEP_TIME = 1100;

	private static final FileFilter DELETE_FILTER = new FileFilter() {
		public boolean accept(File file) {
			if (file.isDirectory()) {
				file.listFiles(this);
			}
			file.delete();
			return false;
		}
	};

	@Override
	public void before() throws Throwable {
		super.before();
		jenkins.setSecurityRealm(
				new HudsonPrivateSecurityRealm(true, false, null));
		webClient = new WebClient();
	}

	public void testJobConfigHistoryPreConfigured() {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
		try {
			final HtmlForm form = webClient.goTo("configure")
					.getFormByName("config");
			form.getInputByName("maxHistoryEntries").setValueAttribute("10");
			form.getInputByName("saveModuleConfiguration").setChecked(false);
			form.getInputByName("skipDuplicateHistory").setChecked(false);
			form.getInputByName("excludePattern")
					.setValueAttribute(JobConfigHistoryConsts.DEFAULT_EXCLUDE);
			form.getInputByName("historyRootDir")
					.setValueAttribute("jobConfigHistory");
			form.getInputByValue("never").setChecked(true);
			submit(form);
		} catch (Exception e) {
			Assert.fail("unable to configure Jenkins instance " + e);
		}
		Assert.assertEquals("Verify history entries to keep setting.", "10",
				jch.getMaxHistoryEntries());
		Assert.assertFalse("Verify Maven module configuration setting.",
				jch.getSaveModuleConfiguration());
		Assert.assertFalse("Verify skip duplicate history setting.",
				jch.getSkipDuplicateHistory());
		Assert.assertEquals("Verify configured history root directory.",
				new File(jenkins.root + "/jobConfigHistory/"
						+ JobConfigHistoryConsts.DEFAULT_HISTORY_DIR),
				jch.getConfiguredHistoryRootDir());
		Assert.assertEquals("Verify exclude pattern setting.",
				JobConfigHistoryConsts.DEFAULT_EXCLUDE,
				jch.getExcludePattern());
		Assert.assertEquals("Verify build badges setting.", "never",
				jch.getShowBuildBadges());

		final XmlFile jenkinsConfig = new XmlFile(
				new File(jenkins.getRootDir(), "config.xml"));
		Assert.assertTrue("Verify a system level configuration is saveable.",
				jch.isSaveable(jenkins, jenkinsConfig));

		Assert.assertEquals("Verify system configuration history location",
				getHistoryDir(jenkinsConfig).getParentFile(), jch.getConfiguredHistoryRootDir());
		testCreateRenameDeleteProject(jch);
		try {
			getHistoryDir(new XmlFile(new File("/tmp")));
			Assert.fail(
					"Verify IAE when attempting to get history dir for a file outside of JENKINS_ROOT.");
		} catch (IllegalArgumentException e) {
			Assert.assertNotNull("Expected IAE", e);
		}
		Assert.assertFalse(
				"Verify false when testing if a file outside of JENKINS_ROOT is saveable.",
				jch.isSaveable(null, new XmlFile(new File("/tmp/config.xml"))));
	}

	public void testJobConfigHistoryDefaults() throws IOException {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);

		Assert.assertNull(
				"Verify number of history entries to keep default setting.",
				jch.getMaxHistoryEntries());
		Assert.assertFalse("Verify Maven module configuration default setting.",
				jch.getSaveModuleConfiguration());
		Assert.assertTrue("Verify skip duplicate history default setting.",
				jch.getSkipDuplicateHistory());
		Assert.assertNull("Verify unconfigured exclude pattern.",
				jch.getExcludePattern());
		Assert.assertEquals("Verify build badges setting.", "always",
				jch.getShowBuildBadges());

		final XmlFile jenkinsConfig = new XmlFile(
				new File(jenkins.getRootDir(), "config.xml"));
		Assert.assertTrue("Verify a system level configuration is saveable.",
				jch.isSaveable(jenkins, jenkinsConfig));
		// This would more naturally belong in
		// JobConfigHistoryTest.testIsSaveable but Mockito chokes on
		// MavenModuleSet.<clinit>:
		MavenModuleSet mms = new MavenModuleSet(Jenkins.getInstance(), "");
		Assert.assertTrue("MavenModuleSet should be saved",
				jch.isSaveable(mms, mms.getConfigFile()));

		Assert.assertEquals("Verify system configuration history location",
				getHistoryDir(jenkinsConfig).getParentFile(), jch.getConfiguredHistoryRootDir());
		testCreateRenameDeleteProject(jch);
	}

	public void testSkipDuplicateHistory()
			throws IOException, SAXException, Exception {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
		HtmlForm form = webClient.goTo("configure").getFormByName("config");
		submit(form);

		final FreeStyleProject project = createFreeStyleProject("testproject");
		final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(
				project);

		// clear out all history - setting to 1 will clear out all with the
		// expectation that we are creating a new entry
		jch.setMaxHistoryEntries("1");
		project.save();
		Thread.sleep(SLEEP_TIME);
		// reset to empty value
		jch.setMaxHistoryEntries("");
		project.save();
		Thread.sleep(SLEEP_TIME);
		// TODO: why do we have 2 entries after the first save operation?
		final int jobLengthBeforeSave = projectAction.getJobConfigs().size();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(SLEEP_TIME);
			project.save();
		}
		Thread.sleep(SLEEP_TIME);
		Assert.assertEquals(
				"Verify 2 project history entry after 5 duplicate saves.",
				jobLengthBeforeSave, projectAction.getJobConfigs().size());

		// system history test - skip duplicate history -hardcode path to Jenkins
		// config
		final File jenkinsConfigDir = new File(jenkins.root,
				JobConfigHistoryConsts.DEFAULT_HISTORY_DIR + "/config");
		final int configLengthBeforeSave = jenkinsConfigDir
				.listFiles(HistoryFileFilter.INSTANCE).length;
		for (int i = 0; i < 5; i++) {
			Thread.sleep(SLEEP_TIME);
			jenkins.save();
		}
		Assert.assertEquals(
				"Verify system history has still only previous entries after 5 duplicate saves.",
				configLengthBeforeSave,
				jenkinsConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

		// verify non-duplicate history is saved
		project.setDescription("new description");
		project.save();
		Assert.assertEquals("Verify non duplicate project history saved.",
				jobLengthBeforeSave + 1, projectAction.getJobConfigs().size());

		// corrupt history record and verify new entry will be saved
		final File[] historyDirs = getHistoryDir(project.getConfigFile())
				.listFiles(HistoryFileFilter.INSTANCE);
		Arrays.sort(historyDirs, Collections.reverseOrder());
		(new File(historyDirs[0], "config.xml"))
				.renameTo(new File(historyDirs[0], "config"));
		Assert.assertTrue(
				"Verify configuration is saveable when history is corrupted.",
				jch.isSaveable(project, project.getConfigFile()));

		// reconfigure to allow saving duplicate history
		form = webClient.goTo("configure").getFormByName("config");
		form.getInputByName("skipDuplicateHistory").setChecked(false);
		submit(form);

		// perform additional save and verify more than one history entries
		// exist
		Thread.sleep(SLEEP_TIME);
		jenkins.save();
		project.save();
		Assert.assertTrue("Verify duplicate project history entries.",
				projectAction.getJobConfigs().size() >= 2);
		Assert.assertTrue("Verify duplicate system history entries.",
				jenkinsConfigDir
						.listFiles(HistoryFileFilter.INSTANCE).length > 1);
	}

	public void testFormValidation() {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
		try {
			final HtmlForm form = webClient.goTo("configure")
					.getFormByName("config");
			Assert.assertFalse(
					"Check no error message present for history entry.",
					form.getTextContent()
							.contains("Enter a valid positive integer"));
			form.getInputByName("maxHistoryEntries").setValueAttribute("-2");
			Assert.assertTrue("Check error message on invalid history entry.",
					form.getTextContent()
							.contains("Enter a valid positive integer"));
			Assert.assertFalse(
					"Check no error message present for regexp excludePattern.",
					form.getTextContent().contains("Invalid regexp"));
			form.getInputByName("excludePattern").setValueAttribute("**");
			Assert.assertTrue(
					"Check error message on invalid regexp excludePattern.",
					form.getTextContent().contains("Invalid regexp"));
			submit(form);
			Assert.assertEquals("Verify invalid regexp string is saved.", "**",
					jch.getExcludePattern());
			Assert.assertNull("Verify invalid regexp has not been loaded.",
					jch.getExcludeRegexpPattern());

		} catch (Exception e) {
			Assert.fail("Unable to complete form validation: " + e);
		}
	}

	public void testMaxHistoryEntries() {
		try {
			final HtmlForm form = webClient.goTo("configure")
					.getFormByName("config");
			form.getInputByName("maxHistoryEntries").setValueAttribute("5");
			form.getInputByName("skipDuplicateHistory").setChecked(false);
			submit(form);

			final FreeStyleProject project = createFreeStyleProject(
					"testproject");
			final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(
					project);

			Assert.assertTrue("Verify at least 1 history entry created.",
					projectAction.getJobConfigs().size() >= 1);
			for (int i = 0; i < 3; i++) {
				Thread.sleep(SLEEP_TIME);
				project.save();
			}
			Assert.assertTrue("Verify at least 4 history entries.",
					projectAction.getJobConfigs().size() >= 4);

			for (int i = 0; i < 3; i++) {
				Thread.sleep(SLEEP_TIME);
				project.save();
			}
			Assert.assertEquals(
					"Verify no more than 5 history entries created + 1 'Created' entry that won't be deleted.",
					5 + 1, projectAction.getJobConfigs().size());
		} catch (Exception e) {
			Assert.fail("Unable to complete max history entries test: " + e);
		}
	}

	public void testAbsPathHistoryRootDir() throws Exception {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
		// create a unique name, then delete the empty file - will be recreated
		// later
		final File root = File
				.createTempFile("jobConfigHistory.test_abs_path", null)
				.getCanonicalFile();
		final String absolutePath = root.getPath();
		root.delete();

		final HtmlForm form = webClient.goTo("configure")
				.getFormByName("config");
		form.getInputByName("historyRootDir").setValueAttribute(absolutePath);
		submit(form);
		Assert.assertEquals("Verify history root configured at absolute path.",
				new File(root, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR),
				jch.getConfiguredHistoryRootDir());

		// save something
		createFreeStyleProject();
		Assert.assertTrue("Verify history root exists.", root.exists());

		// cleanup - Jenkins doesn't know about these files we created
		root.listFiles(DELETE_FILTER);
		root.delete();
		// not really needed, but helpful so we don't clutter the test host with
		// unnecessary files
		Assert.assertFalse("Verify cleanup of history files: " + root,
				root.exists());
	}

	private void testCreateRenameDeleteProject(final JobConfigHistory jch) {
		try {
			final FreeStyleProject project = createFreeStyleProject(
					"testproject");
			final File jobHistoryRootFile = new File(
					jch.getConfiguredHistoryRootDir(), "jobs");

			final File expectedConfigDir = new File(jobHistoryRootFile,
					"testproject");
			Assert.assertEquals("Verify history dir configured as expected.",
					expectedConfigDir, getHistoryDir(project.getConfigFile()));
			Assert.assertTrue(
					"Verify project config history directory created: "
							+ expectedConfigDir,
					expectedConfigDir.exists());

			// since sometimes two history entries are created, we just check
			// if one of them contains "Created"
			boolean createdEntryFound = false;
			for (File file : expectedConfigDir
					.listFiles(HistoryFileFilter.INSTANCE)) {
				if (new XmlFile(new File(file, "history.xml")).asString()
						.contains("Created")) {
					createdEntryFound = true;
					break;
				}
			}
			Assert.assertTrue(
					"Verify one \'created\' history entry on creation.",
					createdEntryFound);
			final int historyEntryCount = expectedConfigDir
					.listFiles(HistoryFileFilter.INSTANCE).length;

			// sleep so we don't overwrite our existing history directory
			Thread.sleep(SLEEP_TIME);
			project.renameTo("renamed_testproject");

			// verify rename moves the history directory as expected
			Assert.assertFalse(
					"Verify on rename old project config history directory removed.",
					expectedConfigDir.exists());
			final File newExpectedConfigDir = new File(expectedConfigDir
					.toString().replace("testproject", "renamed_testproject"));
			Assert.assertTrue(
					"Verify renamed project config history created: "
							+ newExpectedConfigDir,
					newExpectedConfigDir.exists());
			Assert.assertEquals("Verify two history entries after rename.",
					historyEntryCount + 1, newExpectedConfigDir
							.listFiles(HistoryFileFilter.INSTANCE).length);

			// delete project and verify the history directory is gone
			project.delete();
			Assert.assertFalse(
					"Verify on delete project config history directory removed(renamed): "
							+ newExpectedConfigDir,
					newExpectedConfigDir.exists());

			String deletedDir = null;
			for (File file : expectedConfigDir.getParentFile().listFiles()) {
				if (file.getName().contains("renamed_testproject"
						+ DeletedFileFilter.DELETED_MARKER)) {
					deletedDir = file.getPath();
					break;
				}
			}
			Assert.assertNotNull("Verify config history directory of deleted job exists.", deletedDir);
			Assert.assertTrue(
					"Verify config history directory of deleted job is not empty",
					(new File(deletedDir)).listFiles().length > 0);

			boolean deletedEntryFound = false;
			for (File file : (new File(deletedDir))
					.listFiles(HistoryFileFilter.INSTANCE)) {
				if (new XmlFile(new File(file, "history.xml")).asString()
						.contains("Deleted")) {
					deletedEntryFound = true;
					break;
				}
			}
			Assert.assertTrue("Verify one \'deleted\' history entry exists.",
					deletedEntryFound);

		} catch (IOException e) {
			Assert.fail(
					"Unable to complete project creation/rename test: " + e);
		} catch (InterruptedException e) {
			Assert.fail("Interrupted, unable to test project deletion: " + e);
		}
	}

	/**
	 * Tests if project can still be built after the config history root dir has
	 * been changed. (I.e. the project exists but has no configs.)
	 */
	public void testChangedRootDir() {
		try {
			final FreeStyleProject project = createFreeStyleProject("bla");
			final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(
					project);
			Assert.assertTrue("Verify project history entry is not empty.",
					projectAction.getJobConfigs().size() > 0);

			final HtmlForm form = webClient.goTo("configure")
					.getFormByName("config");
			form.getInputByName("historyRootDir").setValueAttribute("newDir");
			submit(form);

			Assert.assertEquals("Verify project history entry is empty.", 0,
					projectAction.getJobConfigs().size());
			assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

			project.save();
			Thread.sleep(SLEEP_TIME);
			Assert.assertTrue("Verify project history entry is not empty.",
					projectAction.getJobConfigs().size() > 0);
			assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
		} catch (Exception e) {
			Assert.fail("Unable to complete changed root dir test: " + e);
		}
	}

	public void testInputOfMaxHistoryEntries() {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);

		// check good value
		jch.setMaxHistoryEntries("5");
		Assert.assertEquals("Verify maxHistoryEntries set to 5", "5",
				jch.getMaxHistoryEntries());

		// check negative value
		jch.setMaxHistoryEntries("-1");
		Assert.assertEquals("Verify maxHistoryEntries still set to 5", "5",
				jch.getMaxHistoryEntries());

		// check non-number value
		jch.setMaxHistoryEntries("K");
		Assert.assertEquals("Verify maxHistoryEntries still set to 5", "5",
				jch.getMaxHistoryEntries());

		// check empty value
		jch.setMaxHistoryEntries("");
		Assert.assertEquals("Verify maxHistoryEntries empty", "",
				jch.getMaxHistoryEntries());
	}

	public void testInputOfMaxDaysToKeepEntries() {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);

		// check good value
		jch.setMaxDaysToKeepEntries("5");
		Assert.assertEquals("Verify maxDaysToKeepEntries set to 5", "5",
				jch.getMaxDaysToKeepEntries());

		// check negative value
		jch.setMaxDaysToKeepEntries("-1");
		Assert.assertEquals("Verify maxDaysToKeepEntries still set to 5", "5",
				jch.getMaxDaysToKeepEntries());

		// check non-number value
		jch.setMaxDaysToKeepEntries("K");
		Assert.assertEquals("Verify maxDaysToKeepEntries still set to 5", "5",
				jch.getMaxDaysToKeepEntries());

		// check empty value
		jch.setMaxDaysToKeepEntries("");
		Assert.assertEquals("Verify maxDaysToKeepEntries empty", "",
				jch.getMaxDaysToKeepEntries());
	}

	private File getHistoryDir(XmlFile xmlFile) {
		final JobConfigHistory jch = jenkins.getPlugin(JobConfigHistory.class);
		final File configFile = xmlFile.getFile();
		return ((FileHistoryDao) PluginUtils.getHistoryDao(jch))
				.getHistoryDir(configFile);
	}
}
