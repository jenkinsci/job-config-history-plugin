package hudson.plugins.jobConfigHistory;

import hudson.model.Node;
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
     * Saves the current configuration of an node.
     *
     * @param node node
     */
    void saveNode(Node node);

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
     * Returns a sorted map of all revisions for this node.
     *
     * The key is an identifier which may be used in
     * {@link HistoryDao#getOldRevision(hudson.model.Node, java.lang.String)}
     *
     * @param node node
     * @return old revisions mapped to the identifier.
     */
    SortedMap<String, HistoryDescr> getRevisions(Node node);

    /**
     * Returns one old configuration of item.
     *
     * @param item project
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(AbstractItem item, String identifier);
    
    /**
     * Returns one old configuration of node.
     *
     * @param node node
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(Node node, String identifier);

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
     * @param configFileName file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    XmlFile getOldRevision(String configFileName, String identifier);
    
    /**
     * Returns whether the revision exists.
     *
     * @param node node
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    boolean hasOldRevision(Node node, String identifier);

    /**
     * Returns whether the revision exists.
     *
     * @param xmlFile file
     * @param identifier timestamp or hash
     * @return old configuration.
     */
    boolean hasOldRevision(XmlFile xmlFile, String identifier);

    /**
     * Determines whether the given node has already been recorded
     * in the history.
     *
     * @param node
     *        the node to check for duplicate history.
     * @return true if the node is a duplicate, false otherwise.
     */
    boolean hasDuplicateHistory(final Node node);
}

