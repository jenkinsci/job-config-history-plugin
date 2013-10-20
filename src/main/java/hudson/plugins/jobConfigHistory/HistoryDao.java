package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import java.io.File;
import java.util.SortedMap;

/**
 * Operations for historization of config files.
 *
 * @author Mirko Friedenhagen
 */
public interface HistoryDao {

    /**
     * Saves the current configuration of an item.
     *
     * @param item project
     */
    void saveItem(AbstractItem item);

    /**
     * Saves a copy of an xml file.
     *
     * @param file xmlFile
     */
    void saveItem(XmlFile file);

    /**
     * Returns a sorted map of all revisions for this xmlFile.
     *
     * The key is an identifier which may be used in
     * {@link HistoryDao#getOldRevision(hudson.model.AbstractItem, java.lang.String)}
     *
     * @param xmlFile file
     * @return old revisions mapped to the identifier.
     */
    SortedMap<String, HistoryDescr> getRevisions(XmlFile xmlFile);

    /**
     * Returns a sorted map of all revisions for this configFile.
     *
     * The key is an identifier which may be used in
     * {@link HistoryDao#getOldRevision(hudson.model.AbstractItem, java.lang.String)}
     *
     * @param configFile file
     * @return old revisions mapped to the identifier.
     */
    SortedMap<String, HistoryDescr> getRevisions(File configFile);

    /**
     * Returns one old configuration of item.
     *
     * @param item project
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(AbstractItem item, String identifier);

    /**
     * Returns one old configuration of xmlFile.
     *
     * @param xmlFile file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(XmlFile xmlFile, String identifier);

    /**
     * Returns one old configuration of file.
     *
     * @param configFile file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(File configFile, String identifier);

    /**
     * Returns one old configuration of file.
     *
     * @param configFileName file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(String configFileName, String identifier);

    /**
     * Returns whether the revision exists.
     *
     * @param item project
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    boolean hasOldRevision(AbstractItem item, String identifier);

    /**
     * Returns whether the revision exists.
     *
     * @param xmlFile file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    boolean hasOldRevision(XmlFile xmlFile, String identifier);

    /**
     * Returns whether the revision exists.
     *
     * @param configFile file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    boolean hasOldRevision(File configFile, String identifier);

   /**
     * Purges old entries for the given history root to maxEntries.
     *
     * @param itemHistoryRoot directory to inspect.
     * @param maxEntries maximum number of entries.
     */
    void purgeOldEntries(final File itemHistoryRoot, final int maxEntries);

    /**
     * Checks whether the respective history entry is a 'Created' entry.
     *
     * @param historyDir The directory, e.g. 2013-01-18_17-33-51
     * @return True if the directory contains a 'Created' entry.
     */
    boolean isCreatedEntry(File historyDir);

    /**
     * Moves the history files of a restored project from the old location (_deleted_) to a directory with the new name.
     *
     * @param oldName The old name of the project (containing "_deleted_")
     * @param newName The new name of the project
     */
    void moveHistory(String oldName, String newName);
}

