package hudson.plugins.jobConfigHistory;

import static java.util.logging.Level.*;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.PeriodicWork;


/**
 *
 * @author kstutz
 *
 */
@Extension
public class JobConfigHistoryPurger extends PeriodicWork {
    /**The logger.*/
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryPurger.class.getName());

    /**Our plugin.*/
    private final JobConfigHistory plugin;

    /**The maximum allowed age of history entries in days.*/
    private int maxAge;
    
    /** The dao. */
    private HistoryDao historyDao;

    /** The overviewDao. */
    private final OverviewHistoryDao overviewHistoryDao;

    /**
     * Standard constructor using instance.
     */
    public JobConfigHistoryPurger() {
        this(Hudson.getInstance().getPlugin(JobConfigHistory.class), PluginUtils.getHistoryDao(), PluginUtils.getHistoryDao());
    }

    /**
     * For tests with injected plugin.
     *
     * @param plugin injected plugin
     * @param historyDao injected HistoryDao
     * @param overviewHistoryDao the value of overviewHistoryDao
     */
    JobConfigHistoryPurger(JobConfigHistory plugin, HistoryDao historyDao, OverviewHistoryDao overviewHistoryDao) {
        this.plugin = plugin;
        this.historyDao = historyDao;
        this.overviewHistoryDao = overviewHistoryDao;
    }

    @Override
    public long getRecurrencePeriod() {
        return DAY;
    }

    @Override
    protected void doRun() throws Exception {
        final String maxAgeString = plugin.getMaxDaysToKeepEntries();
        int maxAge = 0;
        if (StringUtils.isNotEmpty(maxAgeString)) {
            try {
                maxAge = Integer.parseInt(maxAgeString);
                if (maxAge > 0) {
                    LOG.log(FINE, "checking for history files to purge (max age of {0} days allowed)", maxAge);
                    this.setMaxAge(maxAge);
                    purgeHistoryByAge();
                }
            } catch (NumberFormatException e) {
                LOG.log(WARNING, "maximum age of history entries not formatted properly, unable to purge: {0}", maxAgeString);
            }
        }
    }

    /**
     * Performs the actual purge of history entries.
     */
    void purgeHistoryByAge() {
        purgeSystemOrJobHistory(overviewHistoryDao.getSystemConfigs());
        purgeSystemOrJobHistory(overviewHistoryDao.getJobs(""));
    }

    /**
     * Traverse directories in order to find files which are too old.
     * @param itemDirs Config history directories as file arrays.
     */
    void purgeSystemOrJobHistory(File[] itemDirs) {
        if (itemDirs != null && itemDirs.length > 0) {
            for (File itemDir : itemDirs) {
                //itemDir: z.B. Test2 or hudson.tasks.Ant
                final File[] historyDirs = itemDir.listFiles(HistoryFileFilter.INSTANCE);
                if (historyDirs != null) {
                    Arrays.sort(historyDirs);
                    for (File historyDir : historyDirs) {
                        //historyDir: e.g. 2013-01-18_17-33-51
                        if (isTooOld(historyDir)) {
                            if (!historyDao.isCreatedEntry(historyDir)) {
                                LOG.log(FINEST, "Should delete: {0}", historyDir);
                                deleteDirectory(historyDir);
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the history directory is too old by parsing its name as a date
     * and comparing it to the current date minus the maximal allowed age in days.
     * @param historyDir The history directory, e.g. 2013-01-18_17-33-51
     * @return True if it is too old.
     */
    boolean isTooOld(File historyDir) {
        Date parsedDate = null;
        final SimpleDateFormat dateParser = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
        try {
            parsedDate = dateParser.parse(historyDir.getName());
        } catch (ParseException ex) {
            LOG.log(WARNING, "Unable to parse Date: {0}", ex);
        }
        final Calendar historyDate = new GregorianCalendar();
        if (parsedDate != null) {
            historyDate.setTime(parsedDate);
            final Calendar oldestAllowedDate = new GregorianCalendar();
            oldestAllowedDate.add(Calendar.DAY_OF_YEAR, -getMaxAge());
            if (historyDate.before(oldestAllowedDate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a history directory (e.g. Test/2013-18-01_19-53-40),
     * first deleting the files it contains.
     * @param dir The directory which should be deleted.
     */
    void deleteDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (!file.delete()) {
                LOG.log(Level.WARNING, "problem deleting history file: {0}", file);
            }
        }
        if (!dir.delete()) {
            LOG.log(Level.WARNING, "problem deleting history directory: {0}", dir);
        }
    }

    /**
     * @return the maxAge
     */
    int getMaxAge() {
        return maxAge;
    }

    /**
     * For tests.
     * @param maxAge the maxAge to set
     */
    void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
    
    /**
     *
     * @return the historyDao
     */
    HistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
}