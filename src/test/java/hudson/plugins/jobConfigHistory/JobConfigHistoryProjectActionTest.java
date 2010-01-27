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

    /**
     * Test method for {@link hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction#getDiffFile(java.lang.String, java.lang.String)}.
     */
    public void testGetDiffFileStringString() {
        final JobConfigHistoryProjectAction action = new JobConfigHistoryProjectAction(null);
        final String s1 = "123\n346";
        final String s2 = "123\n3467";
        assertEquals("\nDiffs:\n\n< 346\n> 3467\n", action.getDiff(s1.split("\n"), s2.split("\n")));
    }

}
