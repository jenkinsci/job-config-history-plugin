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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import hudson.Plugin;
import hudson.model.User;
import jenkins.model.Jenkins;

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
		Jenkins jenkins = Jenkins.getInstance();
		return jenkins != null ? jenkins.getPlugin(JobConfigHistory.class) : null;
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
	 * For tests.
	 * 
	 * @param plugin
	 *            the plugin.
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
	 * @param plugin
	 *            the plugin.
	 * @return historyDao
	 */
	public static JobConfigHistoryStrategy getAnonymousHistoryDao(
			final JobConfigHistory plugin) {
		return getHistoryDao(plugin, null);
	}

	public static JobConfigHistoryStrategy getHistoryDao(
			final JobConfigHistory plugin, final User user) {
		final String maxHistoryEntriesAsString = plugin.getMaxHistoryEntries();
		int maxHistoryEntries = 0;
		try {
			maxHistoryEntries = Integer.parseInt(maxHistoryEntriesAsString);
		} catch (NumberFormatException e) {
			maxHistoryEntries = 0;
		}
		Jenkins jenkins = Jenkins.getInstance();
		if(jenkins == null)
			return null;
		return new FileHistoryDao(plugin.getConfiguredHistoryRootDir(),
				new File(jenkins.root.getPath()), user,
				maxHistoryEntries, !plugin.getSkipDuplicateHistory());
	}

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
	 * @param timeStamp
	 *            date as string.
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
		Jenkins jenkins = Jenkins.getInstance();
		if(jenkins == null)
			return false;
		try {
			Plugin plugin = jenkins.getPlugin("maven-plugin");
			return plugin.getWrapper().isActive();
		} catch (Exception e) {
			return false;
		}
	}
}
