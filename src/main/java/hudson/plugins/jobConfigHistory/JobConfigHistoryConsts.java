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

    /**
     * Path to the jobConfigHistory base.
     */
    public static final String URLNAME = "jobConfigHistory";
    /**
     * Path to the icon.
     */
    public static final String ICONFILENAME = "/plugin/jobConfigHistory/img/confighistory.svg";
    /**
     * Default root directory for storing history.
     */
    public static final String DEFAULT_HISTORY_DIR = "config-history";
    /**
     * Default directory for storing job history.
     */
    public static final String JOBS_HISTORY_DIR = "jobs";
    /**
     * Default directory for storing node history.
     */
    public static final String NODES_HISTORY_DIR = "nodes";
    /**
     * name of history xml file.
     */
    public static final String HISTORY_FILE = "history.xml";
    /**
     * Default regexp pattern of configuration files not to save.
     */
    public static final String DEFAULT_EXCLUDE = "queue\\.xml|nodeMonitors\\.xml|UpdateCenter\\.xml|global-build-stats|LockableResourcesManager\\.xml|MilestoneStep\\.xml|cloudbees-disk-usage-simple\\.xml";
    /**
     * Format for timestamped dirs.
     */
    public static final String ID_FORMATTER = "yyyy-MM-dd_HH-mm-ss";
    /**
     * Maximum entries to be displayed in the history table pages.
     */
    public static final int DEFAULT_MAX_ENTRIES_PER_PAGE = 25;
    /**
     * Amount of neighbors displayed around the current page number in the page navigation bar. Example (epsilon=2, currentPage=6): [0 ... 4 5 <b>6</b> 7 8  ... 204]
     */
    public static final int PAGING_EPSILON = 2;
    /**
     * Name displayed for null users.
     */
    public static final String UNKNOWN_USER_NAME = "unknown";
    /**
     * Id displayed for null users.
     */
    public static final String UNKNOWN_USER_ID = "unknown";
    /**
     * The xml tag in config.xml which stores the changeReasonComment tag (that is temporarily storing the change message)
     */
    public static final String JOB_LOCAL_CONFIGURATION_XML_TAG = "hudson.plugins.jobConfigHistory.JobLocalConfiguration";
    /**
     * The xml tag in config.xml which stores the change message (temporarily!)
     */
    public static final String CHANGE_REASON_COMMENT_XML_TAG = "changeReasonComment";

    /**
     * Holder for constants.
     */
    private JobConfigHistoryConsts() {
        // Holder for constants
    }
}
