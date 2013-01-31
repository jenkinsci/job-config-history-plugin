package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
    
    /**Number of milliseconds in a day.*/
    private static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
    
    /**Our plugin.*/
    private final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);
    
    /**The maximum allowed age of history entries in days.*/
    private int maxAge;
    
    @Override
    public long getRecurrencePeriod() {
//        return MIN;
        return DAY;
    }

    @Override
    protected void doRun() throws Exception {
        final String maxAgeString = plugin.getMaxDaysToKeepEntries();
        int maxAge = 0;
        if (StringUtils.isNotEmpty(maxAgeString)) {
            try {
                maxAge = Integer.parseInt(maxAgeString);
                if (maxAge < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                LOG.warning("maximum age of history entries not formatted properly, unable to purge: " + maxAgeString);
            }
        }
        if (maxAge > 0) {
            LOG.fine("checking for history files to purge (max age of " + maxAge + " days allowed)");
            this.maxAge = maxAge;
            purgeHistoryByAge();
        }
    }
    
    /**
     * Performs the actual purge of history entries.
     */
    private void purgeHistoryByAge() {
        purgeSystemOrJobHistory(plugin.getConfiguredHistoryRootDir().listFiles());
        purgeSystemOrJobHistory(plugin.getJobHistoryRootDir().listFiles());
    }
    
    /**
     * Traverse directories in order to find files which are too old.
     * @param itemDirs 
     */
    private void purgeSystemOrJobHistory(File[] itemDirs) {
        for (File itemDir : itemDirs) {
            //itemDir: z.B. Test2 oder hudson.tasks.Ant
            final File[] historyDirs = itemDir.listFiles(JobConfigHistory.HISTORY_FILTER);
            if (historyDirs != null) {
                Arrays.sort(historyDirs);
                for (File historyDir : historyDirs) {
                    //historyDir: z.B. 2013-01-18_17-33-51
                    Calendar oldestAllowedDate = new GregorianCalendar();
                    oldestAllowedDate.add(Calendar.DAY_OF_YEAR,-maxAge);
                    Date parsedDate = null;
                    final SimpleDateFormat dateParser = new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
                    try {
                        parsedDate = dateParser.parse(historyDir.getName());
                    } catch (ParseException ex) {
                        LOG.warning("Unable to parse Date: " + ex);
                    }
                    Calendar historyDate = new GregorianCalendar();
                    historyDate.setTime(parsedDate);
                    
                    if (historyDate.before(oldestAllowedDate)) {
                        LOG.finest("Should delete: " + historyDir);
                        deleteDirectory(historyDir);
                    } else {
                        break;
                    }
                }
            }
        }
    }
 
    /**
     * Deletes a history directory (e.g. Test/2013-18-01_19-53-40),
     * first deleting the files it contains.
     * @param dir The directory which should be deleted.
     */
    private void deleteDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (!file.delete()) {
                LOG.warning("problem deleting history file: " + file);
            }
        }
        if (!dir.delete()) {
            LOG.warning("problem deleting history directory: " + dir);
        }
    }
}