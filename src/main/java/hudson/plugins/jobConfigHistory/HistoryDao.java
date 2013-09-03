package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
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
     * @param item
     * @param newName
     */
    void renameItem(AbstractItem item, String newName);

    /**
     * Returns a sorted list of all configurations for this item.
     *
     * The key is an identifier which may be used in
     * {@link HistoryDao#getOldRevision(hudson.model.AbstractItem, java.lang.String)}
     *
     * @param item project
     * @return old configurations
     */
    SortedMap<String, XmlFile> getRevisions(AbstractItem item);

    /**
     * Returns one old configuration of item.
     *
     *
     * @param item
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(AbstractItem item, String identifier);

}

