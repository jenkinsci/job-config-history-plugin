/*
 * The MIT License
 *
 * Copyright 2015 Brandon Koepke.
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

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Node;
import java.io.File;
import java.net.URL;
import jenkins.model.Jenkins;

/**
 * Master class for adding new backends to the history plugin.
 *
 * @author Brandon Koepke
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
