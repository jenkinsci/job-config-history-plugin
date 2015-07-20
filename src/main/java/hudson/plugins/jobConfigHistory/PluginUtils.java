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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     *
     * @return plugin
     */
    public static JobConfigHistory getPlugin() {
        return Hudson.getInstance().getPlugin(JobConfigHistory.class);
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    public static HistoryDaoBackend getHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getHistoryDao(plugin);
    }

    /**
     * Like {@link #getHistoryDao()}, but without a user. Avoids calling
     * {@link User#current()}.
     *
     * @return historyDao
     */
    public static HistoryDaoBackend getAnonymousHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getAnonymousHistoryDao(plugin);
    }

    /**
     * For tests.
     *
     * @param plugin the plugin.
     * @return historyDao
     */
    public static HistoryDaoBackend getHistoryDao(final JobConfigHistory plugin) {
        return getHistoryDao(plugin, User.current());
    }

    /**
     * Like {@link #getHistoryDao(JobConfigHistory)}, but without a user. Avoids
     * calling {@link User#current()}.
     *
     * @param plugin the plugin.
     * @return historyDao
     */
    public static HistoryDaoBackend getAnonymousHistoryDao(final JobConfigHistory plugin) {
        return getHistoryDao(plugin, null);
    }

    private static int valueOfStringOrDefault(String s, int x) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return x;
        }
    }

    private static File getFileFromURL(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            Logger.getLogger(PluginUtils.class.getName()).log(Level.SEVERE, null, ex);
            return new File(url.getPath());
        }
    }

    static HistoryDaoBackend getHistoryDao(final JobConfigHistory plugin, final User user) {
        int maxHistoryEntries = valueOfStringOrDefault(plugin.getMaxHistoryEntries(), 0);
        final URL url;
        try {
            url = plugin.getConfiguredHistoryRootDir();
        } catch (MalformedURLException ex) {
            Logger.getLogger(PluginUtils.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        List<HistoryDaoBackend> backends = HistoryDaoBackend.all();
        for (HistoryDaoBackend backend : backends) {
            if (backend.isUrlSupported(url)) {
                if (backend instanceof FileHistoryDao) {
                    final File file;
                    try {
                        file = getFileFromURL(plugin.getConfiguredHistoryRootDir());
                    } catch (MalformedURLException ex) {
                        throw new IllegalStateException("Failed to initialize file from url: " + url);
                    }
                    // TODO: hack... need to create uniform instantiators as per: 
                    // https://wiki.jenkins-ci.org/display/JENKINS/Defining+a+new+extension+point
                    return new FileHistoryDao(
                        file,
                        new File(Hudson.getInstance().root.getPath()),
                        user,
                        maxHistoryEntries,
                        !plugin.getSkipDuplicateHistory());
                } else {
                    backend.setUrl(url);
                    return backend;
                }
            }
        }
        throw new IllegalStateException("Backend for " + url + " not supported.");
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
