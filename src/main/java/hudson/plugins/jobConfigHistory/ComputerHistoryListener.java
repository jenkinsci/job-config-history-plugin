/*
 * The MIT License
 *
 * Copyright 2013 Lucie Votypkova.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerListener;
import hudson.slaves.EphemeralNode;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Lucie Votypkova
 */
@Extension
public class ComputerHistoryListener extends ComputerListener {

    private static final Logger LOG = Logger
            .getLogger(ComputerHistoryListener.class.getName());
    List<Node> nodes;

    @Override
    public void onConfigurationChange() {
        Jenkins jenkins = Jenkins.get();
        if (nodes == null) {
            nodes = jenkins.getNodes();
        }
        if (nodes.size() < jenkins.getNodes().size()) {
            onAdd();
            nodes = jenkins.getNodes();
            return;
        }
        if (nodes.size() > jenkins.getNodes().size()) {
            onRemove();
            nodes = jenkins.getNodes();
            return;
        }
        if (!nodes.equals(jenkins.getNodes())) {
            onRename();
            nodes = jenkins.getNodes();
        }
        if (nodes.size() == jenkins.getNodes().size()) {
            onChange();
        }
    }

    /**
     * If a new agent gets added.
     */
    private void onAdd() {
        Jenkins jenkins = Jenkins.get();
        if (nodes == null) {
            nodes = jenkins.getNodes();
        }
        for (Node node : jenkins.getNodes()) {
            if (!nodes.contains(node) && isTracked(node)) {
                switchHistoryDao(node).createNewNode(node);
                return;
            }
        }
    }

    /**
     * Is this node likely to be important to the user?
     *
     */
    private boolean isTracked(Node node) {
        return node != null && !(node instanceof AbstractCloudSlave
                || node instanceof EphemeralNode);
    }

    /**
     * If an agent gets removed.
     */
    private void onRemove() {
        Jenkins jenkins = Jenkins.get();
        if (nodes == null) {
            nodes = jenkins.getNodes();
        }
        for (Node node : nodes) {
            if (!jenkins.getNodes().contains(node)
                    && isTracked(node)) {
                switchHistoryDao(node).deleteNode(node);
                return;
            }
        }
    }

    /**
     * If an agent configuration get changed.
     */
    private void onChange() {
        Jenkins jenkins = Jenkins.get();
        final JobConfigHistoryStrategy hdao = PluginUtils.getHistoryDao();
        for (Node node : jenkins.getNodes()) {
            if (!PluginUtils.isUserExcluded(PluginUtils.getPlugin())
                    && isTracked(node) && !hdao.hasDuplicateHistory(node)) {
                PluginUtils.getHistoryDao().saveNode(node);
                return;
            }
        }
    }

    /**
     * If an agent gets renamed.
     */
    private void onRename() {
        Node originalNode = null;
        Jenkins jenkins = Jenkins.get();
        if (nodes == null) {
            nodes = jenkins.getNodes();
        }
        for (Node node : nodes) {
            if (!jenkins.getNodes().contains(node)
                    && isTracked(node)) {
                originalNode = node;
            }
        }
        if (originalNode == null) {
            LOG.log(Level.WARNING, "Can not find changed node.");
            return;
        }
        Node newNode = null;
        for (Node node : jenkins.getNodes()) {
            if (!nodes.contains(node) && isTracked(node)) {
                newNode = node;
            }
        }
        if (!originalNode.getNodeName().equals(newNode.getNodeName())) {
            switchHistoryDao(originalNode).renameNode(newNode,
                    originalNode.getNodeName(), newNode.getNodeName());
        }
    }

    /**
     * Returns NodeListenerHistoryDao depending on the item type.
     *
     * @param node the node to switch on.
     * @return dao
     */
    private NodeListenerHistoryDao switchHistoryDao(Node node) {
        return node instanceof Slave
                ? getHistoryDao()
                : NoOpNodeListenerHistoryDao.INSTANCE;
    }

    NodeListenerHistoryDao getHistoryDao() {
        return PluginUtils.getHistoryDao();
    }

    /**
     * No operation NodeListenerHistoryDao.
     */
    private static class NoOpNodeListenerHistoryDao
            implements
            NodeListenerHistoryDao {

        /**
         * The instance.
         */
        static final NoOpNodeListenerHistoryDao INSTANCE = new NoOpNodeListenerHistoryDao();

        @Override
        public void createNewNode(Node node) {
            LOG.log(Level.FINEST, "onCreated: not an agent {0}, skipping.",
                    node);
        }

        @Override
        public void renameNode(Node node, String oldName, String newName) {
            LOG.log(Level.FINEST, "onRenamed: not an agent {0}, skipping.",
                    node);
        }

        @Override
        public void deleteNode(Node node) {
            LOG.log(Level.FINEST, "onDeleted: not an agent {0}, skipping.",
                    node);
        }

    }

}
