package hudson.plugins.jobConfigHistory;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Node;
import java.io.File;
import java.net.URL;
import jenkins.model.Jenkins;

public abstract class HistoryDaoBackend implements ExtensionPoint, HistoryDao, ItemListenerHistoryDao, OverviewHistoryDao, NodeListenerHistoryDao {
    /**
     * Determines whether this history dao supports the
     * given URL type.
     * 
     * @param url the url to check support for.
     * @return true if this dao supports the given url,
     *         false otherwise.
     */
    public abstract boolean isUrlSupported(final URL url);

    public abstract void setUrl(final URL url);

    /**
     * Determines whether the given node has already been recorded 
     * in the history.
     * 
     * @param node
     *        the node to check for duplicate history.
     * @return true if the node is a duplicate, false otherwise.
     */
    public abstract boolean hasDuplicateHistory(final Node node);

    /**
     * Returns the configuration URL directory for the given configuration file.
     *
     * @param configFile
     *            The configuration file whose content we are saving.
     * @return The base URL to store the history,
     *         or null if the file is not a valid Hudson configuration file.
     */
    public abstract URL getHistoryUrl(final File configFile);

    /**
     * All registered {@link HistoryDaoBackend}s.
     */
    public static ExtensionList<HistoryDaoBackend> all() {
        return Jenkins.getInstance().getExtensionList(HistoryDaoBackend.class);
    }
}
