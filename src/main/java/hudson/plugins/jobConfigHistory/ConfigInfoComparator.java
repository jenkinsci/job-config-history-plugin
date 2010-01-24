package hudson.plugins.jobConfigHistory;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link ConfigInfo}.
 *
 * @author mfriedenhagen
 */
@SuppressWarnings("serial")
final class ConfigInfoComparator implements Comparator<ConfigInfo>, Serializable {

    /**
     * No need to create more than one instance.
     */
    public static final ConfigInfoComparator INSTANCE = new ConfigInfoComparator();

    /** {@inheritDoc} */
    public int compare(final ConfigInfo o1, final ConfigInfo o2) {
        return o2.getDate().compareTo(o1.getDate());
    }
}