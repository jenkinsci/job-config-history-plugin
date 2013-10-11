package hudson.plugins.jobConfigHistory;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;

/**
 *
 * @author Mirko Friedenhagen
 */
public class HistoryFileFilterTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.create();

    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAcceptRootNotExistent() {
        File file = new File("target/I_DO_NOT_EXIST");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAcceptNoHistoryEntries() {
        File file = unpackResourceZip.getResource("jobs/Test2/");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAccept() {
        final File singleDirectory = unpackResourceZip.getResource("config-history/jobs/Test1/2012-11-21_11-29-12/");
        assertTrue(
                HistoryFileFilter.accepts(singleDirectory));
        final File historyDirectory = unpackResourceZip.getResource("config-history/jobs/Test1/");
        assertEquals(
                5,
                historyDirectory.listFiles(HistoryFileFilter.INSTANCE).length);
    }
}
