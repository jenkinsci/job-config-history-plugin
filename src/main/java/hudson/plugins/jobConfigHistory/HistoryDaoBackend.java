package hudson.plugins.jobConfigHistory;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Node;
import java.io.File;
import java.net.URL;
import jenkins.model.Jenkins;

/**
 * Master class for adding new backends to the history plugin.
 *
 * @author bkoepke
 */
public abstract class HistoryDaoBackend
    implements ExtensionPoint, HistoryDao, ItemListenerHistoryDao,
        OverviewHistoryDao, NodeListenerHistoryDao, Purgeable {
    /**
     * Determines whether this history dao supports the
     * given URL type.
     *
     * @param url the url to check support for.
     * @return true if this dao supports the given url,
     *         false otherwise.
     */
    public abstract boolean isUrlSupported(final URL url);

    /**
     * Sets the url for this backend to the specified url.
     * @param url the url to set this backend to.
     */
    public abstract void setUrl(final URL url);

    /**
     * All registered {@link HistoryDaoBackend}s.
     *
     * @return all registered backends.
     */
    public static ExtensionList<HistoryDaoBackend> all() {
        return Jenkins.getInstance().getExtensionList(HistoryDaoBackend.class);
    }
}
