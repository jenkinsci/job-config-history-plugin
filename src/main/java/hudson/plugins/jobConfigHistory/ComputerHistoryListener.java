/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.slaves.ComputerListener;
import hudson.model.Node;
import hudson.model.Slave;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 *
 * @author Lucie Votypkova
 */
@Extension
public class ComputerHistoryListener extends ComputerListener {
    
    List<Node> nodes = Jenkins.getInstance().getNodes();
    
    private static final Logger LOG = Logger.getLogger(ComputerHistoryListener.class.getName());
    
    @Override
    public void onConfigurationChange() {
        if(nodes.size() < Jenkins.getInstance().getNodes().size()) {
            onAdd();
            nodes = Jenkins.getInstance().getNodes();
            return;
        }
        if(nodes.size() > Jenkins.getInstance().getNodes().size()) {
            onRemove();
            nodes = Jenkins.getInstance().getNodes();
            return;
        }
        if(!nodes.equals(Jenkins.getInstance().getNodes())) {
            onRename();
            nodes = Jenkins.getInstance().getNodes();
        }
        if(nodes.size() == Jenkins.getInstance().getNodes().size()) {
            onChange();  
        }
    }
    
    private void onAdd() {
        for(Node node: Jenkins.getInstance().getNodes()) {
            if(!nodes.contains(node)){
                switchHistoryDao(node).createNewNode(node);
                return;
            }
        }
    }
    
    private void onRemove() {
        for (Node node: nodes) {
            if (!Jenkins.getInstance().getNodes().contains(node)) {
                switchHistoryDao(node).deleteNode(node);
                return;
            }
        }
    }
    
    private void onChange() {
        FileHistoryDao hdao = PluginUtils.getHistoryDao();
        for (Node node : Jenkins.getInstance().getNodes()) {
            if (!hdao.hasDuplicateHistory(node)) {
                PluginUtils.getHistoryDao().saveNode(node);
                return;
            }
        }
    }
    
    private void onRename() {
        Node originalNode = null;
        for (Node node: nodes){
            if (!Jenkins.getInstance().getNodes().contains(node))
                originalNode = node;
        }
        if (originalNode == null){
            LOG.log(Level.WARNING, "Can not find changed node.");
            return;
        } 
        Node newNode = null;
        for (Node node: Jenkins.getInstance().getNodes()) {
            if (!nodes.contains(node))
               newNode = node; 
        }
        if (originalNode == null){
            LOG.log(Level.WARNING, "Can not find changed node.");
            return;
        } 
        if (!originalNode.getNodeName().equals(newNode.getNodeName())) {
            switchHistoryDao(originalNode).renameNode(newNode, originalNode.getNodeName(), newNode.getNodeName());
        }
    }
    
    /**
     * Returns NodeListenerHistoryDao depending on the item type.
     *
     * @param node the node to switch on.
     * @return dao
     */
    private NodeListenerHistoryDao switchHistoryDao(Node node) {
        return node instanceof Slave ? getHistoryDao() : NoOpNodeListenerHistoryDao.INSTANCE;
    }
    
    NodeListenerHistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }
    
    /**
     * No operation NodeListenerHistoryDao.
     */
    private static class NoOpNodeListenerHistoryDao implements NodeListenerHistoryDao {

        /**
         * The instance.
         */
        static final NoOpNodeListenerHistoryDao INSTANCE = new NoOpNodeListenerHistoryDao();

        @Override
        public void createNewNode(Node node) {
            LOG.log(Level.FINEST, "onCreated: not an Slave {0}, skipping.", node);
        }

        @Override
        public void renameNode(Node node, String oldName, String newName) {
            LOG.log(Level.FINEST, "onRenamed: not an Slave {0}, skipping.", node);
        }

        @Override
        public void deleteNode(Node node) {
            LOG.log(Level.FINEST, "onDeleted: not an Slave {0}, skipping.", node);
        }

    }
    
}
