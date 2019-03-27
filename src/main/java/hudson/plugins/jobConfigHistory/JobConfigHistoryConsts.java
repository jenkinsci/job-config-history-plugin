/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
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

/**
 * Holder for constants.
 *
 * @author Stefan Brausch
 */
public final class JobConfigHistoryConsts {
	
	/** Path to the jobConfigHistory base. */
	public static final String URLNAME = "jobConfigHistory";

	/** Path to the icon. */
	public static final String ICONFILENAME = "/plugin/jobConfigHistory/img/confighistory.png";

	/** Default root directory for storing history. */
	public static final String DEFAULT_HISTORY_DIR = "config-history";

	/** Default directory for storing job history. */
	public static final String JOBS_HISTORY_DIR = "jobs";

	/** Default directory for storing node history. */
	public static final String NODES_HISTORY_DIR = "nodes";

	/** name of history xml file. */
	public static final String HISTORY_FILE = "history.xml";

	/** Default regexp pattern of configuration files not to save. */
	public static final String DEFAULT_EXCLUDE = "queue\\.xml|nodeMonitors\\.xml|UpdateCenter\\.xml|global-build-stats|LockableResourcesManager\\.xml|MilestoneStep\\.xml";

	/** Format for timestamped dirs. */
	public static final String ID_FORMATTER = "yyyy-MM-dd_HH-mm-ss";

	/** Default maximum number of configuration history entries to keep. */
	public static final String DEFAULT_MAX_HISTORY_ENTRIES = "1000";

	/** Default maximum number of history entries per site to show. */
	public static final String DEFAULT_MAX_ENTRIES_PER_PAGE = "30";

	/** Default maximum number of days to keep entries. */
	public static final String DEFAULT_MAX_DAYS_TO_KEEP_ENTRIES = "20";

	/**
	 * Holder for constants.
	 */
	private JobConfigHistoryConsts() {
		// Holder for constants
	}
}
