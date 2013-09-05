/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
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

import hudson.model.Hudson;
import hudson.model.User;
import java.io.File;

/**
 * Helper class.
 *
 * @author Mirko Friedenhagen
 */
final class PluginUtils {

    /**
     * Do not instantiate.
     */
    private PluginUtils() {
        // Static helper class
    }

    /**
     * Returns the plugin for tests.
     * @return plugin
     */
    public static JobConfigHistory getPlugin() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class);
    }

    /**
     * For tests.
     * @return listener
     */
    public static HistoryDao getHistoryDao() {
        final String maxHistoryEntriesAsString = getPlugin().getMaxHistoryEntries();
        int maxHistoryEntries = 0;
        try {
            maxHistoryEntries = Integer.valueOf(maxHistoryEntriesAsString);
        } catch (NumberFormatException e) {
            maxHistoryEntries = 0;
        }
        return new FileHistoryDao(
                getPlugin().getConfiguredHistoryRootDir(),
                new File(Hudson.getInstance().root.getPath()),
                User.current(),
                maxHistoryEntries);
    }

}
