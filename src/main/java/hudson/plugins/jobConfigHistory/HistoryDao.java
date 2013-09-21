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
     * Adds and saves the initial configuration of an item.
     *
     * @param item project
     */
    void createNewItem(AbstractItem item);

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
     * Deletes the history of an item.
     *
     * @param item project
     */
    void deleteItem(AbstractItem item);

    /**
     * Save and renames the item.
     *
     * @param item project
     * @param oldName old project name
     * @param newName new name
     */
    void renameItem(AbstractItem item, String oldName, String newName);

    /**
     * Returns a sorted map of all revisions for this item.
     *
     * The key is an identifier which may be used in
     * {@link HistoryDao#getOldRevision(hudson.model.AbstractItem, java.lang.String)}
     *
     * @param item project
     * @return old revisions mapped to the identifier.
     */
    SortedMap<String, HistoryDescr> getRevisions(AbstractItem item);

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
     * Returns a list of deleted jobs with a history.
     *
     * @param folderName name of folder.
     * @return list of deleted jobs with a history.
     */
    File[] getDeletedJobs(String folderName);

    /**
     * Returns a list of jobs with a history.
     *
     * @param folderName name of folder
     * @return list of jobs with a history.
     */
    File[] getJobs(String folderName);

    /**
     * Returns a list of all system configuration files with a history.
     *
     * @return list of all system configuration files with a history.
     */
    File[] getSystemConfigs();
}

