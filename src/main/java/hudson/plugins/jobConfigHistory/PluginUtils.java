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

import hudson.ExtensionList;
import hudson.Plugin;
import hudson.model.User;
import hudson.security.ACL;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Helper class.
 *
 * @author Mirko Friedenhagen
 */
final public class PluginUtils {

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
        return ExtensionList.lookupSingleton(JobConfigHistory.class);
    }

    /**
     * For tests.
     *
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getHistoryDao(plugin);
    }

    /**
     * Like {@link #getHistoryDao()}, but without a user. Avoids calling
     * {@link User#current()}.
     *
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getAnonymousHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getAnonymousHistoryDao(plugin);
    }

    /**
     * Like {@link #getHistoryDao()}, but with SYSTEM user. Avoids calling
     * {@link User#current()}.
     * <p>
     * Only used to track changes when initLevel is not COMPLETED.
     *
     * @return historyDao
     */
    static JobConfigHistoryStrategy getSystemHistoryDao() {
        final JobConfigHistory plugin = getPlugin();
        return getSystemHistoryDao(plugin);
    }

    /**
     * For tests.
     *
     * @param plugin the plugin.
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getHistoryDao(
            final JobConfigHistory plugin) {
        return getHistoryDao(plugin, User.current());
    }

    /**
     * Like {@link #getHistoryDao(JobConfigHistory)}, but without a user. Avoids
     * calling {@link User#current()}.
     *
     * @param plugin the plugin.
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getAnonymousHistoryDao(
            final JobConfigHistory plugin) {
        return getHistoryDao(plugin, (User) null);
    }

    /**
     * Like {@link #getHistoryDao(JobConfigHistory)}, but with SYSTEM user. Avoids
     * calling {@link User#current()}.
     * <p>
     * Only used to track changes when initLevel is not COMPLETED.
     *
     * @param plugin the plugin.
     * @return historyDao
     */
    static JobConfigHistoryStrategy getSystemHistoryDao(
            final JobConfigHistory plugin) {
        //avoid loading the SYSTEM user on startup, which causes some errors.
        return getHistoryDao(plugin, new MimickedUser(ACL.SYSTEM_USERNAME, ACL.SYSTEM_USERNAME));
    }

    /**
     * Like {@link #getHistoryDao(JobConfigHistory)}, but with a custom user
     *
     * @param plugin the plugin
     * @param user   the user to initialize the historyDao with.
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getHistoryDao(
            final JobConfigHistory plugin, final User user) {
        return getHistoryDao(plugin, new MimickedUser(user));
    }

    /**
     * Like {@link #getHistoryDao(JobConfigHistory)}, but with a custom user wrapped with {@link MimickedUser}.
     *
     * @param plugin       the plugin
     * @param mimickedUser the user to initialize the historyDao with.
     * @return historyDao
     */
    public static JobConfigHistoryStrategy getHistoryDao(
            final JobConfigHistory plugin, final MimickedUser mimickedUser) {
        final String maxHistoryEntriesAsString = plugin.getMaxHistoryEntries();
        int maxHistoryEntries = 0;
        try {
            maxHistoryEntries = Integer.parseInt(maxHistoryEntriesAsString);
        } catch (NumberFormatException e) {
            maxHistoryEntries = 0;
        }
        return new FileHistoryDao(plugin.getConfiguredHistoryRootDir(),
                new File(Jenkins.get().root.getPath()), mimickedUser,
                maxHistoryEntries, !plugin.getSkipDuplicateHistory());
    }

    /**
     * @param plugin the plugin
     * @return whether the current user (determined by {@link Jenkins#getAuthentication()}) is excluded.
     */
    public static boolean isUserExcluded(final JobConfigHistory plugin) {

        String user = Jenkins.getAuthentication().getName();

        if (plugin.getExcludedUsers() != null) {
            String excludedUsers = plugin.getExcludedUsers().trim();
            String[] segs = excludedUsers.split(Pattern.quote(","));
            for (String seg : segs) {
                if (seg.trim().equals(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Date}.
     *
     * @param timeStamp date as string.
     * @return The parsed date as a java.util.Date.
     */
    public static Date parsedDate(final String timeStamp) {
        try {
            return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER)
                    .parse(timeStamp);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(
                    "Could not parse Date" + timeStamp, ex);
        }
    }

    /**
     * @return true, if Maven integration plugin is available and active.
     */
    public static boolean isMavenPluginAvailable() {
        try {
            Plugin plugin = Jenkins.get().getPlugin("maven-plugin");
            return plugin != null && plugin.getWrapper().isActive();
        } catch (Exception e) {
            return false;
        }
    }
}
