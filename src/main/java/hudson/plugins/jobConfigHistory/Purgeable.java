package hudson.plugins.jobConfigHistory;

import java.io.File;

public interface Purgeable {
    void purgeOldEntries(final File itemHistoryRoot, final int maxEntries);
    boolean isCreatedEntry(final File historyDir);
}
