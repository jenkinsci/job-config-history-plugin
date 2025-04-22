package hudson.plugins.jobConfigHistory;

import org.htmlunit.html.HtmlForm;
import hudson.XmlFile;
import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author jborghi@cisco.com
 */
@WithJenkins
class JobConfigHistoryIT
        extends
        AbstractHudsonTestCaseDeletingInstanceDir {

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
    private JenkinsRule.WebClient webClient;

    @Override
    void setUp(JenkinsRule rule) throws Exception {
        super.setUp(rule);
        rule.jenkins.setSecurityRealm(
                new HudsonPrivateSecurityRealm(true, false, null));
        webClient = rule.createWebClient();
    }

    @Test
    void testJobConfigHistoryPreConfigured() {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        assertDoesNotThrow(() -> {
            final HtmlForm form = webClient.goTo("configure")
                    .getFormByName("config");
            form.getInputByName("maxHistoryEntries").setValue("10");
            form.getInputByName("saveModuleConfiguration").setChecked(false);
            form.getInputByName("skipDuplicateHistory").setChecked(false);
            form.getInputByName("excludePattern")
                    .setValue(JobConfigHistoryConsts.DEFAULT_EXCLUDE);
            form.getInputByName("historyRootDir")
                    .setValue("jobConfigHistory");
            form.getInputByValue("never").setChecked(true);
            rule.submit(form);
        }, "unable to configure Jenkins instance ");
        assertEquals("10",
                jch.getMaxHistoryEntries(),
                "Verify history entries to keep setting.");
        assertFalse(jch.getSaveModuleConfiguration(),
                "Verify Maven module configuration setting.");
        assertFalse(jch.getSkipDuplicateHistory(),
                "Verify skip duplicate history setting.");
        assertEquals(new File(rule.jenkins.root + "/jobConfigHistory/"
                        + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR),
                jch.getConfiguredHistoryRootDir(),
                "Verify configured history root directory.");
        assertEquals(JobConfigHistoryConsts.DEFAULT_EXCLUDE,
                jch.getExcludePattern(),
                "Verify exclude pattern setting.");
        assertEquals("never",
                jch.getShowBuildBadges(),
                "Verify build badges setting.");

        final XmlFile jenkinsConfig = new XmlFile(
                new File(rule.jenkins.getRootDir(), "config.xml"));
        assertTrue(jch.isSaveable(rule.jenkins, jenkinsConfig),
                "Verify a system level configuration is saveable.");

        assertEquals(getHistoryDir(jenkinsConfig).getParentFile(), jch.getConfiguredHistoryRootDir(), "Verify system configuration history location");
        testCreateRenameDeleteProject(jch);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> getHistoryDir(new XmlFile(new File("/tmp"))));
        assertNotNull(e, "Expected IAE");
        assertFalse(
                jch.isSaveable(null, new XmlFile(new File("/tmp/config.xml"))),
                "Verify false when testing if a file outside of JENKINS_ROOT is saveable.");
    }

    @Test
    void testJobConfigHistoryDefaults() {
        final JobConfigHistory jch = PluginUtils.getPlugin();

        assertNull(
                jch.getMaxHistoryEntries(),
                "Verify number of history entries to keep default setting.");
        assertFalse(jch.getSaveModuleConfiguration(),
                "Verify Maven module configuration default setting.");
        assertTrue(jch.getSkipDuplicateHistory(),
                "Verify skip duplicate history default setting.");
        assertNull(jch.getExcludePattern(),
                "Verify unconfigured exclude pattern.");
        assertEquals("always",
                jch.getShowBuildBadges(),
                "Verify build badges setting.");

        final XmlFile jenkinsConfig = new XmlFile(
                new File(rule.jenkins.getRootDir(), "config.xml"));
        assertTrue(jch.isSaveable(rule.jenkins, jenkinsConfig),
                "Verify a system level configuration is saveable.");
        // This would more naturally belong in
        // JobConfigHistoryTest.testIsSaveable but Mockito chokes on
        // MavenModuleSet.<clinit>:
        MavenModuleSet mms = new MavenModuleSet(Jenkins.get(), "");
        assertTrue(jch.isSaveable(mms, mms.getConfigFile()),
                "MavenModuleSet should be saved");

        assertEquals(getHistoryDir(jenkinsConfig).getParentFile(), jch.getConfiguredHistoryRootDir(), "Verify system configuration history location");
        testCreateRenameDeleteProject(jch);
    }

    @Test
    void testSkipDuplicateHistory()
            throws Exception {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        HtmlForm form = webClient.goTo("configure").getFormByName("config");
        rule.submit(form);

        final FreeStyleProject project = rule.createFreeStyleProject("testproject");
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
        assertEquals(
                jobLengthBeforeSave, projectAction.getJobConfigs().size(), "Verify 2 project history entry after 5 duplicate saves.");

        // system history test - skip duplicate history -hardcode path to Jenkins
        // config
        final File jenkinsConfigDir = new File(rule.jenkins.root,
                JobConfigHistoryConsts.DEFAULT_HISTORY_DIR + "/config");
        final int configLengthBeforeSave = jenkinsConfigDir
                .listFiles(HistoryFileFilter.INSTANCE).length;
        for (int i = 0; i < 5; i++) {
            Thread.sleep(SLEEP_TIME);
            rule.jenkins.save();
        }
        assertEquals(
                configLengthBeforeSave,
                jenkinsConfigDir.listFiles(HistoryFileFilter.INSTANCE).length,
                "Verify system history has still only previous entries after 5 duplicate saves.");

        // verify non-duplicate history is saved
        project.setDescription("new description");
        project.save();
        assertEquals(jobLengthBeforeSave + 1, projectAction.getJobConfigs().size(), "Verify non duplicate project history saved.");

        // corrupt history record and verify new entry will be saved
        final File[] historyDirs = getHistoryDir(project.getConfigFile())
                .listFiles(HistoryFileFilter.INSTANCE);
        Arrays.sort(historyDirs, Collections.reverseOrder());
        (new File(historyDirs[0], "config.xml"))
                .renameTo(new File(historyDirs[0], "config"));
        assertTrue(
                jch.isSaveable(project, project.getConfigFile()),
                "Verify configuration is saveable when history is corrupted.");

        // reconfigure to allow saving duplicate history
        form = webClient.goTo("configure").getFormByName("config");
        form.getInputByName("skipDuplicateHistory").setChecked(false);
        rule.submit(form);

        // perform additional save and verify more than one history entries
        // exist
        Thread.sleep(SLEEP_TIME);
        rule.jenkins.save();
        project.save();
        assertTrue(projectAction.getJobConfigs().size() >= 2,
                "Verify duplicate project history entries.");
        assertTrue(jenkinsConfigDir
                        .listFiles(HistoryFileFilter.INSTANCE).length > 1,
                "Verify duplicate system history entries.");
    }

    @Test
    void testFormValidation() {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        assertDoesNotThrow(() -> {
            final HtmlForm form = webClient.goTo("configure")
                    .getFormByName("config");
            assertFalse(
                    form.getTextContent()
                            .contains("Enter a valid positive integer"),
                    "Check no error message present for history entry.");
            form.getInputByName("maxHistoryEntries").setValue("-2");
            assertTrue(form.getTextContent()
                            .contains("Enter a valid positive integer"),
                    "Check error message on invalid history entry.");
            assertFalse(
                    form.getTextContent().contains("Invalid regexp"),
                    "Check no error message present for regexp excludePattern.");
            form.getInputByName("excludePattern").setValue("**");
            assertTrue(
                    form.getTextContent().contains("Invalid regexp"),
                    "Check error message on invalid regexp excludePattern.");
            rule.submit(form);
            assertEquals("**",
                    jch.getExcludePattern(),
                    "Verify invalid regexp string is saved.");
            assertNull(jch.getExcludeRegexpPattern(),
                    "Verify invalid regexp has not been loaded.");

        }, "Unable to complete form validation: ");
    }

    @Test
    void testMaxHistoryEntries() {
        assertDoesNotThrow(() -> {
            final HtmlForm form = webClient.goTo("configure")
                    .getFormByName("config");
            form.getInputByName("maxHistoryEntries").setValue("5");
            form.getInputByName("skipDuplicateHistory").setChecked(false);
            rule.submit(form);

            final FreeStyleProject project = rule.createFreeStyleProject(
                    "testproject");
            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(
                    project);

            assertFalse(projectAction.getJobConfigs().isEmpty(), "Verify at least 1 history entry created.");
            for (int i = 0; i < 3; i++) {
                Thread.sleep(SLEEP_TIME);
                project.save();
            }
            assertTrue(projectAction.getJobConfigs().size() >= 4,
                    "Verify at least 4 history entries.");

            for (int i = 0; i < 3; i++) {
                Thread.sleep(SLEEP_TIME);
                project.save();
            }
            assertEquals(
                    5 + 1, projectAction.getJobConfigs().size(), "Verify no more than 5 history entries created + 1 'Created' entry that won't be deleted.");
        }, "Unable to complete max history entries test: ");
    }

    @Test
    void testAbsPathHistoryRootDir() throws Exception {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        // create a unique name, then delete the empty file - will be recreated
        // later
        final File root = File
                .createTempFile("jobConfigHistory.test_abs_path", null)
                .getCanonicalFile();
        final String absolutePath = root.getPath();
        root.delete();

        final HtmlForm form = webClient.goTo("configure")
                .getFormByName("config");
        form.getInputByName("historyRootDir").setValue(absolutePath);
        rule.submit(form);
        assertEquals(new File(root, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR),
                jch.getConfiguredHistoryRootDir(),
                "Verify history root configured at absolute path.");

        // save something
        rule.createFreeStyleProject();
        assertTrue(root.exists(), "Verify history root exists.");

        // cleanup - Jenkins doesn't know about these files we created
        root.listFiles(DELETE_FILTER);
        root.delete();
        // not really needed, but helpful so we don't clutter the test host with
        // unnecessary files
        assertFalse(root.exists(),
                "Verify cleanup of history files: " + root);
    }

    private void testCreateRenameDeleteProject(final JobConfigHistory jch) {
        try {
            final FreeStyleProject project = rule.createFreeStyleProject(
                    "testproject");
            final File jobHistoryRootFile = new File(
                    jch.getConfiguredHistoryRootDir(), "jobs");

            final File expectedConfigDir = new File(jobHistoryRootFile,
                    "testproject");
            assertEquals(expectedConfigDir, getHistoryDir(project.getConfigFile()), "Verify history dir configured as expected.");
            assertTrue(
                    expectedConfigDir.exists(),
                    "Verify project config history directory created: "
                            + expectedConfigDir);

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
            assertTrue(
                    createdEntryFound,
                    "Verify one 'created' history entry on creation.");
            final int historyEntryCount = expectedConfigDir
                    .listFiles(HistoryFileFilter.INSTANCE).length;

            // sleep so we don't overwrite our existing history directory
            Thread.sleep(SLEEP_TIME);
            project.renameTo("renamed_testproject");

            // verify rename moves the history directory as expected
            assertFalse(
                    expectedConfigDir.exists(),
                    "Verify on rename old project config history directory removed.");
            final File newExpectedConfigDir = new File(expectedConfigDir
                    .toString().replace("testproject", "renamed_testproject"));
            assertTrue(
                    newExpectedConfigDir.exists(),
                    "Verify renamed project config history created: "
                            + newExpectedConfigDir);
            assertEquals(historyEntryCount + 1, newExpectedConfigDir
                            .listFiles(HistoryFileFilter.INSTANCE).length, "Verify two history entries after rename.");

            // delete project and verify the history directory is gone
            project.delete();
            assertFalse(
                    newExpectedConfigDir.exists(),
                    "Verify on delete project config history directory removed(renamed): "
                            + newExpectedConfigDir);

            String deletedDir = null;
            for (File file : expectedConfigDir.getParentFile().listFiles()) {
                if (file.getName().contains("renamed_testproject"
                        + DeletedFileFilter.DELETED_MARKER)) {
                    deletedDir = file.getPath();
                    break;
                }
            }
            assertNotNull(deletedDir, "Verify config history directory of deleted job exists.");
            assertTrue(
                    (new File(deletedDir)).listFiles().length > 0,
                    "Verify config history directory of deleted job is not empty");

            boolean deletedEntryFound = false;
            for (File file : (new File(deletedDir))
                    .listFiles(HistoryFileFilter.INSTANCE)) {
                if (new XmlFile(new File(file, "history.xml")).asString()
                        .contains("Deleted")) {
                    deletedEntryFound = true;
                    break;
                }
            }
            assertTrue(deletedEntryFound,
                    "Verify one 'deleted' history entry exists.");

        } catch (IOException e) {
            fail(
                    "Unable to complete project creation/rename test: " + e);
        } catch (InterruptedException e) {
            fail("Interrupted, unable to test project deletion: " + e);
        }
    }

    /**
     * Tests if project can still be built after the config history root dir has
     * been changed. (I.e. the project exists but has no configs.)
     */
    @Test
    void testChangedRootDir() {
        assertDoesNotThrow(() -> {
            final FreeStyleProject project = rule.createFreeStyleProject("bla");
            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(
                    project);
            assertFalse(projectAction.getJobConfigs().isEmpty(), "Verify project history entry is not empty.");

            final HtmlForm form = webClient.goTo("configure")
                    .getFormByName("config");
            form.getInputByName("historyRootDir").setValue("newDir");
            rule.submit(form);

            assertEquals(0,
                    projectAction.getJobConfigs().size(),
                    "Verify project history entry is empty.");
            rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

            project.save();
            Thread.sleep(SLEEP_TIME);
            assertFalse(projectAction.getJobConfigs().isEmpty(), "Verify project history entry is not empty.");
            rule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        }, "Unable to complete changed root dir test: ");
    }

    @Test
    void testInputOfMaxHistoryEntries() {
        final JobConfigHistory jch = PluginUtils.getPlugin();

        // check good value
        jch.setMaxHistoryEntries("5");
        assertEquals("5",
                jch.getMaxHistoryEntries(),
                "Verify maxHistoryEntries set to 5");

        // check negative value
        jch.setMaxHistoryEntries("-1");
        assertEquals("5",
                jch.getMaxHistoryEntries(),
                "Verify maxHistoryEntries still set to 5");

        // check non-number value
        jch.setMaxHistoryEntries("K");
        assertEquals("5",
                jch.getMaxHistoryEntries(),
                "Verify maxHistoryEntries still set to 5");

        // check empty value
        jch.setMaxHistoryEntries("");
        assertEquals("",
                jch.getMaxHistoryEntries(),
                "Verify maxHistoryEntries empty");
    }

    @Test
    void testInputOfMaxDaysToKeepEntries() {
        final JobConfigHistory jch = PluginUtils.getPlugin();

        // check good value
        jch.setMaxDaysToKeepEntries("5");
        assertEquals("5",
                jch.getMaxDaysToKeepEntries(),
                "Verify maxDaysToKeepEntries set to 5");

        // check negative value
        jch.setMaxDaysToKeepEntries("-1");
        assertEquals("5",
                jch.getMaxDaysToKeepEntries(),
                "Verify maxDaysToKeepEntries still set to 5");

        // check non-number value
        jch.setMaxDaysToKeepEntries("K");
        assertEquals("5",
                jch.getMaxDaysToKeepEntries(),
                "Verify maxDaysToKeepEntries still set to 5");

        // check empty value
        jch.setMaxDaysToKeepEntries("");
        assertEquals("",
                jch.getMaxDaysToKeepEntries(),
                "Verify maxDaysToKeepEntries empty");
    }

    private File getHistoryDir(XmlFile xmlFile) {
        final JobConfigHistory jch = PluginUtils.getPlugin();
        final File configFile = xmlFile.getFile();
        return ((FileHistoryDao) PluginUtils.getHistoryDao(jch))
                .getHistoryDir(configFile);
    }
}
