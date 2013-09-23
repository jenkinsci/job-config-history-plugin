package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.security.HudsonPrivateSecurityRealm;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.gargoylesoftware.htmlunit.html.HtmlForm;

import org.xml.sax.SAXException;

/**
 * @author jborghi@cisco.com
 *
 */
public class JobConfigHistoryIT extends AbstractHudsonTestCaseDeletingInstanceDir {

    private WebClient webClient;
    // we need to sleep between saves so we don't overwrite the history directories
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
    protected void setUp() throws Exception {
        super.setUp();
        hudson.setSecurityRealm(new HudsonPrivateSecurityRealm(true, false, null));
        webClient = new WebClient();
    }

    public void testJobConfigHistoryPreConfigured() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        try {
            final HtmlForm form = webClient.goTo("configure").getFormByName("config");
            form.getInputByName("maxHistoryEntries").setValueAttribute("10");
            form.getInputByName("saveModuleConfiguration").setChecked(false);
            form.getInputByName("skipDuplicateHistory").setChecked(false);
            form.getInputByName("excludePattern").setValueAttribute(JobConfigHistoryConsts.DEFAULT_EXCLUDE);
            form.getInputByName("historyRootDir").setValueAttribute("jobConfigHistory");
            form.getInputByValue("never").setChecked(true);
            submit(form);
        } catch (Exception e) {
            fail("unable to configure Hudson instance " + e);
        }
        assertEquals("Verify history entries to keep setting.", "10", jch.getMaxHistoryEntries());
        assertFalse("Verify Maven module configuration setting.", jch.getSaveModuleConfiguration());
        assertFalse("Verify skip duplicate history setting.", jch.getSkipDuplicateHistory());
        assertEquals("Verify configured history root directory.", new File(hudson.root + "/jobConfigHistory/" + JobConfigHistoryConsts.DEFAULT_HISTORY_DIR), jch.getConfiguredHistoryRootDir());
        assertEquals("Verify exclude pattern setting.", JobConfigHistoryConsts.DEFAULT_EXCLUDE, jch.getExcludePattern());
        assertEquals("Verify build badges setting.", "never", jch.getShowBuildBadges());

        final XmlFile hudsonConfig = new XmlFile(new File(hudson.getRootDir(), "config.xml"));
        assertTrue("Verify a system level configuration is saveable.", jch.isSaveable(hudson, hudsonConfig));

        assertTrue("Verify system configuration history location", getHistoryDir(hudsonConfig).getParentFile().equals(jch.getConfiguredHistoryRootDir()));
        testCreateRenameDeleteProject(jch);
        try {
            getHistoryDir(new XmlFile(new File("/tmp")));
            fail("Verify IAE when attempting to get history dir for a file outside of HUDSON_ROOT.");
        } catch (IllegalArgumentException e) {
            assertNotNull("Expected IAE", e);
        }
        assertFalse("Verify false when testing if a file outside of HUDSON_ROOT is saveable.", jch.isSaveable(null, new XmlFile(new File("/tmp/config.xml"))));
    }

    public void testJobConfigHistoryDefaults() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);

        assertNull("Verify number of history entries to keep default setting.", jch.getMaxHistoryEntries());
        assertTrue("Verify Maven module configuration default setting.", jch.getSaveModuleConfiguration());
        assertTrue("Verify skip duplicate history default setting.", jch.getSkipDuplicateHistory());
        assertNull("Verify unconfigured exclude pattern.", jch.getExcludePattern());
        assertEquals("Verify build badges setting.", "always", jch.getShowBuildBadges());

        final XmlFile hudsonConfig = new XmlFile(new File(hudson.getRootDir(), "config.xml"));
        assertTrue("Verify a system level configuration is saveable.", jch.isSaveable(hudson, hudsonConfig));

        assertTrue("Verify system configuration history location", getHistoryDir(hudsonConfig).getParentFile().equals(jch.getConfiguredHistoryRootDir()));
        testCreateRenameDeleteProject(jch);
    }

    public void testSkipDuplicateHistory() throws IOException, SAXException, Exception {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        HtmlForm form = webClient.goTo("configure").getFormByName("config");
        submit(form);

        final FreeStyleProject project = createFreeStyleProject("testproject");
        final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);

        // clear out all history - setting to 1 will clear out all with the expectation that we are creating a new entry
        jch.setMaxHistoryEntries("1");

        // reset to empty value
        jch.setMaxHistoryEntries("");
        for (int i = 0; i < 3; i++) {
            Thread.sleep(SLEEP_TIME);
            project.save();
        }
        assertEquals("Verify 1 project history entry after 3 duplicate saves.", 1, projectAction.getJobConfigs().size());

        // system history test - skip duplicate history -hardcode path to Hudson config
        final File hudsonConfigDir = new File(hudson.root, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR + "/config");
        for (int i = 0; i < 3; i++) {
            Thread.sleep(SLEEP_TIME);
            hudson.save();
        }
        assertEquals("Verify 1 system history entry after 3 duplicate saves.", 1, hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

        // verify non-duplicate history is saved
        project.setDescription("new description");
        project.save();
        assertEquals("Verify non duplicate project history saved.", 2, projectAction.getJobConfigs().size());

        // corrupt history record and verify new entry will be saved
        final File[] historyDirs = getHistoryDir(project.getConfigFile()).listFiles(HistoryFileFilter.INSTANCE);
        Arrays.sort(historyDirs, Collections.reverseOrder());
        (new File(historyDirs[0], "config.xml")).renameTo(new File(historyDirs[0], "config"));
        assertNull("Verify history dir is corrupted.", jch.getConfigFile(historyDirs[0]));
        assertTrue("Verify configuration is saveable when history is corrupted.", jch.isSaveable(project, project.getConfigFile()));

        // reconfigure to allow saving duplicate history
        form = webClient.goTo("configure").getFormByName("config");
        form.getInputByName("skipDuplicateHistory").setChecked(false);
        submit(form);

        // perform additional save and verify more than one history entries exist
        Thread.sleep(SLEEP_TIME);
        hudson.save();
        project.save();
        assertTrue("Verify duplicate project history entries.", projectAction.getJobConfigs().size() >= 2);
        assertTrue("Verify duplicate system history entries.", hudsonConfigDir.listFiles(HistoryFileFilter.INSTANCE).length > 1);
    }

    public void testFormValidation() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        try {
            final HtmlForm form = webClient.goTo("configure").getFormByName("config");
            assertFalse("Check no error message present for history entry.", form.getTextContent().contains("Enter a valid positive integer"));
            form.getInputByName("maxHistoryEntries").setValueAttribute("-2");
            assertTrue("Check error message on invalid history entry.", form.getTextContent().contains("Enter a valid positive integer"));
            assertFalse("Check no error messgae present for regexp excludePattern.", form.getTextContent().contains("Invalid regexp"));
            form.getInputByName("excludePattern").setValueAttribute("**");
            assertTrue("Check error message on invalid regexp excludePattern.", form.getTextContent().contains("Invalid regexp"));
            submit(form);
            assertEquals("Verify invalid regexp string is saved.", "**", jch.getExcludePattern());
            assertNull("Verify invalid regexp has not been loaded.", jch.getExcludeRegexpPattern());

        } catch (Exception e) {
            fail("Unable to complete form validation: " + e);
        }
    }

    public void testMaxHistoryEntries() {
        try {
            final HtmlForm form = webClient.goTo("configure").getFormByName("config");
            form.getInputByName("maxHistoryEntries").setValueAttribute("5");
            form.getInputByName("skipDuplicateHistory").setChecked(false);
            submit(form);

            final FreeStyleProject project = createFreeStyleProject("testproject");
            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);

            assertTrue("Verify at least 1 history entry created.", projectAction.getJobConfigs().size() >= 1);
            for (int i = 0; i < 3; i++) {
                Thread.sleep(SLEEP_TIME);
                project.save();
            }
            assertTrue("Verify at least 4 history entries.", projectAction.getJobConfigs().size() >= 4);

            for (int i = 0; i < 3; i++) {
                Thread.sleep(SLEEP_TIME);
                project.save();
            }
            assertEquals("Verify no more than 5 history entries created + 1 'Created' entry that won't be deleted.",
                            5+1, projectAction.getJobConfigs().size());
        } catch (Exception e) {
            fail("Unable to complete max history entries test: " + e);
        }
    }

    public void testAbsPathHistoryRootDir() throws Exception {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        // create a unique name, then delete the empty file - will be recreated later
        final File root = File.createTempFile("jobConfigHistory.test_abs_path", null).getCanonicalFile();
        final String absolutePath = root.getPath();
        root.delete();

        final HtmlForm form = webClient.goTo("configure").getFormByName("config");
        form.getInputByName("historyRootDir").setValueAttribute(absolutePath);
        submit(form);
        assertEquals("Verify history root configured at absolute path.", new File(root, JobConfigHistoryConsts.DEFAULT_HISTORY_DIR), jch.getConfiguredHistoryRootDir());

        // save something
        createFreeStyleProject();
        assertTrue("Verify history root exists.", root.exists());

        // cleanup - Hudson doesn't know about these files we created
        root.listFiles(DELETE_FILTER);
        root.delete();
        // not really needed, but helpful so we don't clutter the test host with unnecessary files
        assertFalse("Verify cleanup of history files: " + root, root.exists());
    }

    private void testCreateRenameDeleteProject(final JobConfigHistory jch) {
        try {
            final FreeStyleProject project = createFreeStyleProject("testproject");
            final File jobHistoryRootFile = jch.getJobHistoryRootDir();

            final File expectedConfigDir = new File(jobHistoryRootFile, "testproject");
            assertEquals("Verify history dir configured as expected.", expectedConfigDir, getHistoryDir(project.getConfigFile()));
            assertTrue("Verify project config history directory created: " + expectedConfigDir, expectedConfigDir.exists());

            //since sometimes two history entries are created, we just check
            //if one of them contains "Created"
            boolean createdEntryFound = false;
            for (File file : expectedConfigDir.listFiles(HistoryFileFilter.INSTANCE)) {
                if (new XmlFile(new File(file, "history.xml")).asString().contains("Created")) {
                    createdEntryFound = true;
                    break;
                }
            }
            assertTrue("Verify one \'created\' history entry on creation.", createdEntryFound);
            final int historyEntryCount = expectedConfigDir.listFiles(HistoryFileFilter.INSTANCE).length;

            // sleep so we don't overwrite our existing history directory
            Thread.sleep(SLEEP_TIME);
            project.renameTo("renamed_testproject");

            // verify rename moves the history directory as expected
            assertFalse("Verify on rename old project config history directory removed.", expectedConfigDir.exists());
            final File newExpectedConfigDir = new File(expectedConfigDir.toString().replace("testproject", "renamed_testproject"));
            assertTrue("Verify renamed project config history created: " + newExpectedConfigDir, newExpectedConfigDir.exists());
            assertEquals("Verify two history entries after rename.", historyEntryCount + 1, newExpectedConfigDir.listFiles(HistoryFileFilter.INSTANCE).length);

            // delete project and verify the history directory is gone
            project.delete();
            assertFalse("Verify on delete project config history directory removed(renamed): " + newExpectedConfigDir, newExpectedConfigDir.exists());

            String deletedDir = null;
            for (File file : expectedConfigDir.getParentFile().listFiles()) {
                if (file.getName().contains("renamed_testproject" + JobConfigHistoryConsts.DELETED_MARKER)) {
                    deletedDir = file.getPath();
                    break;
                }
            }
            assertTrue("Verify config history directory of deleted job exists.", deletedDir != null);
            assertTrue("Verify config history directory of deleted job is not empty", (new File(deletedDir)).listFiles().length > 0);

            boolean deletedEntryFound = false;
            for (File file : (new File(deletedDir)).listFiles(HistoryFileFilter.INSTANCE)) {
                if (new XmlFile(new File(file, "history.xml")).asString().contains("Deleted")) {
                    deletedEntryFound = true;
                    break;
                }
            }
            assertTrue("Verify one \'deleted\' history entry exists.", deletedEntryFound);

        } catch (IOException e) {
            fail("Unable to complete project creation/rename test: " + e);
        } catch (InterruptedException e) {
            fail("Interrupted, unable to test project deletion: " + e);
        }
    }

    /**
     * Tests if project can still be built after the config history root dir has been changed.
     * (I.e. the project exists but has no configs.)
     */
    public void testChangedRootDir() {
        try {
            final FreeStyleProject project = createFreeStyleProject("bla");
            final JobConfigHistoryProjectAction projectAction = new JobConfigHistoryProjectAction(project);
            assertTrue("Verify project history entry is not empty.", projectAction.getJobConfigs().size() > 0);

            final HtmlForm form = webClient.goTo("configure").getFormByName("config");
            form.getInputByName("historyRootDir").setValueAttribute("newDir");
            submit(form);

            assertEquals("Verify project history entry is empty.", 0, projectAction.getJobConfigs().size());
            assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());

            project.save();
            Thread.sleep(SLEEP_TIME);
            assertTrue("Verify project history entry is not empty.", projectAction.getJobConfigs().size() > 0);
            assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0).get());
        } catch (Exception e) {
            fail("Unable to complete changed root dir test: " + e);
        }
    }
    
    public void testInputOfMaxHistoryEntries() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        
        // check good value
        jch.setMaxHistoryEntries("5");
        assertEquals("Verify maxHistoryEntries set to 5", "5", jch.getMaxHistoryEntries());

        // check negative value
        jch.setMaxHistoryEntries("-1");
        assertEquals("Verify maxHistoryEntries still set to 5", "5", jch.getMaxHistoryEntries());
        
        // check non-number value
        jch.setMaxHistoryEntries("K");
        assertEquals("Verify maxHistoryEntries still set to 5", "5", jch.getMaxHistoryEntries());

        // check empty value
        jch.setMaxHistoryEntries("");
        assertEquals("Verify maxHistoryEntries empty", "", jch.getMaxHistoryEntries());
    }
    
    public void testInputOfMaxDaysToKeepEntries() {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        
        // check good value
        jch.setMaxDaysToKeepEntries("5");
        assertEquals("Verify maxDaysToKeepEntries set to 5", "5", jch.getMaxDaysToKeepEntries());

        // check negative value
        jch.setMaxDaysToKeepEntries("-1");
        assertEquals("Verify maxDaysToKeepEntries still set to 5", "5", jch.getMaxDaysToKeepEntries());
        
        // check non-number value
        jch.setMaxDaysToKeepEntries("K");
        assertEquals("Verify maxDaysToKeepEntries still set to 5", "5", jch.getMaxDaysToKeepEntries());

        // check empty value
        jch.setMaxDaysToKeepEntries("");
        assertEquals("Verify maxDaysToKeepEntries empty", "", jch.getMaxDaysToKeepEntries());
    }

    private File getHistoryDir(XmlFile xmlFile) {
        final JobConfigHistory jch = hudson.getPlugin(JobConfigHistory.class);
        final File configFile = xmlFile.getFile();
        return ((FileHistoryDao) jch.getHistoryDao()).getHistoryDir(configFile);
    }
}