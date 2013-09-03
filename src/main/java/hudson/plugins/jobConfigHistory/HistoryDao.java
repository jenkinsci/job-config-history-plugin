package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;

/**
 *
 * @author Mirko Friedenhagen
 */
public interface HistoryDao {

    /**
     * Creates a new backup of the job configuration.
     *
     * @param xmlFile
     *            configuration file for the item we want to backup
     */
    void createNewHistoryEntry(final XmlFile xmlFile);
}
