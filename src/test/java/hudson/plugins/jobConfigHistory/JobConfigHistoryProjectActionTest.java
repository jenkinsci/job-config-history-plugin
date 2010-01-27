/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import junit.framework.TestCase;

/**
 * @author mirko
 *
 */
public class JobConfigHistoryProjectActionTest extends TestCase {

    private final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(null);

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringSameLineLength() {
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals("\nDiffs:\n\n< 346\n> 3467\n", action.getDiff(s1.split("\n"), s2.split("\n")));
    }

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringEmpty() {
        assertEquals("\nDiffs:\n\n", action.getDiff(null, null));
        assertEquals("\nDiffs:\n\n", action.getDiff(new String[0], new String[0]));
    }
    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringStringDifferentLineLength() {
        assertEquals("\nDiffs:\n\n", action.getDiff("123\n346".split("\n"), "123\n346\n".split("\n")));
        assertEquals("\nDiffs:\n\n> 123\n", action.getDiff("123\n346".split("\n"), "123\n346\n123".split("\n")));
    }
}
