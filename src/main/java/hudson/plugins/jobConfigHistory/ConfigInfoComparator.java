package hudson.plugins.jobConfigHistory;

import java.util.Comparator;

/**
 * Comparator for {@link ConfigInfo}.
 *
 * @author mfriedenhagen
 */
final class ConfigInfoComparator implements Comparator<ConfigInfo> {

    /**
     * No need to create more than one instance.
     */
    public final static ConfigInfoComparator INSTANCE = new ConfigInfoComparator();

    /** {@inheritDoc} */
    public int compare(final ConfigInfo o1, final ConfigInfo o2) {
        return o2.getDate().compareTo(o1.getDate());
    }
}