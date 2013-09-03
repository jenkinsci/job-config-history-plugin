package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import java.util.List;

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
    //void createNewHistoryEntry(final XmlFile xmlFile);
    
    void createNewItem(AbstractItem item);
    
    void saveItem(AbstractItem item);
    
    void saveItem(XmlFile file);
    
    void deleteItem(AbstractItem item);
    
    void moveItem(AbstractItem item, String newName);
    
    List<XmlFile> getRevisions(AbstractItem item);
    
    XmlFile getOldRevision(AbstractItem item, String identifier);
    
}

