package hudson.plugins.jobConfigHistory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
/**
 *
 * @author Mirko Friedenhagen
 */
public class ParsedDateComparatorTest {

    private static final String DATE = "2012-11-21_11-29-12";

    private static final String DATE_NEWER = "2012-11-21_11-29-14";

    private final HistoryDescr historyDescr = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE);

    private final HistoryDescr historyDescrClone = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE);

    private final HistoryDescr historyDescrNewer = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE_NEWER);

    /**
     * Test of compare method, of class ParsedDateComparator.
     */
    @Test
    public void testCompare() {
        assertEquals(0, ParsedDateComparator.DESCENDING.compare(historyDescr, historyDescrClone));
        assertEquals(0, ParsedDateComparator.DESCENDING.compare(historyDescrClone, historyDescr));
    }

    /**
     * Test of compare method, of class ParsedDateComparator.
     */
    @Test
    public void testSortAndMin() {
        final List<HistoryDescr> list = Arrays.asList(historyDescr, historyDescrNewer);
        Collections.sort(list, ParsedDateComparator.DESCENDING);
        assertEquals(historyDescr, list.get(1));
        assertEquals(historyDescrNewer, list.get(0));
        assertEquals(historyDescrNewer, Collections.min(list, ParsedDateComparator.DESCENDING));
    }
}
