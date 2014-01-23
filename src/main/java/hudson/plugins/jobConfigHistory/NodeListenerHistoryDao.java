/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import hudson.model.Node;

/**
 *
 * @author Lucie Votypkova
 */
public interface NodeListenerHistoryDao {
    
    /**
     * Adds and saves the initial configuration of a node.
     *
     * @param node
     */
    void createNewNode(Node node);

    /**
     * Save and renames the node.
     *
     * @param node
     * @param oldName old node name
     * @param newName new name
     */
    void renameNode(Node node, String oldName, String newName);

    /**
     * Deletes the history of an node.
     *
     * @param node
     */
    void deleteNode(Node node);
    
}
