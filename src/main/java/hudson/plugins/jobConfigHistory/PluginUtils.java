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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
     * @return historyDao
     */
    public static FileHistoryDao getHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getHistoryDao(plugin);
    }

    /**
     * For tests.
     * @param plugin the plugin.
     * @return historyDao
     */
    public static FileHistoryDao getHistoryDao(final JobConfigHistory plugin) {
        final String maxHistoryEntriesAsString = plugin.getMaxHistoryEntries();
        int maxHistoryEntries = 0;
        try {
            maxHistoryEntries = Integer.valueOf(maxHistoryEntriesAsString);
        } catch (NumberFormatException e) {
            maxHistoryEntries = 0;
        }
        return new FileHistoryDao(
                plugin.getConfiguredHistoryRootDir(),
                new File(Hudson.getInstance().root.getPath()),
                User.current(),
                maxHistoryEntries,
                !plugin.getSkipDuplicateHistory());
    }
    /**
     * Returns a {@link Date}.
     *
     * @param timeStamp date as string.
     * @return The parsed date as a java.util.Date.
     */
    public static Date parsedDate(final String timeStamp) {
        try {
            return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER).parse(timeStamp);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse Date" + timeStamp, ex);
        }        
    }
}
