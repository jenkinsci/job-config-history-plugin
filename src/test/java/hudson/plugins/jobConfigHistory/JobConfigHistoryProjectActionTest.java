/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author mirko
 *
 */
public class JobConfigHistoryProjectActionTest extends TestCase {

    private final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(null);

    private final File file1 = new File("old/config.xml");

    private final File file2 = new File("new/config.xml");

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringSameLineLength() {
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,2 @@\n 123\n-346\n+3467\n", action.getDiff(file1, file2, s1.split("\n"), s2.split("\n")));
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringEmpty() {
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", action.getDiff(file1, file2, new String[0], new String[0]));
    }
    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringDifferentLineLength() {
        assertEquals("--- old/config.xml\n+++ new/config.xml\n", action.getDiff(file1, file2, "123\n346".split("\n"), "123\n346\n".split("\n")));
        assertEquals("--- old/config.xml\n+++ new/config.xml\n@@ -1,2 +1,3 @@\n 123\n 346\n+123\n", action.getDiff(file1, file2, "123\n346".split("\n"), "123\n346\n123".split("\n")));
    }
}
