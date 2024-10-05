package hudson.plugins.jobConfigHistory;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for HistoryFileFilter.
 *
 * @author Mirko Friedenhagen
 */
public class HistoryFileFilterTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip
            .create();

    @Test
    public void nonExistingRootShouldNotBeAccepted() {
        File file = new File("target/I_DO_NOT_EXIST");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    @Test
    public void noHistoryEntriesShouldNotBeAccepted() {
        File file = unpackResourceZip.getResource("jobs/Test2/");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    @Test
    public void validHistoryShouldBeAccepted() {
        final File singleDirectory = unpackResourceZip
                .getResource("config-history/jobs/Test1/2012-11-21_11-29-12/");
        assertTrue(HistoryFileFilter.accepts(singleDirectory));
        final File historyDirectory = unpackResourceZip
                .getResource("config-history/jobs/Test1/");
        assertEquals(5,
                historyDirectory.listFiles(HistoryFileFilter.INSTANCE).length);
    }
}
