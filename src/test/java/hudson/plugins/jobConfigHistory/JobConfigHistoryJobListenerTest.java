/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.model.FreeStyleProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

/**
 * @author mirko
 *
 */
public class JobConfigHistoryJobListenerTest extends HudsonTestCase {

    private File jobsDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jobsDir = new File(hudson.root, "jobs");
    }

    public void testCreation() throws IOException, SAXException {
        createFreeStyleProject("newjob");
        final List<File> historyFiles = Arrays.asList(new File(jobsDir, "newjob/config-history").listFiles());
        assertEquals(historyFiles.toString(), 1, historyFiles.size());
    }

    public void testRename() throws IOException, SAXException {
        final FreeStyleProject project = createFreeStyleProject("newjob");
        project.renameTo("renamedjob");
        final File[] historyFiles = new File(jobsDir, "newjob/config-history").listFiles();
        assertNull("Got history files for old job", historyFiles);
        final List<File> historyFilesNew = Arrays.asList(new File(jobsDir, "renamedjob/config-history").listFiles());
        assertEquals(historyFilesNew.toString(), 1, historyFilesNew.size());
    }
}
