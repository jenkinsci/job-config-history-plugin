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
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.INSTANCE;
    
    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAcceptRootNotExistent() {
        File file = new File("target/I_DO_NOT_EXIST");
        HistoryFileFilter sut = HistoryFileFilter.INSTANCE;
        assertFalse(sut.accept(file));
    }
    
    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAcceptNoHistoryEntries() {
        File file = unpackResourceZip.getResource("jobs/Test2/");
        HistoryFileFilter sut = HistoryFileFilter.INSTANCE;
        assertFalse(sut.accept(file));
    }

    /**
     * Test of accept method, of class HistoryFileFilter.
     */
    @Test
    public void testAccept() {        
        File file = unpackResourceZip.getResource("config-history/jobs/Test1/2012-11-21_11-29-12/");
        HistoryFileFilter sut = HistoryFileFilter.INSTANCE;
        assertTrue(sut.accept(file));
    }
}
