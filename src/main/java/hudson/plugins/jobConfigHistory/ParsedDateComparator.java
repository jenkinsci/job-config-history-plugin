package hudson.plugins.jobConfigHistory;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link ParsedDate}, sort order depends on {@link ParsedDate#parsedDate()}.
 *
 * Sort in descending order.
 *
 * @author mfriedenhagen
 */
@SuppressWarnings("serial")
final class ParsedDateComparator implements Comparator<ParsedDate>, Serializable {

    /**
     * No need to create more than one instance for descending.
     */
    public static final ParsedDateComparator DESCENDING = new ParsedDateComparator();

    /** {@inheritDoc} */
    public int compare(final ParsedDate ci1, final ParsedDate ci2) {
        return ci2.parsedDate().compareTo(ci1.parsedDate());
    }
}