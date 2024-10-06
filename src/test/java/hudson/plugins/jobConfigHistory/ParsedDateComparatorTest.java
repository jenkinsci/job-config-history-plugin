package hudson.plugins.jobConfigHistory;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for ParsedDateComparator.
 *
 * @author Mirko Friedenhagen
 */
public class ParsedDateComparatorTest {

    private static final String DATE = "2012-11-21_11-29-12";

    private static final String DATE_NEWER = "2012-11-21_11-29-14";

    private final HistoryDescr historyDescr = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE, null, null);

    private final HistoryDescr historyDescrClone = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE, null, null);

    private final HistoryDescr historyDescrNewer = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE_NEWER, null,
            null);

    @Test
    public void objectsWithSameValuesShouldBeEqual() {
        assertEquals(0, ParsedDateComparator.DESCENDING.compare(historyDescr,
                historyDescrClone));
        assertEquals(0, ParsedDateComparator.DESCENDING
                .compare(historyDescrClone, historyDescr));
    }

    @Test
    public void historyDescrShouldBeSortedCorrectly() {
        final List<HistoryDescr> list = Arrays.asList(historyDescr,
                historyDescrNewer);
        list.sort(ParsedDateComparator.DESCENDING);
        assertEquals(historyDescr, list.get(1));
        assertEquals(historyDescrNewer, list.get(0));
        assertEquals(historyDescrNewer,
                Collections.min(list, ParsedDateComparator.DESCENDING));
    }
}
