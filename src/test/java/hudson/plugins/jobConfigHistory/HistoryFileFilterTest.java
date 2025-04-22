package hudson.plugins.jobConfigHistory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for HistoryFileFilter.
 *
 * @author Mirko Friedenhagen
 */
class HistoryFileFilterTest {

    private UnpackResourceZip unpackResourceZip;

    @BeforeEach
    void setUp() throws Exception {
        unpackResourceZip = UnpackResourceZip.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (unpackResourceZip != null) {
            unpackResourceZip.cleanUp();
        }
    }

    @Test
    void nonExistingRootShouldNotBeAccepted() {
        File file = new File("target/I_DO_NOT_EXIST");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    @Test
    void noHistoryEntriesShouldNotBeAccepted() {
        File file = unpackResourceZip.getResource("jobs/Test2/");
        assertFalse(HistoryFileFilter.accepts(file));
    }

    @Test
    void validHistoryShouldBeAccepted() {
        final File singleDirectory = unpackResourceZip
                .getResource("config-history/jobs/Test1/2012-11-21_11-29-12/");
        assertTrue(HistoryFileFilter.accepts(singleDirectory));
        final File historyDirectory = unpackResourceZip
                .getResource("config-history/jobs/Test1/");
        assertEquals(5,
                historyDirectory.listFiles(HistoryFileFilter.INSTANCE).length);
    }
}
