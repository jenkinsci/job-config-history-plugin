package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.FileFilter;

/**
 * Filters history files.
 *
 * @author Mirko Friedenhagen
 */
class HistoryFileFilter implements FileFilter {

    /** Singleton. */
    static HistoryFileFilter INSTANCE = new HistoryFileFilter();

    @Override
    public boolean accept(File file) {
        return file.exists() && new File(file, JobConfigHistoryConsts.HISTORY_FILE).exists();
    }

}
