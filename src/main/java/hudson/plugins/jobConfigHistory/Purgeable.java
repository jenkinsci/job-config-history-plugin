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

import java.io.File;

/**
 * Implementors support periodic purging of history entries.
 *
 * @author Brandon Koepke
 */
public interface Purgeable {
    /**
     * Purges entries when there are more entries than specified by max entries.
     * <p>
     * The purge will drop old entries according to:
     * <p>
     * sort(entries, e.timestamp()); for (int i = maxEntries; i &lt;
     * entries.length(); i++) drop(entries[i]);
     *
     * @param itemHistoryRoot the history root to drop entries from.
     * @param maxEntries      the maximum number of entries to retain.
     */
    void purgeOldEntries(final File itemHistoryRoot, final int maxEntries);

    /**
     * Determines whether the specified directory should be dropped or not.
     *
     * @param historyDir the directory to check against.
     * @return true if it can be deleted, false otherwise.
     */
    boolean isCreatedEntry(final File historyDir);
}
