package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.io.FileFilter;

/**
 * A filter to return only those directories of a file listing that represent configuration history directories.
 *
 * @author Mirko Friedenhagen
 */
class HistoryFileFilter implements FileFilter {

    /** Singleton. */
    static final HistoryFileFilter INSTANCE = new HistoryFileFilter();

    @Override
    public boolean accept(File file) {
        return file.exists() && new File(file, JobConfigHistoryConsts.HISTORY_FILE).exists();
    }

}
